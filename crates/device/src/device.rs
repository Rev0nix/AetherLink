use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
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
    #[serde(default)]
    pub ip_address: Option<String>,
    #[serde(default)]
    pub connection_type: Option<String>,
    #[serde(default)]
    pub last_seen: Option<i64>,
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
            ip_address: None,
            connection_type: None,
            last_seen: None,
        }
    }

    pub fn with_details(
        id: String,
        name: String,
        platform: Platform,
        battery: u8,
        connected: bool,
        ip_address: Option<String>,
        connection_type: Option<String>,
    ) -> Self {
        Self {
            id,
            name,
            platform,
            battery,
            connected,
            ip_address,
            connection_type,
            last_seen: Some(chrono::Utc::now().timestamp_millis()),
        }
    }
}
