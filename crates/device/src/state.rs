use std::collections::HashMap;

use uuid::Uuid;

use crate::device::Device;

#[derive(Default)]
pub struct DeviceState {
    pub devices: HashMap<Uuid, Device>,
}