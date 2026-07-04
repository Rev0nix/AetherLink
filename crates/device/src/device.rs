use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum Platform {
    Android,
    Linux,
    Windows,
    MacOS,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Device {
    pub id: String,
    pub name: String,
    pub platform: Platform,
    pub battery: u8,
    pub connected: bool,
}

impl Device {
    pub fn new(
        id: String,
        name: String,
        platform: Platform,
        battery: u8,
        connected: bool,
    ) -> Self {
        Self {
            id,
            name,
            platform,
            battery,
            connected,
        }
    }
}