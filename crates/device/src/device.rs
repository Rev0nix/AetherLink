use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum Platform {
    Android,
    Linux,
    Windows,
    MacOS,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Device {
    pub id: Uuid,
    pub name: String,
    pub platform: Platform,
    pub battery: u8,
    pub connected: bool,
}

impl Device {
    pub fn new(
        name: String,
        platform: Platform,
        battery: u8,
        connected: bool,
    ) -> Self {
        Self {
            id: Uuid::new_v4(),
            name,
            platform,
            battery,
            connected,
        }
    }
}