mod commands;
use commands::device::*;
use device::Device;
use device::DeviceService;


#[tauri::command]
fn list_phone_files(serial: String, path: String) -> Vec<String> {
    device::FileService::list(&serial, &path)
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![
            get_devices,
            get_device_info,
            list_phone_files,
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}


#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![
            get_devices,
            get_device_info,
        ])
        .run(tauri::generate_context!())
        .expect("error while running application");
}