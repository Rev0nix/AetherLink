use device::{Device, DeviceInfo, DeviceService};

#[tauri::command]
pub fn get_devices() -> Vec<Device> {
    DeviceService::discover()
}

#[tauri::command]
pub fn get_device_info(serial: String) -> DeviceInfo {
    DeviceService::info(&serial)
}

#[tauri::command]
pub fn download_phone_file(serial: String, remote: String, local: String) {
    device::FileService::download(&serial, &remote, &local);
}
