use crate::{adb, Device};

pub struct DeviceService;

impl DeviceService {
    pub fn discover() -> Vec<Device> {
        adb::discover_devices()
    }
}

use crate::DeviceInfo;

impl DeviceService {
    pub fn info(serial: &str) -> DeviceInfo {
        adb::device_info(serial)
    }
}