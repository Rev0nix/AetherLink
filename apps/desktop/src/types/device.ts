export type Platform =
  | "Android"
  | "Linux"
  | "Windows"
  | "MacOS";

export interface Device {
  id: string;
  name: string;
  platform: Platform;
  battery: number;
  connected: boolean;
}