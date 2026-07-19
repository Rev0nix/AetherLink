use serde::{Deserialize, Serialize};

use crate::connection::{ConnectionState, ConnectionType};

/// Represents a connected Android device.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Device {
    pub id: String,
    pub serial: String,
    pub name: String,
    pub manufacturer: String,
    pub model: String,
    pub android_version: String,

    pub battery_level: u8,
    pub is_charging: bool,

    pub connection_type: ConnectionType,
    pub connection_state: ConnectionState,
}