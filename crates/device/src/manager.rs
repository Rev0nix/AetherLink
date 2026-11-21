use std::sync::{Arc, OnceLock, RwLock};

use crate::{
    device::Device,
    state::DeviceState,
};

#[derive(Clone)]
pub struct DeviceManager {
    state: Arc<RwLock<DeviceState>>,
}

static INSTANCE: OnceLock<DeviceManager> = OnceLock::new();

impl Default for DeviceManager {
    fn default() -> Self {
        Self::new()
    }
}

impl DeviceManager {
    pub fn global() -> &'static DeviceManager {
        INSTANCE.get_or_init(DeviceManager::new)
    }

    pub fn new() -> Self {
        Self {
            state: Arc::new(RwLock::new(DeviceState::default())),
        }
    }

    pub fn register(&self, device: Device) {
        let mut state = self.state.write().unwrap();
        // Remove any old device entry that has the same name or IP address to prevent duplicates
        state.devices.retain(|_, d| {
            d.name != device.name
                && (device.ip_address.is_none() || d.ip_address != device.ip_address)
        });
        state.devices.insert(device.id.clone(), device);
    }

    pub fn set_connected(&self, id: &str, connected: bool) {
        let mut state = self.state.write().unwrap();
        if !connected {
            state.devices.remove(id);
        } else if let Some(dev) = state.devices.get_mut(id) {
            dev.connected = true;
            dev.last_seen = Some(chrono::Utc::now().timestamp_millis());
        }
    }

    pub fn set_battery(&self, id: &str, battery: u8) {
        let mut state = self.state.write().unwrap();
        if let Some(dev) = state.devices.get_mut(id) {
            dev.battery = battery;
            dev.last_seen = Some(chrono::Utc::now().timestamp_millis());
        }
    }

    pub fn remove(&self, id: &str) {
        self.state
            .write()
            .unwrap()
            .devices
            .remove(id);
    }

    pub fn devices(&self) -> Vec<Device> {
        let state = self.state.read().unwrap();
        let mut list: Vec<Device> = Vec::new();
        for dev in state.devices.values() {
            if let Some(existing) = list.iter_mut().find(|d| d.name == dev.name) {
                if dev.connected && !existing.connected {
                    *existing = dev.clone();
                }
            } else {
                list.push(dev.clone());
            }
        }
        list
    }

    pub fn get_device(&self, id: &str) -> Option<Device> {
        self.state
            .read()
            .unwrap()
            .devices
            .get(id)
            .cloned()
    }
}
