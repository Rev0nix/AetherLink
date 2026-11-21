use device::{PairedDevice, PairingManager};

#[tauri::command]
pub fn get_paired_devices() -> Vec<PairedDevice> {
    PairingManager::global().get_paired_devices()
}

#[tauri::command]
pub fn confirm_pairing(device_id: String, pin: String) -> bool {
    PairingManager::global().confirm_pairing(&device_id, &pin)
}

#[tauri::command]
pub fn reject_pairing(device_id: String) {
    PairingManager::global().reject_pairing(&device_id);
}

#[tauri::command]
pub fn unpair_device(device_id: String) -> bool {
    PairingManager::global().unpair_device(&device_id)
}
