use crate::{adb, Device, DeviceInfo, DeviceManager};

pub struct DeviceService;

impl DeviceService {
    pub fn discover() -> Vec<Device> {
        let mut devices = DeviceManager::global().devices();

        // Also check ADB for wired devices
        let adb_devices = adb::discover_devices();
        for adb_dev in adb_devices {
            if !devices.iter().any(|d| d.id == adb_dev.id || d.name == adb_dev.name) {
                devices.push(adb_dev);
            }
        }

        // Deduplicate devices by name (prefer connected/active ones)
        let mut unique_devices: Vec<Device> = Vec::new();
        for dev in devices {
            if let Some(existing) = unique_devices.iter_mut().find(|d| d.name == dev.name) {
                if dev.connected && !existing.connected {
                    *existing = dev;
                }
            } else {
                unique_devices.push(dev);
            }
        }

        unique_devices
    }

    pub fn info(serial: &str) -> DeviceInfo {
        adb::device_info(serial)
    }
}
