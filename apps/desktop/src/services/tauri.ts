import { invoke } from "@tauri-apps/api/core";
import type { Device } from "../types/device";
import type { PhoneFile } from "../types/file";

export async function getDevices(): Promise<Device[]> {
  return invoke<Device[]>("get_devices");
}

export async function listPhoneFiles(
  serial: string,
  path: string,
): Promise<PhoneFile[]> {
  return invoke("list_phone_files", {
    serial,
    path,
  });
}

export interface DeviceInfo {
  serial: string;
  manufacturer: string;
  model: string;
  android_version: string;
  battery: number;
}

export async function getDeviceInfo(
  serial: string
): Promise<DeviceInfo> {
  return invoke("get_device_info", { serial });
}

export async function uploadPhoneFile(
  serial: string,
  local: string,
  remote: string,
) {
  return invoke("upload_phone_file", {
    serial,
    local,
    remote,
  });
}

export async function downloadPhoneFile(
  serial: string,
  remote: string,
  local: string,
) {
  return invoke("download_phone_file", {
    serial,
    remote,
    local,
  });
}