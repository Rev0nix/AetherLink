import { invoke } from "@tauri-apps/api/core";
import type { Device } from "../types/device";

export async function getDevices(): Promise<Device[]> {
  return invoke<Device[]>("get_devices");
}