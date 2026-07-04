use crate::{adb, Device};

pub struct DeviceService;

impl DeviceService {
    pub fn discover() -> Vec<Device> {
        adb::discover_devices()
    }
}