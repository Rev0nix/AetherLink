use device::{Device, DeviceService};

pub struct DeviceManager {
    devices: Vec<Device>,
}

impl DeviceManager {
    pub fn new() -> Self {
        Self {
            devices: DeviceService::discover(),
        }
    }

    pub fn refresh(&mut self) {
        self.devices = DeviceService::discover();
    }

    pub fn devices(&self) -> &[Device] {
        &self.devices
    }

    pub fn connected(&self) -> usize {
        self.devices.iter().filter(|d| d.connected).count()
    }
}