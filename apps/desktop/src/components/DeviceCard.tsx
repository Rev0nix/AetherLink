import type { Device } from "../types/device";
import { useNavigate } from "react-router-dom";
import { Smartphone, BatteryCharging, ArrowRight } from "lucide-react";

interface Props {
  device: Device;
}

export default function DeviceCard({ device }: Props) {
  const navigate = useNavigate();

  return (
    <div
      onClick={() => navigate(`/device/${device.id}`)}
      className="p-4 rounded-2xl bg-[#171D2B]/90 hover:bg-[#171D2B] border border-[#20283A] hover:border-[#7C5CFC]/40 transition-all cursor-pointer shadow-sm group space-y-3"
    >
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-3">
          <div
            className={`w-10 h-10 rounded-xl flex items-center justify-center border ${
              device.connected
                ? "bg-[#35D49A]/15 text-[#35D49A] border-[#35D49A]/30"
                : "bg-[#20283A] text-[#929BB0] border-[#20283A]"
            }`}
          >
            <Smartphone size={20} />
          </div>
          <div>
            <h3 className="text-sm font-bold text-white group-hover:text-[#32C5FF] transition-colors">
              {device.name}
            </h3>
            <span className="text-[11px] text-[#929BB0]">
              {device.platform}
            </span>
          </div>
        </div>

        <span
          className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold ${
            device.connected
              ? "bg-[#35D49A]/15 text-[#35D49A]"
              : "bg-[#20283A] text-[#929BB0]"
          }`}
        >
          <span
            className={`w-1.5 h-1.5 rounded-full ${
              device.connected ? "bg-[#35D49A] animate-pulse" : "bg-[#929BB0]"
            }`}
          />
          {device.connected ? "Online" : "Offline"}
        </span>
      </div>

      <div className="flex items-center justify-between text-xs text-[#929BB0] pt-1 border-t border-[#20283A]/60">
        <span className="flex items-center gap-1 text-[#35D49A]">
          <BatteryCharging size={13} />
          {device.battery}%
        </span>
        <div className="flex items-center gap-1 text-xs text-[#7C5CFC] font-semibold group-hover:translate-x-0.5 transition-transform">
          <span>Manage</span>
          <ArrowRight size={12} />
        </div>
      </div>
    </div>
  );
}
