import { create } from "zustand";
import type { Device } from "../types/device";

interface AppState {
  devices: Device[];
  loadDevices: () => Promise<void>;
}

export const useAppStore = create<AppState>((set) => ({
  devices: [],

  loadDevices: async () => {
    const devices = await getDevices();
    set({ devices });
  },
}));