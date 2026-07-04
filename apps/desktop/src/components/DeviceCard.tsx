import type { Device } from "../types/device";

interface Props {
  device: Device;
}

export default function DeviceCard({ device }: Props) {
  return (
    <div
      style={{
        border: "1px solid #444",
        borderRadius: "12px",
        padding: "16px",
        marginBottom: "12px",
      }}
    >
      <h2 className="text-xl font-bold">{device.name}</h2>

      <p>Platform: {device.platform}</p>

      <p>Battery: {device.battery}%</p>

      <p>
        {device.connected ? "🟢 Connected" : "⚪ Offline"}
      </p>
    </div>
  );
}