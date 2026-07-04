use std::process::Command;

fn adb_shell(serial: &str, command: &[&str]) -> String {
    let output = Command::new("adb")
        .arg("-s")
        .arg(serial)
        .arg("shell")
        .args(command)
        .output()
        .expect("Failed to execute adb");

    String::from_utf8_lossy(&output.stdout).trim().to_string()
}

pub fn list_devices() -> Vec<String> {
    let output = Command::new("adb")
        .arg("devices")
        .output()
        .expect("Failed to execute adb");

    let stdout = String::from_utf8_lossy(&output.stdout);

    stdout
        .lines()
        .skip(1)
        .filter_map(|line| {
            let mut parts = line.split_whitespace();

            let serial = parts.next()?;
            let status = parts.next()?;

            if status == "device" {
                Some(serial.to_string())
            } else {
                None
            }
        })
        .collect()
}

pub fn model(serial: &str) -> String {
    adb_shell(serial, &["getprop", "ro.product.model"])
}

pub fn manufacturer(serial: &str) -> String {
    adb_shell(serial, &["getprop", "ro.product.manufacturer"])
}

pub fn android_version(serial: &str) -> String {
    adb_shell(serial, &["getprop", "ro.build.version.release"])
}

pub fn battery(serial: &str) -> u8 {
    let output = adb_shell(serial, &["dumpsys", "battery"]);

    for line in output.lines() {
        let line = line.trim();

        if let Some(level) = line.strip_prefix("level:") {
            return level.trim().parse::<u8>().unwrap_or(0);
        }
    }

    0
}

use crate::{Device, Platform};


pub fn discover_devices() -> Vec<Device> {
    list_devices()
        .into_iter()
        .map(|serial| Device {
            id: serial.clone(),
            name: model(&serial),
            platform: Platform::Android,
            battery: battery(&serial),
            connected: true,
        })
        .collect()
}

pub fn list_files(serial: &str, path: &str) -> Vec<String> {
    let output = adb_shell(serial, &["ls", path]);

    output
        .lines()
        .map(|line| line.trim().to_string())
        .filter(|line| !line.is_empty())
        .collect()
}

use crate::DeviceInfo;

pub fn device_info(serial: &str) -> DeviceInfo {
    DeviceInfo {
        serial: serial.to_string(),
        manufacturer: manufacturer(serial),
        model: model(serial),
        android_version: android_version(serial),
        battery: battery(serial),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn device_information() {
        let devices = list_devices();

        for serial in devices {
            println!("Serial       : {}", serial);
            println!("Manufacturer : {}", manufacturer(&serial));
            println!("Model        : {}", model(&serial));
            println!("Android      : {}", android_version(&serial));
            println!("Battery");
            println!("{}", battery(&serial));
        }
    }
}

#[test]
fn list_downloads() {
    let devices = list_devices();

    if let Some(serial) = devices.first() {
        let files = list_files(serial, "/sdcard/Download");

        println!("{:#?}", files);
    }
}
