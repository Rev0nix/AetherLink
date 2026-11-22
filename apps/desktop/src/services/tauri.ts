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

export interface PairedDevice {
  id: string;
  name: string;
  paired_at: number;
}

export interface PairingRequestEvent {
  device_id: string;
  device_name: string;
  pin: string;
}

export async function getPairedDevices(): Promise<PairedDevice[]> {
  return invoke<PairedDevice[]>("get_paired_devices");
}

export async function confirmPairing(
  deviceId: string,
  pin: string,
): Promise<boolean> {
  return invoke<boolean>("confirm_pairing", {
    deviceId,
    pin,
  });
}

export async function rejectPairing(deviceId: string): Promise<void> {
  return invoke("reject_pairing", { deviceId });
}

export async function unpairDevice(deviceId: string): Promise<boolean> {
  return invoke<boolean>("unpair_device", { deviceId });
}

export interface NetworkInfo {
  port: number;
  mdns_service: string;
  local_ip: string;
  connected_count: number;
}

export interface ClipboardItem {
  id: string;
  content: string;
  timestamp: number;
  source: string;
  pinned?: boolean;
}

export interface NotificationItem {
  id: string;
  app_name: string;
  package_name: string;
  title: string;
  text: string;
  timestamp: number;
}

export async function sendClipboard(
  content: string,
  targetDeviceId?: string,
): Promise<boolean> {
  return invoke<boolean>("send_clipboard", {
    content,
    targetDeviceId: targetDeviceId ?? null,
  });
}

export async function pingDevice(deviceId: string): Promise<boolean> {
  return invoke<boolean>("ping_device", { deviceId });
}

export async function getNetworkInfo(): Promise<NetworkInfo> {
  return invoke<NetworkInfo>("get_network_info");
}

export interface ReceivedFile {
  name: string;
  path: string;
  size: number;
  category: "image" | "video" | "audio" | "document" | "archive" | "other";
  modified: number;
  extension: string;
}

export interface FileSendProgressEvent {
  file_id: string;
  file_name: string;
  progress: number;
  sent_bytes: number;
  total_bytes: number;
  status: string;
}

export interface FileSendCompleteEvent {
  file_id: string;
  file_name: string;
  total_bytes: number;
  status: string;
}

export interface FileReceivedEvent {
  file_id: string;
  file_name: string;
  path: string;
}

export async function getReceivedFiles(): Promise<ReceivedFile[]> {
  return invoke<ReceivedFile[]>("get_received_files");
}

export async function openDownloadsFolder(): Promise<void> {
  return invoke("open_downloads_folder");
}

export async function openFile(path: string): Promise<void> {
  return invoke("open_file", { path });
}

export async function deleteReceivedFile(path: string): Promise<void> {
  return invoke("delete_received_file", { path });
}

export async function sendFileToDevice(
  deviceId: string,
  filePath: string,
): Promise<string> {
  return invoke<string>("send_file_to_device", {
    deviceId,
    filePath,
  });
}

export async function sendFileBytes(
  deviceId: string,
  fileName: string,
  fileBytes: number[],
): Promise<string> {
  return invoke<string>("send_file_bytes", {
    deviceId,
    fileName,
    fileBytes,
  });
}

export async function pickFilesDialog(): Promise<string[]> {
  try {
    return await invoke<string[]>("pick_files_dialog");
  } catch (err) {
    console.warn("pick_files_dialog invoke error, falling back:", err);
    return [];
  }
}

export interface AppInfo {
  version: string;
  name: string;
  os: string;
  arch: string;
}

export async function getAppInfo(): Promise<AppInfo> {
  try {
    return await invoke<AppInfo>("get_app_info");
  } catch (err) {
    console.warn("get_app_info fallback:", err);
    return {
      version: "2.2.2",
      name: "VEXYRON",
      os: "linux",
      arch: "x86_64",
    };
  }
}

export async function openUrl(url: string): Promise<void> {
  try {
    await invoke("open_url", { url });
  } catch (_e) {
    window.open(url, "_blank");
  }
}

export async function installDesktopUpdate(url: string): Promise<string> {
  return await invoke<string>("install_desktop_update", { url });
}
