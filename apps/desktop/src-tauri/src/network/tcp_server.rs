use base64::prelude::*;
use device::{Device, DeviceManager, PairingManager, Platform};
use protocol::{
    ClipboardPayload, FileChunkPayload, FileCompletePayload, FileTransferRequest, Message,
    MessageType, NotificationPayload, PairRequestPayload,
};
use serde_json::json;
use std::collections::HashMap;
use std::fs::{self, File, OpenOptions};
use std::io::{BufRead, BufReader, Write};
use std::net::{TcpListener, TcpStream};
use std::path::PathBuf;
use std::sync::{Arc, Mutex, OnceLock};
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

    pub fn count(&self) -> usize {
        let map = self.clients.lock().unwrap();
        map.len()
    }

    pub fn register(&self, device_id: String, stream: Arc<Mutex<TcpStream>>) {
        let mut map = self.clients.lock().unwrap();
        map.insert(device_id, stream);
    }

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
    } else {
        let home = std::env::var("HOME").unwrap_or_else(|_| ".".to_string());
        PathBuf::from(home).join("Downloads")
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
                    MessageType::Hello => Message::new(
                        MessageType::Welcome,
                        request_id,
                        json!({
                            "name": "AetherLink Desktop",
                            "version": "1.0.0"
                        }),
                    ),

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

                                        Message::new(
                                            MessageType::PairResponse,
                                            request_id,
                                            json!({
                                                "status": "CONFIRMED",
                                                "pin": null,
                                                "message": "Device successfully paired and trusted"
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

                                    Message::new(
                                        MessageType::PairResponse,
                                        request_id,
                                        json!({
                                            "status": "PAIRED",
                                            "pin": null,
                                            "message": "Device is already trusted"
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

                                    Message::new(
                                        MessageType::PairResponse,
                                        request_id,
                                        json!({
                                            "status": "PENDING",
                                            "pin": pin,
                                            "message": "Verification code generated"
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

                    MessageType::Command | MessageType::Pong | MessageType::Error => {
                        // Client acknowledgment, pong, or client error report:
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

pub fn start_server(app_handle: Option<tauri::AppHandle>) {
    println!("==============================");
    println!("AetherLink TCP Server");
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

    let listener = TcpListener::bind(("0.0.0.0", PORT))
        .expect("Failed to bind TCP port");

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
