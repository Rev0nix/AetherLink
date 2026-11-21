import { useEffect, useState } from "react";
import { useLocation, Link } from "react-router-dom";
import {
  QrCode,
  Smartphone,
  Wifi,
  Search,
  Bell,
  RefreshCw,
  ExternalLink,
  ShieldCheck,
  ChevronRight,
} from "lucide-react";
import { useAppStore } from "../store/appStore";
import { getNetworkInfo, NetworkInfo } from "../services/tauri";

const routeMeta: Record<string, { title: string; subtitle: string }> = {
  "/": {
    title: "Command Center",
    subtitle: "Real-time synchronization and connectivity dashboard",
  },
  "/devices": {
    title: "Device Management",
    subtitle: "Paired smartphones, network probes, and security keys",
  },
  "/clipboard": {
    title: "Clipboard Hub",
    subtitle: "Seamless bidirectional cross-device clipboard synchronization",
  },
  "/notifications": {
    title: "Notification Stream",
    subtitle: "Mirror and manage Android notifications on your desktop",
  },
  "/files": {
    title: "Storage & File Transfer",
    subtitle: "Browse phone storage and transfer files wirelessly",
  },
  "/settings": {
    title: "System Settings",
    subtitle: "Socket configuration, network ports, and sync preferences",
  },
};

export default function TopBar() {
  const location = useLocation();
  const { devices, openQrModal, loadDevices } = useAppStore();
  const [networkInfo, setNetworkInfo] = useState<NetworkInfo | null>(null);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const meta = routeMeta[location.pathname] || {
    title: "AetherLink",
    subtitle: "Cross-platform device ecosystem",
  };

  const activeDevice = devices.find((d) => d.connected);

  useEffect(() => {
    void getNetworkInfo().then(setNetworkInfo).catch(() => {});
  }, []);

  const handleRefresh = async () => {
    setIsRefreshing(true);
    try {
      await loadDevices();
      const net = await getNetworkInfo();
      setNetworkInfo(net);
    } catch (e) {
      console.error(e);
    } finally {
      setTimeout(() => setIsRefreshing(false), 500);
    }
  };

  return (
    <header className="h-20 bg-[#111522]/80 backdrop-blur-xl border-b border-[#20283A] px-8 flex items-center justify-between z-10">
      {/* Title & Breadcrumb */}
      <div className="flex flex-col justify-center">
        <div className="flex items-center gap-2 text-xs font-medium text-[#929BB0] mb-0.5">
          <span>AetherLink</span>
          <ChevronRight size={12} className="text-[#929BB0]/60" />
          <span className="text-[#32C5FF] font-semibold">{meta.title}</span>
        </div>
        <h1 className="text-xl font-extrabold tracking-tight text-[#F5F7FF]">
          {meta.title}
        </h1>
      </div>

      {/* Right Action Bar */}
      <div className="flex items-center gap-3">
        {/* Network IP/Port Badge */}
        <div className="hidden lg:flex items-center gap-2 px-3 py-1.5 rounded-xl bg-[#171D2B] border border-[#20283A] text-xs">
          <span className="w-2 h-2 rounded-full bg-[#35D49A] animate-pulse" />
          <span className="text-[#929BB0] font-mono">LAN:</span>
          <span className="font-mono font-bold text-[#32C5FF]">
            {networkInfo?.local_ip || "192.168.0.118"}:45876
          </span>
        </div>

        {/* Active Phone Pill */}
        {activeDevice ? (
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-[#35D49A]/10 border border-[#35D49A]/30 text-xs text-[#35D49A]">
            <Smartphone size={14} />
            <span className="font-semibold">{activeDevice.name}</span>
            <span className="text-[10px] bg-[#35D49A]/20 px-1.5 py-0.5 rounded font-mono font-bold">
              {activeDevice.battery}%
            </span>
          </div>
        ) : (
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-[#20283A]/70 border border-[#20283A] text-xs text-[#929BB0]">
            <Smartphone size={14} />
            <span>No Phone Active</span>
          </div>
        )}

        {/* Refresh Button */}
        <button
          onClick={handleRefresh}
          className={`p-2.5 rounded-xl bg-[#171D2B] hover:bg-[#20283A] border border-[#20283A] text-[#929BB0] hover:text-[#F5F7FF] transition-all cursor-pointer ${
            isRefreshing ? "animate-spin text-[#32C5FF]" : ""
          }`}
          title="Refresh Network & Devices"
        >
          <RefreshCw size={16} />
        </button>

        {/* Pair Device Button (Triggers QR Modal) */}
        <button
          onClick={openQrModal}
          className="flex items-center gap-2 px-4 py-2 rounded-xl bg-gradient-to-r from-[#7C5CFC] to-[#32C5FF] hover:from-[#6b4bf0] hover:to-[#22b7f0] text-white text-xs font-bold transition-all shadow-lg shadow-[#7C5CFC]/25 cursor-pointer active:scale-95"
        >
          <QrCode size={16} />
          <span>Pair Phone</span>
        </button>
      </div>
    </header>
  );
}
