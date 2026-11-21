import { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import {
  QrCode,
  Smartphone,
  RefreshCw,
  ChevronRight,
} from "lucide-react";
import { useAppStore } from "../store/appStore";
import { getNetworkInfo, NetworkInfo } from "../services/tauri";
import { useTranslation } from "../i18n";

export default function TopBar() {
  const location = useLocation();
  const { devices, openQrModal, loadDevices } = useAppStore();
  const { t } = useTranslation();
  const [networkInfo, setNetworkInfo] = useState<NetworkInfo | null>(null);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const getRouteTitle = () => {
    switch (location.pathname) {
      case "/":
        return t("nav.dashboard");
      case "/devices":
        return t("nav.devices");
      case "/clipboard":
        return t("nav.clipboard");
      case "/notifications":
        return t("nav.notifications");
      case "/files":
        return t("nav.files");
      case "/settings":
        return t("topbar.settings_title");
      default:
        return "AetherLink";
    }
  };

  const title = getRouteTitle();
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
    <header className="h-20 bg-[var(--bg-secondary)]/85 backdrop-blur-xl border-b border-[var(--border-color)] px-8 flex items-center justify-between z-10 text-[var(--text-primary)]">
      {/* Title & Breadcrumb */}
      <div className="flex flex-col justify-center">
        <div className="flex items-center gap-2 text-xs font-medium text-[var(--text-secondary)] mb-0.5">
          <span>AetherLink</span>
          <ChevronRight size={12} className="text-[var(--text-secondary)]/60" />
          <span className="text-[#32C5FF] font-semibold">{title}</span>
        </div>
        <h1 className="text-xl font-extrabold tracking-tight text-[var(--text-primary)]">
          {title}
        </h1>
      </div>

      {/* Right Action Bar */}
      <div className="flex items-center gap-3">
        {/* Network IP/Port Badge */}
        <div className="hidden lg:flex items-center gap-2 px-3 py-1.5 rounded-xl bg-[var(--bg-card)] border border-[var(--border-color)] text-xs">
          <span className="w-2 h-2 rounded-full bg-[#35D49A] animate-pulse" />
          <span className="text-[var(--text-secondary)] font-mono">{t("topbar.lan")}:</span>
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
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-[var(--bg-surface)] border border-[var(--border-color)] text-xs text-[var(--text-secondary)]">
            <Smartphone size={14} />
            <span>{t("topbar.no_device")}</span>
          </div>
        )}

        {/* Refresh Button */}
        <button
          onClick={handleRefresh}
          className={`p-2.5 rounded-xl bg-[var(--bg-card)] hover:bg-[var(--bg-surface)] border border-[var(--border-color)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition-all cursor-pointer ${
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
          <span>{t("topbar.pair_phone")}</span>
        </button>
      </div>
    </header>
  );
}
