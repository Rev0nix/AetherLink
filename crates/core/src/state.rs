use crate::DeviceManager;

pub struct AppState {
    pub device_manager: DeviceManager,
}

impl AppState {
    pub fn new() -> Self {
        Self {
            device_manager: DeviceManager::new(),
        }
    }
}