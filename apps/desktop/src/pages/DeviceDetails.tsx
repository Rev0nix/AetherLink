import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { getDeviceInfo, type DeviceInfo } from "../services/tauri";
import { useAppStore } from "../store/appStore";
import DeviceActions from "../components/device/DeviceActions";

export default function DeviceDetails() {
  const { id } = useParams();

  const device = useAppStore((state) =>
    state.devices.find((d) => d.id === id)
  );

  const [info, setInfo] = useState<DeviceInfo | null>(null);

  useEffect(() => {
    if (!device) return;

    void getDeviceInfo(device.id).then(setInfo);
  }, [device]);

  if (!device) {
    return <h1>Device not found</h1>;
  }

  if (!info) {
    return <h1>Loading...</h1>;
  }

  return (
    <div className="space-y-6">
      <h1 className="text-4xl font-bold">
        Device Details
      </h1>

      <div className="rounded-xl border border-zinc-700 p-6 space-y-4">
        <h2 className="text-2xl font-bold">
          {info.model}
        </h2>

        <p>Manufacturer: {info.manufacturer}</p>

        <p>Platform: {device.platform}</p>

        <p>Android Version: {info.android_version}</p>

        <p>Battery: {info.battery}%</p>

        <p>Serial: {info.serial}</p>

        <p>
          Status: {device.connected ? "🟢 Connected" : "⚪ Offline"}
        </p>
      </div>

      <DeviceActions
        onBrowseFiles={() => {
          console.log("Browse Files clicked");
        }}
      />
    </div>
  );
}