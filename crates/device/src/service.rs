use crate::{Device, Platform};
use uuid::Uuid;

pub struct DeviceService;

impl DeviceService {
    pub fn discover() -> Vec<Device> {
        vec![
            Device {
                id: Uuid::new_v4(),
                name: "Google Pixel 8 Pro".into(),
                platform: Platform::Android,
                battery: 87,
                connected: true,
            },
            Device {
                id: Uuid::new_v4(),
                name: "Samsung Galaxy S24".into(),
                platform: Platform::Android,
                battery: 63,
                connected: false,
            },
        ]
    }
}