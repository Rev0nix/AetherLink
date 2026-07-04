use device::Device;

use crate::AppState;

pub struct CoreApi {
    state: AppState,
}

impl CoreApi {
    pub fn new() -> Self {
        Self {
            state: AppState::new(),
        }
    }

    pub fn devices(&self) -> &[Device] {
        self.state.device_manager.devices()
    }

    pub fn connected_devices(&self) -> usize {
        self.state.device_manager.connected()
    }
}