use device::Device;
use device::DeviceService;

#[tauri::command]
fn get_devices() -> Vec<Device> {
    let devices = DeviceService::discover();

    println!("==============================");
    println!("Found {} devices", devices.len());

    for d in &devices {
        println!("{:?}", d);
    }

    println!("==============================");

    devices
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![get_devices])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}