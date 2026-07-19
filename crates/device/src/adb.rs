use crate::PhoneFile;
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

pub fn list_files(serial: &str, path: &str) -> Vec<PhoneFile> {
    let output = adb_shell(serial, &["sh", "-c", &format!("ls -Ap \"{}\"", path)]);

    output
        .lines()
        .filter(|line| !line.trim().is_empty())
        .map(|line| {
            let name = line.trim();

            PhoneFile {
                name: name.trim_end_matches('/').to_string(),
                path: format!("{}/{}", path, name.trim_end_matches('/')),
                is_directory: name.ends_with('/'),
                size: 0,
            }
        })
        .collect()
}
pub fn upload_file(serial: &str, local: &str, remote: &str) {
    Command::new("adb")
        .arg("-s")
        .arg(serial)
        .arg("push")
        .arg(local)
        .arg(remote)
        .status()
        .expect("Failed to upload file");
}

pub fn download_file(serial: &str, remote: &str, local: &str) {
    Command::new("adb")
        .arg("-s")
        .arg(serial)
        .arg("pull")
        .arg(remote)
        .arg(local)
        .status()
        .expect("Failed to download file");
}

pub fn delete_file(serial: &str, path: &str) {
    Command::new("adb")
        .arg("-s")
        .arg(serial)
        .arg("shell")
        .arg("rm")
        .arg(path)
        .status()
        .expect("Failed to delete file");
}

pub fn rename_file(serial: &str, old: &str, new: &str) {
    Command::new("adb")
        .arg("-s")
        .arg(serial)
        .arg("shell")
        .arg("mv")
        .arg(old)
        .arg(new)
        .status()
        .expect("Failed to rename file");
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

#[test]
fn test_list_download_folder() {
    let devices = list_devices();

    if let Some(serial) = devices.first() {
        let files = list_files(serial, "/sdcard/Download");

        println!("{:#?}", files);
    }
}
