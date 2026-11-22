use crate::network::tcp_server::{get_downloads_dir, ConnectionPool};
use base64::prelude::*;
use protocol::{
    ClipboardPayload, FileChunkPayload, FileCompletePayload, FileTransferRequest, Message,
    MessageType,
};
use serde_json::json;
use std::fs::{self, File};
use std::io::Read;
use std::net::UdpSocket;
use std::path::{Path, PathBuf};
use tauri::Emitter;
use uuid::Uuid;

fn get_local_lan_ip() -> String {
    let socket = match UdpSocket::bind("0.0.0.0:0") {
        Ok(s) => s,
        Err(_) => return "127.0.0.1".to_string(),
    };

    if socket.connect("8.8.8.8:80").is_ok() {
        if let Ok(addr) = socket.local_addr() {
            return addr.ip().to_string();
        }
    }

    "127.0.0.1".to_string()
}

#[tauri::command]
pub async fn pick_files_dialog(app_handle: tauri::AppHandle) -> Vec<String> {
    tauri::async_runtime::spawn_blocking(move || {
        #[cfg(target_os = "linux")]
        {
            if let Ok(output) = std::process::Command::new("zenity")
                .arg("--file-selection")
                .arg("--multiple")
                .arg("--separator=|")
                .arg("--title=Select files to beam to phone")
                .output()
            {
                if output.status.success() {
                    let text = String::from_utf8_lossy(&output.stdout);
                    let paths: Vec<String> = text
                        .trim()
                        .split('|')
                        .map(|s| s.trim().to_string())
                        .filter(|s| !s.is_empty())
                        .collect();
                    if !paths.is_empty() {
                        return paths;
                    }
                }
            }
        }

        use tauri_plugin_dialog::DialogExt;
        let files = app_handle
            .dialog()
            .file()
            .set_title("Select files to beam to phone")
            .blocking_pick_files();

        match files {
            Some(list) => list
                .into_iter()
                .filter_map(|fp| fp.into_path().ok().map(|p| p.to_string_lossy().to_string()))
                .collect(),
            None => Vec::new(),
        }
    })
    .await
    .unwrap_or_default()
}

#[tauri::command]
pub fn send_clipboard(content: String, target_device_id: Option<String>) -> bool {
    crate::network::tcp_server::set_last_synced_clipboard(&content);
    crate::network::tcp_server::push_to_os_clipboard(content.clone());

    let payload = json!(ClipboardPayload {
        content,
        timestamp: chrono::Utc::now().timestamp_millis(),
        source_device: Some("VEXYRON Desktop".to_string()),
    });

    let message = Message::new(
        MessageType::Clipboard,
        Uuid::new_v4().to_string(),
        payload,
    );

    if let Some(dev_id) = target_device_id {
        ConnectionPool::global().send_to(&dev_id, &message).unwrap_or(false)
    } else {
        ConnectionPool::global().broadcast(&message) > 0
    }
}

#[tauri::command]
pub fn ping_device(device_id: String) -> bool {
    let message = Message::new(
        MessageType::Ping,
        Uuid::new_v4().to_string(),
        json!({
            "timestamp": chrono::Utc::now().timestamp_millis()
        }),
    );

    ConnectionPool::global().send_to(&device_id, &message).unwrap_or(false)
}

#[tauri::command]
pub fn get_network_info() -> serde_json::Value {
    let connected_count = device::DeviceManager::global()
        .devices()
        .iter()
        .filter(|d| d.connected)
        .count();

    json!({
        "port": 45876,
        "mdns_service": "_aetherlink._tcp.local.",
        "local_ip": get_local_lan_ip(),
        "connected_count": connected_count,
    })
}

#[tauri::command]
pub fn get_received_files() -> Vec<serde_json::Value> {
    let downloads_dir = get_downloads_dir();
    let mut files = Vec::new();

    if let Ok(entries) = fs::read_dir(&downloads_dir) {
        for entry in entries.flatten() {
            if let Ok(metadata) = entry.metadata() {
                if metadata.is_file() {
                    let file_name = entry.file_name().to_string_lossy().to_string();
                    let size_bytes = metadata.len();
                    let modified = metadata
                        .modified()
                        .ok()
                        .and_then(|t| t.duration_since(std::time::UNIX_EPOCH).ok())
                        .map(|d| d.as_millis() as i64)
                        .unwrap_or(0);

                    let ext = Path::new(&file_name)
                        .extension()
                        .and_then(|s| s.to_str())
                        .unwrap_or("")
                        .to_lowercase();

                    let category = match ext.as_str() {
                        "png" | "jpg" | "jpeg" | "gif" | "webp" | "svg" | "bmp" => "image",
                        "mp4" | "mkv" | "mov" | "avi" | "webm" => "video",
                        "mp3" | "wav" | "flac" | "aac" | "ogg" | "m4a" => "audio",
                        "pdf" | "doc" | "docx" | "txt" | "md" | "xls" | "xlsx" | "csv" => "document",
                        "zip" | "tar" | "gz" | "7z" | "rar" | "apk" => "archive",
                        _ => "other",
                    };

                    files.push(json!({
                        "name": file_name,
                        "path": entry.path().to_string_lossy().to_string(),
                        "size": size_bytes,
                        "category": category,
                        "modified": modified,
                        "extension": ext
                    }));
                }
            }
        }
    }

    files.sort_by(|a, b| {
        let b_mod = b.get("modified").and_then(|m| m.as_i64()).unwrap_or(0);
        let a_mod = a.get("modified").and_then(|m| m.as_i64()).unwrap_or(0);
        b_mod.cmp(&a_mod)
    });

    files.truncate(50);

    files
}

#[tauri::command]
pub fn open_downloads_folder() -> Result<(), String> {
    let dir = get_downloads_dir();
    #[cfg(target_os = "linux")]
    {
        std::process::Command::new("xdg-open")
            .arg(&dir)
            .spawn()
            .map_err(|e| format!("Failed to open folder: {e}"))?;
    }
    #[cfg(target_os = "windows")]
    {
        std::process::Command::new("explorer")
            .arg(&dir)
            .spawn()
            .map_err(|e| format!("Failed to open folder: {e}"))?;
    }
    #[cfg(target_os = "macos")]
    {
        std::process::Command::new("open")
            .arg(&dir)
            .spawn()
            .map_err(|e| format!("Failed to open folder: {e}"))?;
    }
    Ok(())
}

#[tauri::command]
pub fn open_file(path: String) -> Result<(), String> {
    let p = PathBuf::from(&path);
    if !p.exists() {
        return Err("File does not exist".to_string());
    }

    #[cfg(target_os = "linux")]
    {
        std::process::Command::new("xdg-open")
            .arg(&p)
            .spawn()
            .map_err(|e| format!("Failed to open file: {e}"))?;
    }
    #[cfg(target_os = "windows")]
    {
        std::process::Command::new("cmd")
            .args(["/C", "start", "", &p.to_string_lossy()])
            .spawn()
            .map_err(|e| format!("Failed to open file: {e}"))?;
    }
    #[cfg(target_os = "macos")]
    {
        std::process::Command::new("open")
            .arg(&p)
            .spawn()
            .map_err(|e| format!("Failed to open file: {e}"))?;
    }
    Ok(())
}

#[tauri::command]
pub fn delete_received_file(path: String) -> Result<(), String> {
    let p = PathBuf::from(&path);
    if p.exists() {
        fs::remove_file(p).map_err(|e| format!("Failed to delete file: {e}"))?;
    }
    Ok(())
}

#[tauri::command]
pub fn send_file_to_device(
    app_handle: tauri::AppHandle,
    device_id: String,
    file_path: String,
) -> Result<String, String> {
    let path = PathBuf::from(&file_path);
    if !path.exists() {
        return Err(format!("File '{}' not found", file_path));
    }

    let file_name = path
        .file_name()
        .map(|n| n.to_string_lossy().to_string())
        .unwrap_or_else(|| "unnamed_file".to_string());

    let metadata = fs::metadata(&path).map_err(|e| format!("Cannot read metadata: {e}"))?;
    let file_size = metadata.len();
    let file_id = Uuid::new_v4().to_string();

    let ext = path
        .extension()
        .and_then(|s| s.to_str())
        .unwrap_or("")
        .to_lowercase();
    let mime_type = match ext.as_str() {
        "png" => "image/png",
        "jpg" | "jpeg" => "image/jpeg",
        "pdf" => "application/pdf",
        "mp4" => "video/mp4",
        "zip" => "application/zip",
        "apk" => "application/vnd.android.package-archive",
        _ => "application/octet-stream",
    };

    // 1. Send File Request
    let req_payload = json!(FileTransferRequest {
        file_id: file_id.clone(),
        file_name: file_name.clone(),
        file_size,
        mime_type: mime_type.to_string(),
    });

    let req_msg = Message::new(
        MessageType::FileRequest,
        Uuid::new_v4().to_string(),
        req_payload,
    );

    let sent = ConnectionPool::global()
        .send_to(&device_id, &req_msg)
        .map_err(|e| format!("Failed to send file request: {e}"))?;

    if !sent {
        return Err("No Android smartphone is currently connected over Wi-Fi. Please check your phone connection.".to_string());
    }

    let handle_clone = app_handle.clone();
    let dev_id_clone = device_id.clone();
    let file_id_clone = file_id.clone();
    let file_name_clone = file_name.clone();

    // Perform streaming in a worker thread so UI remains 100% fluid
    std::thread::spawn(move || {
        // Brief pause to allow Android client to process FILE_REQUEST and prepare file receiver
        std::thread::sleep(std::time::Duration::from_millis(60));

        let chunk_size = 32 * 1024;
        let total_chunks = if file_size == 0 {
            1
        } else {
            ((file_size + chunk_size - 1) / chunk_size) as usize
        };

        let mut file = match File::open(&path) {
            Ok(f) => f,
            Err(e) => {
                let _ = handle_clone.emit(
                    "file-send-error",
                    json!({
                        "file_id": file_id_clone,
                        "file_name": file_name_clone,
                        "error": format!("Could not open file: {e}")
                    }),
                );
                return;
            }
        };

        let mut buffer = vec![0u8; chunk_size as usize];
        let mut chunk_index = 0;
        let mut total_sent: u64 = 0;

        loop {
            let bytes_read = match file.read(&mut buffer) {
                Ok(0) => break,
                Ok(n) => n,
                Err(e) => {
                    let _ = handle_clone.emit(
                        "file-send-error",
                        json!({
                            "file_id": file_id_clone,
                            "file_name": file_name_clone,
                            "error": format!("Read error: {e}")
                        }),
                    );
                    return;
                }
            };

            let chunk_slice = &buffer[..bytes_read];
            let data_base64 = BASE64_STANDARD.encode(chunk_slice);

            let chunk_payload = json!(FileChunkPayload {
                file_id: file_id_clone.clone(),
                chunk_index,
                total_chunks,
                data_base64,
            });

            let chunk_msg = Message::new(
                MessageType::FileChunk,
                Uuid::new_v4().to_string(),
                chunk_payload,
            );

            let sent_ok = ConnectionPool::global().send_to(&dev_id_clone, &chunk_msg).unwrap_or(false);
            if !sent_ok {
                let _ = handle_clone.emit(
                    "file-send-error",
                    json!({
                        "file_id": file_id_clone,
                        "file_name": file_name_clone,
                        "error": "Device connection dropped while sending"
                    }),
                );
                return;
            }

            total_sent += bytes_read as u64;
            let progress = if file_size > 0 {
                (total_sent as f64 / file_size as f64) as f32
            } else {
                1.0
            };

            let _ = handle_clone.emit(
                "file-send-progress",
                json!({
                    "file_id": file_id_clone,
                    "file_name": file_name_clone,
                    "progress": progress,
                    "sent_bytes": total_sent,
                    "total_bytes": file_size,
                    "status": format!("Chunk {}/{}", chunk_index + 1, total_chunks)
                }),
            );

            chunk_index += 1;
            std::thread::sleep(std::time::Duration::from_millis(1));
        }

        // 3. Send File Complete
        let comp_payload = json!(FileCompletePayload {
            file_id: file_id_clone.clone(),
            file_name: file_name_clone.clone(),
            status: "SUCCESS".to_string(),
        });

        let comp_msg = Message::new(
            MessageType::FileComplete,
            Uuid::new_v4().to_string(),
            comp_payload,
        );

        let _ = ConnectionPool::global().send_to(&dev_id_clone, &comp_msg);

        let _ = handle_clone.emit(
            "file-send-complete",
            json!({
                "file_id": file_id_clone,
                "file_name": file_name_clone,
                "total_bytes": file_size,
                "status": "SUCCESS"
            }),
        );
    });

    Ok(file_id)
}

#[tauri::command]
pub fn send_file_bytes(
    app_handle: tauri::AppHandle,
    device_id: String,
    file_name: String,
    file_bytes: Vec<u8>,
) -> Result<String, String> {
    let file_size = file_bytes.len() as u64;
    let file_id = Uuid::new_v4().to_string();

    let ext = Path::new(&file_name)
        .extension()
        .and_then(|s| s.to_str())
        .unwrap_or("")
        .to_lowercase();
    let mime_type = match ext.as_str() {
        "png" => "image/png",
        "jpg" | "jpeg" => "image/jpeg",
        "pdf" => "application/pdf",
        "mp4" => "video/mp4",
        "zip" => "application/zip",
        "apk" => "application/vnd.android.package-archive",
        _ => "application/octet-stream",
    };

    // 1. Send File Request
    let req_payload = json!(FileTransferRequest {
        file_id: file_id.clone(),
        file_name: file_name.clone(),
        file_size,
        mime_type: mime_type.to_string(),
    });

    let req_msg = Message::new(
        MessageType::FileRequest,
        Uuid::new_v4().to_string(),
        req_payload,
    );

    let sent = ConnectionPool::global()
        .send_to(&device_id, &req_msg)
        .map_err(|e| format!("Failed to send file request: {e}"))?;

    if !sent {
        return Err("No Android smartphone is currently connected over Wi-Fi. Please check your phone connection.".to_string());
    }

    let handle_clone = app_handle.clone();
    let dev_id_clone = device_id.clone();
    let file_id_clone = file_id.clone();
    let file_name_clone = file_name.clone();

    std::thread::spawn(move || {
        std::thread::sleep(std::time::Duration::from_millis(60));

        let chunk_size = 32 * 1024;
        let total_chunks = if file_size == 0 {
            1
        } else {
            (file_size as usize + chunk_size - 1) / chunk_size
        };

        let mut chunk_index = 0;
        let mut total_sent: u64 = 0;

        for chunk_slice in file_bytes.chunks(chunk_size) {
            let data_base64 = BASE64_STANDARD.encode(chunk_slice);

            let chunk_payload = json!(FileChunkPayload {
                file_id: file_id_clone.clone(),
                chunk_index,
                total_chunks,
                data_base64,
            });

            let chunk_msg = Message::new(
                MessageType::FileChunk,
                Uuid::new_v4().to_string(),
                chunk_payload,
            );

            let sent_ok = ConnectionPool::global().send_to(&dev_id_clone, &chunk_msg).unwrap_or(false);
            if !sent_ok {
                let _ = handle_clone.emit(
                    "file-send-error",
                    json!({
                        "file_id": file_id_clone,
                        "file_name": file_name_clone,
                        "error": "Device connection dropped while sending"
                    }),
                );
                return;
            }

            total_sent += chunk_slice.len() as u64;
            let progress = if file_size > 0 {
                (total_sent as f64 / file_size as f64) as f32
            } else {
                1.0
            };

            let _ = handle_clone.emit(
                "file-send-progress",
                json!({
                    "file_id": file_id_clone,
                    "file_name": file_name_clone,
                    "progress": progress,
                    "sent_bytes": total_sent,
                    "total_bytes": file_size,
                    "status": format!("Chunk {}/{}", chunk_index + 1, total_chunks)
                }),
            );

            chunk_index += 1;
            std::thread::sleep(std::time::Duration::from_millis(1));
        }

        // Send File Complete
        let comp_payload = json!(FileCompletePayload {
            file_id: file_id_clone.clone(),
            file_name: file_name_clone.clone(),
            status: "SUCCESS".to_string(),
        });

        let comp_msg = Message::new(
            MessageType::FileComplete,
            Uuid::new_v4().to_string(),
            comp_payload,
        );

        let _ = ConnectionPool::global().send_to(&dev_id_clone, &comp_msg);

        let _ = handle_clone.emit(
            "file-send-complete",
            json!({
                "file_id": file_id_clone,
                "file_name": file_name_clone,
                "total_bytes": file_size,
                "status": "SUCCESS"
            }),
        );
    });

    Ok(file_id)
}

#[derive(serde::Serialize)]
pub struct AppInfo {
    pub version: String,
    pub name: String,
    pub os: String,
    pub arch: String,
}

#[tauri::command]
pub fn get_app_info() -> AppInfo {
    AppInfo {
        version: env!("CARGO_PKG_VERSION").to_string(),
        name: "VEXYRON".to_string(),
        os: std::env::consts::OS.to_string(),
        arch: std::env::consts::ARCH.to_string(),
    }
}

#[tauri::command]
pub fn open_url(url: String) -> Result<(), String> {
    #[cfg(target_os = "linux")]
    {
        std::process::Command::new("xdg-open")
            .arg(&url)
            .spawn()
            .map_err(|e| format!("Failed to open URL: {e}"))?;
    }
    #[cfg(target_os = "windows")]
    {
        std::process::Command::new("cmd")
            .args(["/C", "start", "", &url])
            .spawn()
            .map_err(|e| format!("Failed to open URL: {e}"))?;
    }
    #[cfg(target_os = "macos")]
    {
        std::process::Command::new("open")
            .arg(&url)
            .spawn()
            .map_err(|e| format!("Failed to open URL: {e}"))?;
    }
    Ok(())
}

pub fn cleanup_legacy_installations() {
    #[cfg(target_os = "windows")]
    {
        let _ = std::process::Command::new("taskkill")
            .args(["/F", "/IM", "aether-link.exe", "/IM", "aetherlink.exe", "/IM", "desktop.exe", "/T"])
            .output();

        if let Some(local_appdata) = std::env::var_os("LOCALAPPDATA") {
            let old_programs = std::path::PathBuf::from(&local_appdata).join("Programs").join("AetherLink");
            if old_programs.exists() {
                let _ = std::fs::remove_dir_all(old_programs);
            }
            let old_appdata = std::path::PathBuf::from(&local_appdata).join("AetherLink");
            if old_appdata.exists() {
                let _ = std::fs::remove_dir_all(old_appdata);
            }
        }
    }

    #[cfg(target_os = "linux")]
    {
        if let Some(home) = dirs::home_dir() {
            let old_desktop_shortcut = home.join(".local/share/applications/aether-link.desktop");
            if old_desktop_shortcut.exists() {
                let _ = std::fs::remove_file(old_desktop_shortcut);
            }
        }
    }
}

#[tauri::command]
pub async fn install_desktop_update(url: String) -> Result<String, String> {
    let temp_dir = std::env::temp_dir();

    #[cfg(target_os = "windows")]
    let filename = "VEXYRON-Windows-Setup.exe";

    #[cfg(target_os = "linux")]
    let filename = "VEXYRON-Linux.deb";

    #[cfg(not(any(target_os = "windows", target_os = "linux")))]
    let filename = "VEXYRON-Update.bin";

    let target_path = temp_dir.join(filename);

    let output = std::process::Command::new("curl")
        .args([
            "-L",
            "--fail",
            "--silent",
            "--show-error",
            &url,
            "-o",
            &target_path.to_string_lossy(),
        ])
        .output()
        .map_err(|e| format!("Download execution failed: {e}"))?;

    if !output.status.success() {
        let err = String::from_utf8_lossy(&output.stderr);
        return Err(format!("Download failed: {err}"));
    }

    if !target_path.exists() || std::fs::metadata(&target_path).map(|m| m.len()).unwrap_or(0) == 0 {
        return Err("Downloaded update package is empty or invalid.".to_string());
    }

    #[cfg(target_os = "windows")]
    {
        std::process::Command::new(&target_path)
            .spawn()
            .map_err(|e| format!("Failed to launch installer: {e}"))?;

        std::thread::spawn(|| {
            std::thread::sleep(std::time::Duration::from_millis(1000));
            std::process::exit(0);
        });

        Ok("Installer launched! Application will close to apply update and replace files.".to_string())
    }

    #[cfg(target_os = "linux")]
    {
        let deb_str = target_path.to_string_lossy().to_string();

        let mut launched = false;
        if let Ok(_) = std::process::Command::new("xdg-open").arg(&deb_str).spawn() {
            launched = true;
        }

        if !launched {
            if let Ok(_) = std::process::Command::new("gnome-software").arg(&deb_str).spawn() {
                launched = true;
            }
        }

        if !launched {
            let _ = std::process::Command::new("pkexec")
                .args(["apt", "install", "-y", "--reinstall", &deb_str])
                .spawn();
        }

        Ok("Update package downloaded and opened with system package installer.".to_string())
    }

    #[cfg(not(any(target_os = "windows", target_os = "linux")))]
    {
        Ok(format!("Downloaded update to {}", target_path.to_string_lossy()))
    }
}
