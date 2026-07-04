pub mod connection;
pub mod device;
pub mod discovery;
pub mod heartbeat;
pub mod manager;
pub mod pairing;
pub mod service;
pub mod state;
pub mod adb;
pub mod info;
pub mod file;
pub mod file_service;

pub fn get_devices() -> Vec<Device> {
    manager::DeviceManager::default().devices()
}
pub use file_service::FileService;
pub use file::PhoneFile;
pub use info::DeviceInfo;
pub use device::{Device, Platform};
pub use manager::DeviceManager;
pub use service::DeviceService;