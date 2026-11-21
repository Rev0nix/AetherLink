import { getDevices } from "../services/tauri";
import { create } from "zustand";
import type { Device } from "../types/device";

interface AppState {
  devices: Device[];
  loadDevices: () => Promise<void>;
  isQrModalOpen: boolean;
  openQrModal: () => void;
  closeQrModal: () => void;
  selectedDeviceSerial: string | null;
  setSelectedDeviceSerial: (serial: string | null) => void;
}

export const useAppStore = create<AppState>((set, get) => ({
  devices: [],
  isQrModalOpen: false,
  selectedDeviceSerial: null,

  openQrModal: () => set({ isQrModalOpen: true }),
  closeQrModal: () => set({ isQrModalOpen: false }),
  setSelectedDeviceSerial: (serial) => set({ selectedDeviceSerial: serial }),

  loadDevices: async () => {
    try {
      const devices = await getDevices();
      const currentSelected = get().selectedDeviceSerial;
      const active = devices.find((d) => d.connected);

      set({
        devices,
        selectedDeviceSerial: currentSelected || (active ? active.id : devices[0]?.id || null),
      });
    } catch (err) {
      console.error("Error loading devices:", err);
    }
  },
}));
