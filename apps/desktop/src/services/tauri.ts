import { invoke } from "@tauri-apps/api/core";
import type { Device } from "../types/device";

export async function getDevices(): Promise<Device[]> {
  return invoke<Device[]>("get_devices");
}

export async function listPhoneFiles(
  serial: string,
  path: string,
): Promise<string[]> {
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