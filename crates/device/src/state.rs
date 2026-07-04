use std::collections::HashMap;

use crate::device::Device;

#[derive(Default)]
pub struct DeviceState {
    pub devices: HashMap<String, Device>,
}