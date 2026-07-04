use device::DeviceService;
use device::Device;

pub fn devices() -> Vec<Device> {
    DeviceService::discover()
}