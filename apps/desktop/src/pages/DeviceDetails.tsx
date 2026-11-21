import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  Smartphone,
  Cpu,
  BatteryCharging,
  Wifi,
  ShieldCheck,
  ArrowLeft,
  HardDrive,
} from "lucide-react";
import { getDeviceInfo, type DeviceInfo } from "../services/tauri";
import { useAppStore } from "../store/appStore";
import DeviceActions from "../components/device/DeviceActions";

export default function DeviceDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const device = useAppStore((state) =>
    state.devices.find((d) => d.id === id)
  );

  const [info, setInfo] = useState<DeviceInfo | null>(null);

  useEffect(() => {
    if (!device) return;
    void getDeviceInfo(device.id).then(setInfo);
  }, [device]);

  if (!device) {
    return (
      <div className="p-12 text-center rounded-3xl bg-[#111522] border border-[#20283A] space-y-4">
        <h2 className="text-xl font-bold text-white">Device Not Found</h2>
        <p className="text-xs text-[#929BB0]">
          The requested device record could not be found.
        </p>
        <button
          onClick={() => navigate("/devices")}
          className="px-4 py-2 rounded-xl bg-[#171D2B] text-xs font-semibold text-white"
        >
          Back to Devices
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-8 max-w-4xl mx-auto pb-12">
      {/* Top Header */}
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate(-1)}
          className="p-2.5 rounded-xl bg-[#171D2B] hover:bg-[#20283A] text-[#929BB0] hover:text-white border border-[#20283A] transition cursor-pointer"
        >
          <ArrowLeft size={16} />
        </button>
        <div>
          <h1 className="text-2xl font-black text-white">
            {info?.model || device.name}
          </h1>
          <p className="text-xs text-[#929BB0]">
            Hardware specifications and active connectivity parameters
          </p>
        </div>
      </div>

      {/* Device Specs Card */}
      <div className="rounded-3xl bg-[#111522]/90 border border-[#20283A] p-6 space-y-5 shadow-xl">
        <div className="flex items-center justify-between pb-4 border-b border-[#20283A]">
          <div className="flex items-center gap-3.5">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-[#7C5CFC]/20 to-[#32C5FF]/20 border border-[#7C5CFC]/30 flex items-center justify-center text-[#32C5FF]">
              <Smartphone size={24} />
            </div>
            <div>
              <div className="text-base font-bold text-white">
                {info?.manufacturer || "Android"} {info?.model || device.name}
              </div>
              <div className="text-xs text-[#929BB0] font-mono">
                Serial / ID: {device.id}
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <span
              className={`inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-bold border ${
                device.connected
                  ? "bg-[#35D49A]/15 text-[#35D49A] border-[#35D49A]/30"
                  : "bg-[#20283A] text-[#929BB0] border-[#20283A]"
              }`}
            >
              <span
                className={`w-1.5 h-1.5 rounded-full ${
                  device.connected ? "bg-[#35D49A] animate-pulse" : "bg-[#929BB0]"
                }`}
              />
              {device.connected ? "Connected" : "Offline"}
            </span>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4 text-xs">
          <div className="p-4 rounded-2xl bg-[#171D2B]/80 border border-[#20283A] space-y-1">
            <span className="text-[#929BB0] text-[11px] uppercase tracking-wider font-semibold">
              Operating System
            </span>
            <div className="font-bold text-white text-sm">
              Android {info?.android_version || "14+"}
            </div>
            <p className="text-[10px] text-[#929BB0]">{device.platform}</p>
          </div>

          <div className="p-4 rounded-2xl bg-[#171D2B]/80 border border-[#20283A] space-y-1">
            <span className="text-[#929BB0] text-[11px] uppercase tracking-wider font-semibold">
              Battery Level
            </span>
            <div className="font-bold text-[#35D49A] text-sm flex items-center gap-1.5">
              <BatteryCharging size={16} />
              {info?.battery ?? device.battery}%
            </div>
            <p className="text-[10px] text-[#929BB0]">Live telemetry status</p>
          </div>

          <div className="p-4 rounded-2xl bg-[#171D2B]/80 border border-[#20283A] space-y-1">
            <span className="text-[#929BB0] text-[11px] uppercase tracking-wider font-semibold">
              Socket Protocol
            </span>
            <div className="font-bold text-[#32C5FF] text-sm font-mono">
              Port 45876 (TCP)
            </div>
            <p className="text-[10px] text-[#929BB0]">Encrypted local handshake</p>
          </div>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="space-y-3">
        <h2 className="text-sm font-bold text-white uppercase tracking-wider">
          Quick Device Actions
        </h2>
        <DeviceActions
          onBrowseFiles={() => {
            navigate("/files");
          }}
        />
      </div>
    </div>
  );
}
