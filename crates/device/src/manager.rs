use std::sync::{Arc, RwLock};

use crate::{
    device::Device,
    state::DeviceState,
};

pub struct DeviceManager {
    state: Arc<RwLock<DeviceState>>,
}

impl Default for DeviceManager {
    fn default() -> Self {
        Self {
            state: Arc::new(RwLock::new(DeviceState::default())),
        }
    }
}

impl DeviceManager {
    pub fn register(&self, device: Device) {
        self.state
            .write()
            .unwrap()
            .devices
            .insert(device.id.clone(), device);
    }

    pub fn remove(&self, id: &str) {
        self.state
            .write()
            .unwrap()
            .devices
            .remove(id);
    }

    pub fn devices(&self) -> Vec<Device> {
        self.state
            .read()
            .unwrap()
            .devices
            .values()
            .cloned()
            .collect()
    }
}