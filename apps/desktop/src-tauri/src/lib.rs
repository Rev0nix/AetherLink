use serde::Serialize;

#[derive(Serialize)]
struct Device {
    id: String,
    name: String,
    platform: String,
    battery: u8,
    connected: bool,
}

#[tauri::command]
fn get_devices() -> Vec<Device> {
    vec![
        Device {
            id: "pixel-8".into(),
            name: "Google Pixel 8 Pro".into(),
            platform: "Android".into(),
            battery: 87,
            connected: true,
        },
        Device {
            id: "s24".into(),
            name: "Samsung Galaxy S24".into(),
            platform: "Android".into(),
            battery: 63,
            connected: false,
        },
    ]
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![get_devices])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}