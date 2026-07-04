import { useEffect } from "react";
import DeviceCard from "../components/DeviceCard";
import { useAppStore } from "../store/appStore";

export default function Dashboard() {
  const { devices, loadDevices } = useAppStore();

  useEffect(() => {
    loadDevices();
  }, [loadDevices]);

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-4xl font-bold">
          Welcome to AetherLink
        </h1>

        <p className="text-zinc-400">
          Connected Android Devices
        </p>
      </div>

      <div className="space-y-4">
        {devices.map((device) => (
          <DeviceCard
            key={device.id}
            device={device}
          />
        ))}
      </div>
    </div>
  );
}