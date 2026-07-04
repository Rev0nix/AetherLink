import { useEffect } from "react";
import DeviceCard from "../components/DeviceCard";
import { useAppStore } from "../store/appStore";

export default function Dashboard() {
  const { devices, loadDevices } = useAppStore();

  useEffect(() => {
    void loadDevices();

    const timer = setInterval(() => {
      void loadDevices();
    }, 5000);

    return () => clearInterval(timer);
  }, []);

  console.log("Dashboard devices:", devices);

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

      <button
        onClick={loadDevices}
        className="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
      >
        Refresh Devices
      </button>

      <p className="text-lg font-semibold">
        Connected Devices: {devices.length}
      </p>

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