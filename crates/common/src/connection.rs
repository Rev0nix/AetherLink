use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ConnectionType {
    Usb,
    Wifi,
    Bluetooth,
    Relay,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ConnectionState {
    Connected,
    Connecting,
    Disconnected,
    Pairing,
}