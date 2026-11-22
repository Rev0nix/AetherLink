use base64::prelude::*;
use device::{Device, DeviceManager, PairingManager, Platform};
use protocol::{
    ClipboardPayload, FileChunkPayload, FileCompletePayload, FileTransferRequest, Message,
    MessageType, NotificationPayload, PairRequestPayload, TerminalExecPayload,
};
use serde_json::json;
use std::collections::HashMap;
use std::fs::{self, File, OpenOptions};
use std::io::{BufRead, BufReader, Read, Write};
use std::net::{TcpListener, TcpStream};
use std::path::PathBuf;
use std::process::{Command, Stdio};
use std::sync::{Arc, Mutex, OnceLock};
use std::time::{Duration, Instant};
use tauri::Emitter;
use uuid::Uuid;

const PORT: u16 = 45876;

// Connection Pool for active connected devices
#[derive(Default)]
pub struct ConnectionPool {
    clients: Mutex<HashMap<String, Arc<Mutex<TcpStream>>>>,
}

static POOL: OnceLock<ConnectionPool> = OnceLock::new();
static LAST_SYNCED_CLIPBOARD: Mutex<String> = Mutex::new(String::new());
static CLIPBOARD_TX: OnceLock<std::sync::mpsc::Sender<String>> = OnceLock::new();

pub fn set_last_synced_clipboard(text: &str) {
    if let Ok(mut lock) = LAST_SYNCED_CLIPBOARD.lock() {
        *lock = text.to_string();
    }
}

pub fn get_last_synced_clipboard() -> String {
    LAST_SYNCED_CLIPBOARD.lock().map(|s| s.clone()).unwrap_or_default()
}

pub fn push_to_os_clipboard(text: String) {
    if let Some(tx) = CLIPBOARD_TX.get() {
        let _ = tx.send(text);
    }
}

impl ConnectionPool {
    pub fn global() -> &'static ConnectionPool {
        POOL.get_or_init(ConnectionPool::default)
    }

    pub fn is_empty(&self) -> bool {
        let map = self.clients.lock().unwrap();
        map.is_empty()
    }

    #[allow(dead_code)]
    pub fn count(&self) -> usize {
        let map = self.clients.lock().unwrap();
        map.len()
    }

    pub fn register(&self, device_id: String, stream: Arc<Mutex<TcpStream>>) {
        let mut map = self.clients.lock().unwrap();
        map.insert(device_id, stream);
    }

    #[allow(dead_code)]
    pub fn remove(&self, device_id: &str) {
        let mut map = self.clients.lock().unwrap();
        map.remove(device_id);
    }

    pub fn remove_if_matching(&self, device_id: &str, stream_arc: &Arc<Mutex<TcpStream>>) -> bool {
        let mut map = self.clients.lock().unwrap();
        if let Some(existing) = map.get(device_id) {
            if Arc::ptr_eq(existing, stream_arc) {
                map.remove(device_id);
                return true;
            }
        }
        false
    }

    pub fn send_to(&self, device_id: &str, message: &Message) -> std::io::Result<bool> {
        let map = self.clients.lock().unwrap();
        if let Some(stream_arc) = map.get(device_id) {
            let mut stream = stream_arc.lock().unwrap();
            send_message(&mut stream, message.clone())?;
            return Ok(true);
        }

        // Fallback: If device_id wasn't an exact match, but there are connected clients, send to the first available connected client!
        if let Some((actual_id, stream_arc)) = map.iter().next() {
            println!("[ConnectionPool] Routing message to active device: {}", actual_id);
            let mut stream = stream_arc.lock().unwrap();
            send_message(&mut stream, message.clone())?;
            return Ok(true);
        }

        Ok(false)
    }

    pub fn broadcast(&self, message: &Message) -> usize {
        let map = self.clients.lock().unwrap();
        let mut count = 0;
        for (_, stream_arc) in map.iter() {
            if let Ok(mut stream) = stream_arc.lock() {
                if send_message(&mut stream, message.clone()).is_ok() {
                    count += 1;
                }
            }
        }
        count
    }
}

pub fn send_message(stream: &mut TcpStream, message: Message) -> std::io::Result<()> {
    let encoded = message
        .encode()
        .map_err(std::io::Error::other)?;

    stream.write_all(encoded.as_bytes())?;
    stream.write_all(b"\n")?;
    stream.flush()
}

pub fn get_downloads_dir() -> PathBuf {
    if let Some(user_dirs) = dirs::download_dir() {
        user_dirs
    } else if let Some(home) = dirs::home_dir() {
        home.join("Downloads")
    } else {
        let home = std::env::var("USERPROFILE")
            .or_else(|_| std::env::var("HOME"))
            .unwrap_or_else(|_| ".".to_string());
        PathBuf::from(home).join("Downloads")
    }
}

pub fn whoami_info() -> String {
    let user = std::env::var("USER")
        .or_else(|_| std::env::var("USERNAME"))
        .unwrap_or_else(|_| "user".to_string());
    let host = std::env::var("HOSTNAME")
        .or_else(|_| std::env::var("COMPUTERNAME"))
        .unwrap_or_else(|_| "localhost".to_string());
    format!("{}@{}", user, host)
}

pub fn run_terminal_command(
    cmd_trimmed: &str,
    cwd: &std::path::Path,
    sudo_pwd: Option<&str>,
) -> (String, String, i32) {
    let timeout = Duration::from_secs(30);

    #[cfg(target_os = "windows")]
    {
        let child_res = Command::new("powershell.exe")
            .args(&["-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", cmd_trimmed])
            .current_dir(cwd)
            .stdout(Stdio::piped())
            .stderr(Stdio::piped())
            .spawn()
            .or_else(|_| {
                Command::new("cmd.exe")
                    .args(&["/C", cmd_trimmed])
                    .current_dir(cwd)
                    .stdout(Stdio::piped())
                    .stderr(Stdio::piped())
                    .spawn()
            });

        match child_res {
            Ok(mut child) => {
                let start = Instant::now();
                loop {
                    match child.try_wait() {
                        Ok(Some(status)) => {
                            let mut stdout_buf = Vec::new();
                            let mut stderr_buf = Vec::new();
                            if let Some(mut out) = child.stdout.take() {
                                let _ = out.read_to_end(&mut stdout_buf);
                            }
                            if let Some(mut err) = child.stderr.take() {
                                let _ = err.read_to_end(&mut stderr_buf);
                            }
                            let stdout = String::from_utf8_lossy(&stdout_buf).to_string();
                            let stderr = String::from_utf8_lossy(&stderr_buf).to_string();
                            return (stdout, stderr, status.code().unwrap_or(0));
                        }
                        Ok(None) => {
                            if start.elapsed() > timeout {
                                let _ = child.kill();
                                return (
                                    String::new(),
                                    "Command timed out after 30 seconds. Interactive commands are not supported without a TTY.".to_string(),
                                    124,
                                );
                            }
                            std::thread::sleep(Duration::from_millis(50));
                        }
                        Err(e) => {
                            return (String::new(), format!("Process monitoring error: {e}"), 1);
                        }
                    }
                }
            }
            Err(e) => (String::new(), format!("Failed to spawn shell: {e}"), 127),
        }
    }

    #[cfg(not(target_os = "windows"))]
    {
        let is_sudo = cmd_trimmed.starts_with("sudo ");
        let mut actual_cmd = cmd_trimmed.to_string();
        let mut stdin_data: Option<Vec<u8>> = None;

        if is_sudo {
            if let Some(pass) = sudo_pwd {
                if !pass.is_empty() {
                    // Rewrite to sudo -S -k -p "" so it reads cleanly from stdin without prompting on tty
                    actual_cmd = cmd_trimmed.replacen("sudo ", "sudo -S -k -p \"\" ", 1);
                    let mut data = pass.as_bytes().to_vec();
                    data.push(b'\n');
                    stdin_data = Some(data);
                }
            } else {
                // Check if passwordless sudo is configured
                let check_status = Command::new("sudo")
                    .args(&["-n", "true"])
                    .status();
                if let Ok(st) = check_status {
                    if st.success() {
                        actual_cmd = cmd_trimmed.replacen("sudo ", "sudo -n ", 1);
                    } else {
                        return (
                            String::new(),
                            "sudo: Password required. Tap the 🔑 Sudo key in your phone Remote Terminal to enter your laptop's sudo password.".to_string(),
                            1,
                        );
                    }
                } else {
                    return (
                        String::new(),
                        "sudo: Password required. Tap the 🔑 Sudo key in your phone Remote Terminal to enter your laptop's sudo password.".to_string(),
                        1,
                    );
                }
            }
        }

        let child_res = Command::new("sh")
            .args(&["-c", &actual_cmd])
            .current_dir(cwd)
            .stdin(if stdin_data.is_some() { Stdio::piped() } else { Stdio::null() })
            .stdout(Stdio::piped())
            .stderr(Stdio::piped())
            .spawn();

        match child_res {
            Ok(mut child) => {
                if let Some(data) = stdin_data {
                    if let Some(mut sin) = child.stdin.take() {
                        let _ = sin.write_all(&data);
                        let _ = sin.flush();
                        drop(sin); // Close stdin so sudo knows input has finished
                    }
                }

                let start = Instant::now();
                loop {
                    match child.try_wait() {
                        Ok(Some(status)) => {
                            let mut stdout_buf = Vec::new();
                            let mut stderr_buf = Vec::new();
                            if let Some(mut out) = child.stdout.take() {
                                let _ = out.read_to_end(&mut stdout_buf);
                            }
                            if let Some(mut err) = child.stderr.take() {
                                let _ = err.read_to_end(&mut stderr_buf);
                            }
                            let stdout = String::from_utf8_lossy(&stdout_buf).to_string();
                            let stderr = String::from_utf8_lossy(&stderr_buf).to_string();
                            return (stdout, stderr, status.code().unwrap_or(0));
                        }
                        Ok(None) => {
                            if start.elapsed() > timeout {
                                let _ = child.kill();
                                return (
                                    String::new(),
                                    "Command timed out after 30 seconds. Interactive commands (e.g. top, nano) are not supported without a TTY.".to_string(),
                                    124,
                                );
                            }
                            std::thread::sleep(Duration::from_millis(50));
                        }
                        Err(e) => {
                            return (String::new(), format!("Process monitoring error: {e}"), 1);
                        }
                    }
                }
            }
            Err(e) => (String::new(), format!("Failed to execute process: {e}"), 127),
        }
    }
}

fn handle_client(stream: TcpStream, app_handle: Option<tauri::AppHandle>) {
    let _ = stream.set_nodelay(true);
    let peer_addr = stream.peer_addr().ok();
    let peer_ip = peer_addr.map(|a| a.ip().to_string()).unwrap_or_default();
    println!("Client Connected from {peer_ip}!");

    let reader_stream = match stream.try_clone() {
        Ok(stream) => stream,
        Err(error) => {
            println!("Failed to clone TCP stream: {error}");
            return;
        }
    };

    let stream_arc = Arc::new(Mutex::new(stream));
    let mut reader = BufReader::new(reader_stream);
    let mut connected_device_id: Option<String> = None;
    let mut active_file_transfers: HashMap<String, PathBuf> = HashMap::new();
    let mut session_cwd: PathBuf = dirs::home_dir().unwrap_or_else(|| PathBuf::from("."));

    loop {
        let mut line = String::new();

        match reader.read_line(&mut line) {
            Ok(0) => {
                println!("Client Disconnected from {peer_ip}");
                break;
            }

            Ok(_) => {
                let line = line.trim();

                if line.is_empty() {
                    continue;
                }

                let message = match Message::decode(line) {
                    Ok(message) => message,
                    Err(error) => {
                        println!("Protocol decode error: {error}");

                        let response = Message::new(
                            MessageType::Error,
                            Uuid::new_v4().to_string(),
                            json!({
                                "message": "Invalid protocol message"
                            }),
                        );

                        if let Ok(mut stream) = stream_arc.lock() {
                            let _ = send_message(&mut stream, response);
                        }

                        continue;
                    }
                };

                let request_id = message.request_id.clone();

                let response = match message.message_type {
                    MessageType::Hello => {
                        let os_type = if cfg!(target_os = "windows") {
                            "Windows"
                        } else if cfg!(target_os = "macos") {
                            "macOS"
                        } else {
                            "Linux"
                        };
                        let host_name = whoami_info();

                        Message::new(
                            MessageType::Welcome,
                            request_id,
                            json!({
                                "name": "VEXYRON Desktop",
                                "version": "2.3.6",
                                "os": os_type,
                                "platform": os_type,
                                "hostname": host_name
                            }),
                        )
                    }

                    MessageType::Ping => Message::new(
                        MessageType::Pong,
                        request_id,
                        json!({
                            "timestamp": chrono::Utc::now().timestamp_millis()
                        }),
                    ),

                    MessageType::DeviceInfo => {
                        if let Some(dev_id) = &connected_device_id {
                            let battery = message.payload.get("battery").and_then(|b| b.as_u64()).unwrap_or(100) as u8;
                            DeviceManager::global().set_battery(dev_id, battery);
                            if let Some(ref handle) = app_handle {
                                let _ = handle.emit("devices-updated", json!({ "device_id": dev_id }));
                            }
                        }
                        Message::new(
                            MessageType::Command,
                            request_id,
                            json!({ "status": "ACK" }),
                        )
                    }

                    MessageType::PairRequest => {
                        let req: Result<PairRequestPayload, _> =
                            serde_json::from_value(message.payload);

                        match req {
                            Ok(req) => {
                                let manager = PairingManager::global();

                                if let Some(pin) = req.pin {
                                    if manager.confirm_pairing(&req.device_id, &pin) {
                                        println!(
                                            "Pairing confirmed for device: {} ({})",
                                            req.device_name, req.device_id
                                        );

                                        connected_device_id = Some(req.device_id.clone());
                                        ConnectionPool::global().register(
                                            req.device_id.clone(),
                                            stream_arc.clone(),
                                        );

                                        let device = Device::with_details(
                                            req.device_id.clone(),
                                            req.device_name.clone(),
                                            Platform::Android,
                                            85,
                                            true,
                                            Some(peer_ip.clone()),
                                            Some("Wireless".to_string()),
                                        );
                                        DeviceManager::global().register(device);

                                        if let Some(ref handle) = app_handle {
                                            let _ = handle.emit(
                                                "device-paired",
                                                json!({
                                                    "device_id": req.device_id,
                                                    "device_name": req.device_name
                                                }),
                                            );
                                            let _ = handle.emit("devices-updated", json!({}));
                                        }

                                        let os_type = if cfg!(target_os = "windows") {
                                            "Windows"
                                        } else if cfg!(target_os = "macos") {
                                            "macOS"
                                        } else {
                                            "Linux"
                                        };
                                        let host_name = whoami_info();

                                        Message::new(
                                            MessageType::PairResponse,
                                            request_id,
                                            json!({
                                                "status": "CONFIRMED",
                                                "pin": null,
                                                "message": "Device successfully paired and trusted",
                                                "os": os_type,
                                                "platform": os_type,
                                                "hostname": host_name
                                            }),
                                        )
                                    } else {
                                        println!(
                                            "Pairing verification failed for device: {}",
                                            req.device_id
                                        );

                                        Message::new(
                                            MessageType::PairResponse,
                                            request_id,
                                            json!({
                                                "status": "REJECTED",
                                                "pin": null,
                                                "message": "Invalid PIN or pairing session expired"
                                            }),
                                        )
                                    }
                                } else if manager.is_paired(&req.device_id) || manager.is_paired_by_name(&req.device_name) {
                                    manager.update_device_id_for_name(&req.device_name, &req.device_id);
                                    println!(
                                        "Device {} ({}) is already trusted",
                                        req.device_name, req.device_id
                                    );

                                    connected_device_id = Some(req.device_id.clone());
                                    ConnectionPool::global().register(
                                        req.device_id.clone(),
                                        stream_arc.clone(),
                                    );

                                    let device = Device::with_details(
                                        req.device_id.clone(),
                                        req.device_name.clone(),
                                        Platform::Android,
                                        85,
                                        true,
                                        Some(peer_ip.clone()),
                                        Some("Wireless".to_string()),
                                    );
                                    DeviceManager::global().register(device);

                                    if let Some(ref handle) = app_handle {
                                        let _ = handle.emit("devices-updated", json!({}));
                                    }

                                    let os_type = if cfg!(target_os = "windows") {
                                        "Windows"
                                    } else if cfg!(target_os = "macos") {
                                        "macOS"
                                    } else {
                                        "Linux"
                                    };
                                    let host_name = whoami_info();

                                    Message::new(
                                        MessageType::PairResponse,
                                        request_id,
                                        json!({
                                            "status": "PAIRED",
                                            "pin": null,
                                            "message": "Device is already trusted",
                                            "os": os_type,
                                            "platform": os_type,
                                            "hostname": host_name
                                        }),
                                    )
                                } else {
                                    let pin = manager
                                        .request_pairing(&req.device_id, &req.device_name);

                                    if let Some(ref handle) = app_handle {
                                        let _ = handle.emit(
                                            "pairing-requested",
                                            json!({
                                                "device_id": req.device_id,
                                                "device_name": req.device_name,
                                                "pin": pin
                                            }),
                                        );
                                    }

                                    let os_type = if cfg!(target_os = "windows") {
                                        "Windows"
                                    } else if cfg!(target_os = "macos") {
                                        "macOS"
                                    } else {
                                        "Linux"
                                    };
                                    let host_name = whoami_info();

                                    Message::new(
                                        MessageType::PairResponse,
                                        request_id,
                                        json!({
                                            "status": "PENDING",
                                            "pin": pin,
                                            "message": "Verification code generated",
                                            "os": os_type,
                                            "platform": os_type,
                                            "hostname": host_name
                                        }),
                                    )
                                }
                            }
                            Err(error) => Message::new(
                                MessageType::Error,
                                request_id,
                                json!({
                                    "message": format!("Invalid PairRequest: {error}")
                                }),
                            ),
                        }
                    }

                    MessageType::Clipboard => {
                        let payload: Result<ClipboardPayload, _> =
                            serde_json::from_value(message.payload.clone());

                        let (content, timestamp, source) = match payload {
                            Ok(clip) => (
                                clip.content,
                                clip.timestamp,
                                clip.source_device.unwrap_or_else(|| "Android Device".to_string()),
                            ),
                            Err(e) => {
                                eprintln!("[TCP SERVER] Error parsing ClipboardPayload ({e}), fallback to direct extraction");
                                let text = message
                                    .payload
                                    .get("content")
                                    .and_then(|v| v.as_str())
                                    .unwrap_or_default()
                                    .to_string();
                                (
                                    text,
                                    chrono::Utc::now().timestamp_millis(),
                                    "Android Device".to_string(),
                                )
                            }
                        };

                        if !content.is_empty() {
                            println!("[TCP SERVER] Received clipboard from Phone ({} chars)", content.len());
                            set_last_synced_clipboard(&content);
                            push_to_os_clipboard(content.clone());

                            if let Some(ref handle) = app_handle {
                                let _ = handle.emit("clipboard-received", json!({
                                    "content": content,
                                    "timestamp": timestamp,
                                    "source": source
                                }));
                            }
                        }

                        // Do not reply back to avoid ping-pong echo loops
                        continue;
                    }

                    MessageType::Notification => {
                        let payload: Result<NotificationPayload, _> =
                            serde_json::from_value(message.payload);

                        if let Ok(notif) = payload {
                            println!("Notification received: [{}] {}", notif.app_name, notif.title);
                            if let Some(ref handle) = app_handle {
                                let _ = handle.emit("notification-received", json!({
                                    "id": notif.id,
                                    "app_name": notif.app_name,
                                    "package_name": notif.package_name,
                                    "title": notif.title,
                                    "text": notif.text,
                                    "timestamp": notif.timestamp,
                                }));
                            }
                        }

                        Message::new(
                            MessageType::Command,
                            request_id,
                            json!({ "status": "RECEIVED" }),
                        )
                    }

                    MessageType::FileRequest => {
                        let req: Result<FileTransferRequest, _> =
                            serde_json::from_value(message.payload);

                        match req {
                            Ok(file_req) => {
                                let downloads_dir = get_downloads_dir();
                                let _ = fs::create_dir_all(&downloads_dir);

                                // Sanitize filename: strip any paths, colons, or traversal characters
                                let raw_name = file_req.file_name.trim();
                                let clean_name = std::path::Path::new(raw_name)
                                    .file_name()
                                    .and_then(|n| n.to_str())
                                    .unwrap_or("received_file")
                                    .replace(['/', '\\', ':', '*', '?', '"', '<', '>', '|'], "_");

                                let final_name = if clean_name.is_empty() {
                                    let id_prefix = if file_req.file_id.len() >= 8 { &file_req.file_id[..8] } else { &file_req.file_id };
                                    format!("aether_file_{}", id_prefix)
                                } else {
                                    clean_name
                                };

                                let target_file = downloads_dir.join(&final_name);
                                println!("[TCP SERVER] Preparing incoming file: {:?}", target_file);

                                match File::create(&target_file) {
                                    Ok(_) => {
                                        println!("[TCP SERVER] Initialized file on disk: {:?}", target_file);
                                        active_file_transfers.insert(file_req.file_id.clone(), target_file.clone());
                                    }
                                    Err(err) => {
                                        eprintln!("[TCP SERVER] Error creating file {:?}: {}", target_file, err);
                                        let fallback = downloads_dir.join(format!("received_{}", chrono::Utc::now().timestamp()));
                                        if let Ok(_) = File::create(&fallback) {
                                            active_file_transfers.insert(file_req.file_id.clone(), fallback);
                                        }
                                    }
                                }

                                if let Some(ref handle) = app_handle {
                                    let _ = handle.emit("file-transfer-started", json!({
                                        "file_id": file_req.file_id,
                                        "file_name": file_req.file_name,
                                        "file_size": file_req.file_size
                                    }));
                                }

                                Message::new(
                                    MessageType::Command,
                                    request_id,
                                    json!({
                                        "status": "READY",
                                        "file_id": file_req.file_id
                                    }),
                                )
                            }
                            Err(e) => Message::new(
                                MessageType::Error,
                                request_id,
                                json!({ "message": format!("File request error: {e}") }),
                            ),
                        }
                    }

                    MessageType::FileChunk => {
                        let chunk: Result<FileChunkPayload, _> =
                            serde_json::from_value(message.payload);

                        match chunk {
                            Ok(c) => {
                                if let Some(path) = active_file_transfers.get(&c.file_id) {
                                    match BASE64_STANDARD.decode(&c.data_base64) {
                                        Ok(bytes) => {
                                            match OpenOptions::new().append(true).open(path) {
                                                Ok(mut f) => {
                                                    if let Err(e) = f.write_all(&bytes) {
                                                        eprintln!("[TCP SERVER] Write error chunk {}: {}", c.chunk_index, e);
                                                    }
                                                }
                                                Err(e) => {
                                                    eprintln!("[TCP SERVER] Open error for append on {:?}: {}", path, e);
                                                }
                                            }
                                        }
                                        Err(e) => {
                                            eprintln!("[TCP SERVER] Base64 decode error on chunk {}: {}", c.chunk_index, e);
                                        }
                                    }
                                } else {
                                    eprintln!("[TCP SERVER] No active transfer file found for file_id={}", c.file_id);
                                }

                                Message::new(
                                    MessageType::Command,
                                    request_id,
                                    json!({ "status": "CHUNK_ACK", "chunk": c.chunk_index }),
                                )
                            }
                            Err(e) => Message::new(
                                MessageType::Error,
                                request_id,
                                json!({ "message": format!("File chunk error: {e}") }),
                            ),
                        }
                    }

                    MessageType::FileComplete => {
                        let comp: Result<FileCompletePayload, _> =
                            serde_json::from_value(message.payload);

                        if let Ok(c) = comp {
                            let path = active_file_transfers.remove(&c.file_id);
                            let size = path.as_ref().and_then(|p| fs::metadata(p).ok()).map(|m| m.len()).unwrap_or(0);
                            println!("[TCP SERVER] File transfer COMPLETED for {:?}, total size: {} bytes", path, size);

                            if let Some(ref handle) = app_handle {
                                let _ = handle.emit("file-received", json!({
                                    "file_id": c.file_id,
                                    "file_name": c.file_name,
                                    "path": path.map(|p| p.to_string_lossy().to_string()).unwrap_or_default(),
                                    "size": size
                                }));
                            }
                        }

                        Message::new(
                            MessageType::Command,
                            request_id,
                            json!({ "status": "COMPLETED" }),
                        )
                    }

                    MessageType::TerminalExec => {
                        let payload_res: Result<TerminalExecPayload, _> =
                            serde_json::from_value(message.payload.clone());
                        let (raw_cmd, requested_cwd, sudo_pwd) = match payload_res {
                            Ok(p) => (p.command, p.cwd, p.sudo_password),
                            Err(_) => {
                                let cmd = message
                                    .payload
                                    .get("command")
                                    .and_then(|v| v.as_str())
                                    .unwrap_or("")
                                    .to_string();
                                let c = message
                                    .payload
                                    .get("cwd")
                                    .and_then(|v| v.as_str())
                                    .map(|s| s.to_string());
                                let sp = message
                                    .payload
                                    .get("sudo_password")
                                    .and_then(|v| v.as_str())
                                    .map(|s| s.to_string());
                                (cmd, c, sp)
                            }
                        };

                        if let Some(c) = requested_cwd {
                            let p = PathBuf::from(&c);
                            if p.is_dir() {
                                session_cwd = p;
                            }
                        }

                        let cmd_trimmed = raw_cmd.trim();
                        let mut stdout_text = String::new();
                        let mut stderr_text = String::new();
                        let mut exit_code = 0;

                        if cmd_trimmed.is_empty() {
                            // Welcome banner / connection info
                            #[cfg(target_os = "windows")]
                            {
                                stdout_text = format!(
                                    "Microsoft Windows PowerShell Remote Shell\r\nConnected to: {}\r\nReady for input.",
                                    whoami_info()
                                );
                            }
                            #[cfg(not(target_os = "windows"))]
                            {
                                stdout_text = format!(
                                    "Linux / POSIX Remote Shell\nConnected to: {}\nReady for input.",
                                    whoami_info()
                                );
                            }
                        } else if cmd_trimmed == "clear" || cmd_trimmed == "cls" {
                            stdout_text = "[CLEAR]".to_string();
                        } else if cmd_trimmed.starts_with("cd ") || cmd_trimmed == "cd" {
                            let target = cmd_trimmed.strip_prefix("cd").unwrap().trim();
                            if target.is_empty() || target == "~" {
                                if let Some(h) = dirs::home_dir() {
                                    session_cwd = h;
                                }
                            } else {
                                let target_path = if target.starts_with('/')
                                    || (target.len() > 1 && target.chars().nth(1) == Some(':'))
                                {
                                    PathBuf::from(target)
                                } else {
                                    session_cwd.join(target)
                                };

                                match std::fs::canonicalize(&target_path) {
                                    Ok(canonical) => {
                                        session_cwd = canonical;
                                    }
                                    Err(err) => {
                                        stderr_text = format!("cd: {}: {}", target, err);
                                        exit_code = 1;
                                    }
                                }
                            }
                        } else {
                            // Robust shell execution with sudo support and timeout protection
                            let (out, err, code) = run_terminal_command(
                                cmd_trimmed,
                                &session_cwd,
                                sudo_pwd.as_deref(),
                            );
                            stdout_text = out;
                            stderr_text = err;
                            exit_code = code;
                        }

                        // Safeguard buffer size to prevent socket saturation
                        const MAX_OUTPUT_BYTES: usize = 65536;
                        if stdout_text.len() > MAX_OUTPUT_BYTES {
                            stdout_text.truncate(MAX_OUTPUT_BYTES);
                            stdout_text.push_str("\n... [Output truncated at 64 KB] ...");
                        }
                        if stderr_text.len() > MAX_OUTPUT_BYTES {
                            stderr_text.truncate(MAX_OUTPUT_BYTES);
                            stderr_text.push_str("\n... [Error output truncated at 64 KB] ...");
                        }

                        let os_type = if cfg!(target_os = "windows") {
                            "Windows"
                        } else if cfg!(target_os = "macos") {
                            "macOS"
                        } else {
                            "Linux"
                        };

                        let out_payload = json!({
                            "command": cmd_trimmed,
                            "stdout": stdout_text,
                            "stderr": stderr_text,
                            "exit_code": exit_code,
                            "cwd": session_cwd.to_string_lossy().to_string(),
                            "os": os_type,
                            "platform": os_type,
                            "hostname": whoami_info(),
                            "origin": "remote"
                        });

                        if let Some(ref handle) = app_handle {
                            let _ = handle.emit("terminal-command-executed", &out_payload);
                        }

                        Message::new(
                            MessageType::TerminalOutput,
                            request_id,
                            out_payload,
                        )
                    }

                    MessageType::Command | MessageType::Pong | MessageType::Error | MessageType::TerminalOutput => {
                        // Client acknowledgment, pong, client error report, or output report:
                        // Do not reply with another message to avoid infinite loop.
                        continue;
                    }

                    _ => Message::new(
                        MessageType::Error,
                        request_id,
                        json!({
                            "message": "Message type not implemented yet"
                        }),
                    ),
                };

                if let Ok(mut stream) = stream_arc.lock() {
                    if let Err(error) = send_message(&mut stream, response) {
                        println!("Write Error: {error}");
                        break;
                    }
                }
            }

            Err(error) => {
                println!("Read Error: {error}");
                break;
            }
        }
    }

    // Cleanup on disconnect - only remove if this specific stream is still the active one
    if let Some(dev_id) = connected_device_id {
        if ConnectionPool::global().remove_if_matching(&dev_id, &stream_arc) {
            println!("Cleaning up disconnected session for device {dev_id}");
            DeviceManager::global().set_connected(&dev_id, false);

            if let Some(ref handle) = app_handle {
                let _ = handle.emit("device-disconnected", json!({ "device_id": dev_id }));
                let _ = handle.emit("devices-updated", json!({}));
            }
        } else {
            println!("Stale session ended for device {dev_id}, active newer session preserved.");
        }
    }
}

fn create_tcp_listener(port: u16) -> std::io::Result<TcpListener> {
    use socket2::{Domain, Socket, Type};
    let socket = Socket::new(Domain::IPV4, Type::STREAM, None)?;
    let _ = socket.set_reuse_address(true);
    #[cfg(all(unix, not(target_os = "solaris"), not(target_os = "illumos")))]
    {
        let _ = socket.set_reuse_port(true);
    }
    let _ = socket.set_nonblocking(false);
    let addr: std::net::SocketAddr = format!("0.0.0.0:{port}").parse().map_err(|e| {
        std::io::Error::new(std::io::ErrorKind::InvalidInput, e)
    })?;
    socket.bind(&addr.into())?;
    socket.listen(128)?;
    Ok(socket.into())
}

pub fn start_server(app_handle: Option<tauri::AppHandle>) {
    println!("==============================");
    println!("VEXYRON TCP Server");
    println!("==============================");

    let _mdns_daemon = match super::discovery::start_advertisement(PORT) {
        Ok(daemon) => {
            println!("mDNS discovery broadcast active on port {PORT}");
            Some(daemon)
        }
        Err(error) => {
            println!("Warning: Could not start mDNS advertisement: {error}");
            None
        }
    };

    let mut retry_count = 0;
    let listener = loop {
        match create_tcp_listener(PORT) {
            Ok(l) => break l,
            Err(error) => {
                if retry_count < 5 {
                    retry_count += 1;
                    eprintln!("Warning: Failed to bind TCP port {PORT} ({error}), reclaiming port and retrying {retry_count}/5 in 1s...");
                    #[cfg(unix)]
                    {
                        let _ = std::process::Command::new("fuser")
                            .args(&["-k", &format!("{PORT}/tcp")])
                            .output();
                    }
                    std::thread::sleep(std::time::Duration::from_millis(1000));
                } else {
                    eprintln!("Error: Could not bind TCP server to port {PORT}: {error}");
                    return;
                }
            }
        }
    };

    println!("Listening on port {PORT}...");

    for stream in listener.incoming() {
        match stream {
            Ok(stream) => {
                let handle_clone = app_handle.clone();
                std::thread::spawn(move || {
                    handle_client(stream, handle_clone);
                });
            }

            Err(error) => {
                println!("Connection Error: {error}");
            }
        }
    }
}

pub fn start_clipboard_watcher(app_handle: Option<tauri::AppHandle>) {
    let (tx, rx) = std::sync::mpsc::channel::<String>();
    let _ = CLIPBOARD_TX.set(tx);

    std::thread::spawn(move || {
        println!("[CLIPBOARD WORKER] Starting persistent Clipboard thread on Wayland/X11");
        let mut cb = match arboard::Clipboard::new() {
            Ok(c) => c,
            Err(e) => {
                eprintln!("[CLIPBOARD WORKER] Failed to initialize arboard: {e}");
                return;
            }
        };

        let mut last_local_clip = String::new();
        if let Ok(text) = cb.get_text() {
            last_local_clip = text.clone();
            set_last_synced_clipboard(&text);
        }

        loop {
            // 1. Process any incoming text from phone to write to laptop OS clipboard
            while let Ok(text_from_phone) = rx.try_recv() {
                if !text_from_phone.is_empty() {
                    println!("[CLIPBOARD WORKER] Setting persistent OS clipboard ({} chars)", text_from_phone.len());
                    last_local_clip = text_from_phone.clone();
                    set_last_synced_clipboard(&text_from_phone);
                    if let Err(e) = cb.set_text(&text_from_phone) {
                        eprintln!("[CLIPBOARD WORKER] Error writing to OS clipboard: {e}");
                    }
                }
            }

            // 2. Check if user copied anything locally on laptop to beam to phone
            if !ConnectionPool::global().is_empty() {
                if let Ok(current_clip) = cb.get_text() {
                    if !current_clip.is_empty() && current_clip != last_local_clip {
                        let last_synced = get_last_synced_clipboard();
                        if current_clip != last_synced {
                            println!("[CLIPBOARD WORKER] Local laptop copy detected ({} chars), beaming to phone...", current_clip.len());
                            last_local_clip = current_clip.clone();
                            set_last_synced_clipboard(&current_clip);

                            let payload = json!(ClipboardPayload {
                                content: current_clip.clone(),
                                timestamp: chrono::Utc::now().timestamp_millis(),
                                source_device: Some("Laptop".to_string()),
                            });

                            let msg = Message::new(
                                MessageType::Clipboard,
                                Uuid::new_v4().to_string(),
                                payload,
                            );

                            let count = ConnectionPool::global().broadcast(&msg);
                            println!("[CLIPBOARD WORKER] Broadcasted to {} phone(s)", count);

                            if let Some(ref handle) = app_handle {
                                let _ = handle.emit("clipboard-received", json!({
                                    "content": current_clip,
                                    "timestamp": chrono::Utc::now().timestamp_millis(),
                                    "source": "Laptop"
                                }));
                            }
                        } else {
                            last_local_clip = current_clip;
                        }
                    }
                }
            }

            std::thread::sleep(std::time::Duration::from_millis(400));
        }
    });
}
