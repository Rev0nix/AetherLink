mod commands;
mod network;

use commands::device::*;
use commands::features::*;
use commands::pairing::*;

#[tauri::command]
fn list_phone_files(serial: String, path: String) -> Vec<device::PhoneFile> {
    device::FileService::list(&serial, &path)
}

#[tauri::command]
fn upload_phone_file(serial: String, local: String, remote: String) {
    device::FileService::upload(&serial, &local, &remote);
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_dialog::init())
        .setup(|app| {
            commands::features::cleanup_legacy_installations();
            let handle = app.handle().clone();
            let handle_for_clipboard = app.handle().clone();
            std::thread::spawn(move || {
                network::tcp_server::start_server(Some(handle));
            });
            network::tcp_server::start_clipboard_watcher(Some(handle_for_clipboard));
            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            get_devices,
            get_device_info,
            list_phone_files,
            upload_phone_file,
            download_phone_file,
            get_paired_devices,
            confirm_pairing,
            reject_pairing,
            unpair_device,
            send_clipboard,
            ping_device,
            get_network_info,
            get_received_files,
            open_downloads_folder,
            open_file,
            delete_received_file,
            send_file_to_device,
            send_file_bytes,
            pick_files_dialog,
            get_app_info,
            open_url,
            install_desktop_update,
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
