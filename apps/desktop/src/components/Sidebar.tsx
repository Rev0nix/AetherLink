import { useEffect, useState } from "react";
import {
  LayoutDashboard,
  Smartphone,
  Clipboard,
  Bell,
  Settings,
  FolderSync,
  Cpu,
  Wifi,
  QrCode,
  BatteryCharging,
  Sparkles,
  ChevronRight,
} from "lucide-react";
import { NavLink, useLocation } from "react-router-dom";
import { useAppStore } from "../store/appStore";
import { getNetworkInfo, NetworkInfo } from "../services/tauri";

const menuItems = [
  {
    name: "Dashboard",
    icon: LayoutDashboard,
    path: "/",
    badge: null,
  },
  {
    name: "Devices",
    icon: Smartphone,
    path: "/devices",
    badge: "live",
  },
  {
    name: "Clipboard Hub",
    icon: Clipboard,
    path: "/clipboard",
    badge: null,
  },
  {
    name: "Notifications",
    icon: Bell,
    path: "/notifications",
    badge: null,
  },
  {
    name: "File Explorer",
    icon: FolderSync,
    path: "/files",
    badge: null,
  },
  {
    name: "Settings",
    icon: Settings,
    path: "/settings",
    badge: null,
  },
];

export default function Sidebar() {
  const { devices } = useAppStore();
  const [networkInfo, setNetworkInfo] = useState<NetworkInfo | null>(null);
  const location = useLocation();

  useEffect(() => {
    void getNetworkInfo().then(setNetworkInfo).catch(() => {});
  }, []);

  const activeDevice = devices.find((d) => d.connected);
  const connectedCount = devices.filter((d) => d.connected).length;

  return (
    <aside className="w-72 bg-[#111522]/95 border-r border-[#20283A] flex flex-col justify-between select-none relative z-20 backdrop-blur-2xl">
      {/* Top Branding */}
      <div>
        <div className="p-6 border-b border-[#20283A]/80">
          <div className="flex items-center gap-3">
            <div className="relative">
              <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-[#7C5CFC] via-[#32C5FF] to-cyan-300 flex items-center justify-center shadow-lg shadow-[#7C5CFC]/30">
                <Sparkles size={22} className="text-white" />
              </div>
              <span className="absolute -bottom-1 -right-1 flex h-3 w-3">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#35D49A] opacity-75"></span>
                <span className="relative inline-flex rounded-full h-3 w-3 bg-[#35D49A] border-2 border-[#111522]"></span>
              </span>
            </div>

            <div className="space-y-0.5">
              <div className="flex items-center gap-1.5">
                <h1 className="text-xl font-extrabold tracking-tight text-[#F5F7FF]">
                  Aether<span className="text-[#32C5FF]">Link</span>
                </h1>
                <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-[#20283A] text-[#7C5CFC] font-bold">
                  v1.0
                </span>
              </div>
              <p className="text-[11px] font-medium text-[#929BB0] tracking-wide uppercase">
                One Connection. Every Device.
              </p>
            </div>
          </div>
        </div>

        {/* Live Device Status Pill */}
        <div className="px-4 py-3">
          <div className="p-3.5 rounded-2xl bg-[#171D2B]/90 border border-[#20283A] shadow-inner space-y-2">
            <div className="flex items-center justify-between text-xs">
              <span className="text-[#929BB0] font-semibold text-[11px] uppercase tracking-wider flex items-center gap-1.5">
                <Wifi size={13} className="text-[#32C5FF]" />
                Connectivity
              </span>
              <span
                className={`text-[11px] font-bold px-2 py-0.5 rounded-full flex items-center gap-1 ${
                  connectedCount > 0
                    ? "bg-[#35D49A]/15 text-[#35D49A] border border-[#35D49A]/30"
                    : "bg-[#20283A] text-[#929BB0]"
                }`}
              >
                <span
                  className={`w-1.5 h-1.5 rounded-full ${
                    connectedCount > 0 ? "bg-[#35D49A] animate-pulse" : "bg-[#929BB0]"
                  }`}
                />
                {connectedCount > 0 ? `${connectedCount} Phone Online` : "Awaiting Client"}
              </span>
            </div>

            {activeDevice ? (
              <div className="space-y-1">
                <div className="text-xs font-bold text-[#F5F7FF] truncate">
                  {activeDevice.name}
                </div>
                <div className="flex items-center justify-between text-[11px] text-[#929BB0]">
                  <span className="flex items-center gap-1 text-[#35D49A]">
                    <BatteryCharging size={12} />
                    {activeDevice.battery}%
                  </span>
                  <span className="font-mono text-[#7C5CFC]">Socket Active</span>
                </div>
              </div>
            ) : (
              <div className="text-[11px] text-[#929BB0] leading-snug">
                Port <span className="font-mono text-[#32C5FF]">45876</span> ready. Open app on phone to link.
              </div>
            )}
          </div>
        </div>

        {/* Navigation Menu */}
        <nav className="p-3 space-y-1.5">
          {menuItems.map((item) => {
            const isActive = location.pathname === item.path;
            const Icon = item.icon;

            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={`group flex items-center justify-between rounded-2xl px-4 py-3 text-sm font-semibold transition-all cursor-pointer relative ${
                  isActive
                    ? "bg-gradient-to-r from-[#7C5CFC]/20 to-[#32C5FF]/10 text-white border border-[#7C5CFC]/40 shadow-md shadow-[#7C5CFC]/10"
                    : "text-[#929BB0] hover:text-[#F5F7FF] hover:bg-[#171D2B]/70"
                }`}
              >
                <div className="flex items-center gap-3">
                  <div
                    className={`p-1.5 rounded-xl transition-all ${
                      isActive
                        ? "bg-[#7C5CFC] text-white shadow-sm shadow-[#7C5CFC]/50"
                        : "bg-[#171D2B] text-[#929BB0] group-hover:text-white"
                    }`}
                  >
                    <Icon size={17} />
                  </div>
                  <span>{item.name}</span>
                </div>

                {item.badge === "live" && connectedCount > 0 && (
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-[#35D49A]/20 text-[#35D49A] border border-[#35D49A]/30">
                    {connectedCount}
                  </span>
                )}

                {isActive && (
                  <span className="w-1.5 h-4 rounded-full bg-[#32C5FF] absolute right-2" />
                )}
              </NavLink>
            );
          })}
        </nav>
      </div>

      {/* Bottom Core Engine Telemetry */}
      <div className="p-4 border-t border-[#20283A]/80 space-y-2">
        <div className="p-3.5 rounded-2xl bg-[#090B14]/80 border border-[#20283A] space-y-1.5">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold text-[#F5F7FF] flex items-center gap-1.5">
              <Cpu size={14} className="text-[#35D49A]" />
              Tokio Core
            </span>
            <span className="text-[10px] font-mono text-[#35D49A] font-semibold bg-[#35D49A]/10 px-1.5 py-0.5 rounded">
              RUNNING
            </span>
          </div>

          <div className="text-[10px] font-mono text-[#929BB0] space-y-0.5">
            <div className="flex justify-between">
              <span>HOST IP:</span>
              <span className="text-[#32C5FF]">{networkInfo?.local_ip || "192.168.0.118"}</span>
            </div>
            <div className="flex justify-between">
              <span>PORT:</span>
              <span className="text-white">45876</span>
            </div>
          </div>
        </div>

        <div className="text-[10px] text-center text-[#929BB0]/60 font-mono">
          AetherLink • Linux Native
        </div>
      </div>
    </aside>
  );
}
