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
  BatteryCharging,
  Sparkles,
  Download,
} from "lucide-react";
import { NavLink, useLocation } from "react-router-dom";
import { useAppStore } from "../store/appStore";
import { getNetworkInfo, NetworkInfo, getAppInfo } from "../services/tauri";
import { useTranslation } from "../i18n";
import { useUpdateStore } from "../services/updateService";

export default function Sidebar() {
  const { devices } = useAppStore();
  const { t } = useTranslation();
  const { isUpdateAvailable, currentVersion } = useUpdateStore();
  const [networkInfo, setNetworkInfo] = useState<NetworkInfo | null>(null);
  const [appVersion, setAppVersion] = useState(currentVersion);
  const location = useLocation();

  useEffect(() => {
    void getNetworkInfo().then(setNetworkInfo).catch(() => {});
    void getAppInfo().then((info) => {
      if (info?.version) setAppVersion(info.version);
    }).catch(() => {});
  }, []);

  const menuItems = [
    {
      name: t("nav.dashboard"),
      icon: LayoutDashboard,
      path: "/",
      badge: null,
    },
    {
      name: t("nav.devices"),
      icon: Smartphone,
      path: "/devices",
      badge: "live",
    },
    {
      name: t("nav.clipboard"),
      icon: Clipboard,
      path: "/clipboard",
      badge: null,
    },
    {
      name: t("nav.notifications"),
      icon: Bell,
      path: "/notifications",
      badge: null,
    },
    {
      name: t("nav.files"),
      icon: FolderSync,
      path: "/files",
      badge: null,
    },
    {
      name: t("nav.settings"),
      icon: Settings,
      path: "/settings",
      badge: isUpdateAvailable ? "update" : null,
    },
  ];

  const activeDevice = devices.find((d) => d.connected);
  const connectedCount = devices.filter((d) => d.connected).length;

  return (
    <aside className="w-72 bg-[var(--bg-secondary)] border-r border-[var(--border-color)] flex flex-col justify-between select-none relative z-20 backdrop-blur-2xl text-[var(--text-primary)]">
      {/* Top Branding */}
      <div>
        <div className="p-6 border-b border-[var(--border-color)]">
          <div className="flex items-center gap-3">
            <div className="relative">
              <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-[#7C5CFC] via-[#32C5FF] to-cyan-300 flex items-center justify-center shadow-lg shadow-[#7C5CFC]/30 overflow-hidden border border-[#32C5FF]/40">
                <img src="/src/assets/logo.png" alt="VEXYRON" className="w-full h-full object-cover" />
              </div>
              <span className="absolute -bottom-1 -right-1 flex h-3 w-3">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#35D49A] opacity-75"></span>
                <span className="relative inline-flex rounded-full h-3 w-3 bg-[#35D49A] border-2 border-[var(--bg-secondary)]"></span>
              </span>
            </div>

            <div className="space-y-0.5">
              <div className="flex items-center gap-1.5">
                <h1 className="text-xl font-extrabold tracking-tight text-[var(--text-primary)]">
                  VEXY<span className="text-[#32C5FF]">RON</span>
                </h1>
                <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-[var(--bg-surface)] text-[#7C5CFC] font-bold">
                  v{appVersion}
                </span>
              </div>
              <p className="text-[10px] font-medium text-[var(--text-secondary)] tracking-wide uppercase">
                {t("nav.tagline")}
              </p>
            </div>
          </div>
        </div>

        {/* Live Device Status Pill */}
        <div className="px-4 py-3">
          <div className="p-3.5 rounded-2xl bg-[var(--bg-card)] border border-[var(--border-color)] shadow-inner space-y-2">
            <div className="flex items-center justify-between text-xs">
              <span className="text-[var(--text-secondary)] font-semibold text-[11px] uppercase tracking-wider flex items-center gap-1.5">
                <Wifi size={13} className="text-[#32C5FF]" />
                Connectivity
              </span>
              <span
                className={`text-[11px] font-bold px-2 py-0.5 rounded-full flex items-center gap-1 ${
                  connectedCount > 0
                    ? "bg-[#35D49A]/15 text-[#35D49A] border border-[#35D49A]/30"
                    : "bg-[var(--bg-surface)] text-[var(--text-secondary)]"
                }`}
              >
                <span
                  className={`w-1.5 h-1.5 rounded-full ${
                    connectedCount > 0 ? "bg-[#35D49A] animate-pulse" : "bg-[var(--text-secondary)]"
                  }`}
                />
                {connectedCount > 0 ? `${connectedCount} Online` : "Awaiting Client"}
              </span>
            </div>

            {activeDevice ? (
              <div className="space-y-1">
                <div className="text-xs font-bold text-[var(--text-primary)] truncate">
                  {activeDevice.name}
                </div>
                <div className="flex items-center justify-between text-[11px] text-[var(--text-secondary)]">
                  <span className="flex items-center gap-1 text-[#35D49A]">
                    <BatteryCharging size={12} />
                    {activeDevice.battery}%
                  </span>
                  <span className="font-mono text-[#7C5CFC]">Socket Active</span>
                </div>
              </div>
            ) : (
              <div className="text-[11px] text-[var(--text-secondary)] leading-snug">
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
                    ? "bg-gradient-to-r from-[#7C5CFC]/20 to-[#32C5FF]/10 text-[var(--text-primary)] border border-[#7C5CFC]/40 shadow-md shadow-[#7C5CFC]/10"
                    : "text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-[var(--bg-card)]/70"
                }`}
              >
                <div className="flex items-center gap-3">
                  <div
                    className={`p-1.5 rounded-xl transition-all ${
                      isActive
                        ? "bg-[#7C5CFC] text-white shadow-sm shadow-[#7C5CFC]/50"
                        : "bg-[var(--bg-card)] text-[var(--text-secondary)] group-hover:text-[var(--text-primary)]"
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

                {item.badge === "update" && (
                  <span className="flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-[#32C5FF]/20 text-[#32C5FF] border border-[#32C5FF]/40 animate-pulse">
                    <Download size={10} />
                    New
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
      <div className="p-4 border-t border-[var(--border-color)] space-y-2">
        <div className="p-3.5 rounded-2xl bg-[var(--bg-card)] border border-[var(--border-color)] space-y-1.5">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold text-[var(--text-primary)] flex items-center gap-1.5">
              <Cpu size={14} className="text-[#35D49A]" />
              Tokio Core
            </span>
            <span className="text-[10px] font-mono text-[#35D49A] font-semibold bg-[#35D49A]/10 px-1.5 py-0.5 rounded">
              RUNNING
            </span>
          </div>

          <div className="text-[10px] font-mono text-[var(--text-secondary)] space-y-0.5">
            <div className="flex justify-between">
              <span>HOST IP:</span>
              <span className="text-[#32C5FF]">{networkInfo?.local_ip || "192.168.0.118"}</span>
            </div>
            <div className="flex justify-between">
              <span>PORT:</span>
              <span className="text-[var(--text-primary)]">45876</span>
            </div>
          </div>
        </div>

        <div className="text-[10px] text-center text-[var(--text-secondary)]/70 font-mono">
          VEXYRON • v{appVersion}
        </div>
      </div>
    </aside>
  );
}
