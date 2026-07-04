import { getDevices } from "../services/tauri";
import { create } from "zustand";
import type { Device } from "../types/device";

interface AppState {
  devices: Device[];
  loadDevices: () => Promise<void>;
}

export const useAppStore = create<AppState>((set) => ({
  devices: [],

  loadDevices: async () => {
    try {
      const devices = await getDevices();

      console.log("Devices from backend:", devices);

      set({ devices });
    } catch (err) {
      console.error("Error loading devices:", err);
    }
  },
}));