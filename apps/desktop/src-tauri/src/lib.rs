mod commands;

use commands::device::*;

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
        .invoke_handler(tauri::generate_handler![
            get_devices,
            get_device_info,
            list_phone_files,
            upload_phone_file,
            download_phone_file,
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
