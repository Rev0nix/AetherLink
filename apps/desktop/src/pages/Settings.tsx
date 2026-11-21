import { useEffect, useState } from "react";
import {
  Wifi,
  FolderDown,
  Clipboard,
  ShieldCheck,
  Cpu,
  RefreshCw,
  Check,
  Copy,
  Terminal,
  Bell,
  HardDrive,
  Info,
  Shield,
  Layers,
} from "lucide-react";
import { getNetworkInfo, NetworkInfo } from "../services/tauri";

export default function Settings() {
  const [networkInfo, setNetworkInfo] = useState<NetworkInfo | null>(null);
  const [autoCopyClipboard, setAutoCopyClipboard] = useState(true);
  const [autoAcceptFiles, setAutoAcceptFiles] = useState(true);
  const [systemNotifications, setSystemNotifications] = useState(true);
  const [copiedPath, setCopiedPath] = useState(false);
  const [copiedUfw, setCopiedUfw] = useState(false);

  useEffect(() => {
    void fetchNetwork();
  }, []);

  const fetchNetwork = async () => {
    try {
      const info = await getNetworkInfo();
      setNetworkInfo(info);
    } catch (e) {
      console.error("Failed to fetch network info:", e);
    }
  };

  const copyDownloadPath = () => {
    navigator.clipboard.writeText("~/Downloads");
    setCopiedPath(true);
    setTimeout(() => setCopiedPath(false), 2000);
  };

  const copyUfw = () => {
    navigator.clipboard.writeText("sudo ufw allow 45876/tcp && sudo ufw allow 5353/udp");
    setCopiedUfw(true);
    setTimeout(() => setCopiedUfw(false), 2000);
  };

  return (
    <div className="space-y-8 max-w-4xl mx-auto pb-16">
      {/* Header */}
      <div>
        <div className="inline-flex items-center gap-2 rounded-full bg-[#7C5CFC]/15 border border-[#7C5CFC]/30 px-3 py-1 text-xs font-semibold text-[#7C5CFC] mb-2">
          <Shield size={14} />
          Configuration & Preferences
        </div>
        <h1 className="text-3xl font-extrabold text-white">System Settings</h1>
        <p className="text-sm text-[#929BB0]">
          Configure connection ports, synchronization preferences, firewall rules, and download locations.
        </p>
      </div>

      {/* Network & Service Config */}
      <div className="rounded-3xl bg-[#111522]/90 border border-[#20283A] p-6 space-y-5 shadow-xl">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-2xl bg-[#32C5FF]/15 text-[#32C5FF] border border-[#32C5FF]/30">
              <Wifi size={20} />
            </div>
            <div>
              <h2 className="text-base font-bold text-white">
                Network & Wireless Protocol
              </h2>
              <p className="text-xs text-[#929BB0]">
                Local TCP socket stream and zero-config mDNS discovery
              </p>
            </div>
          </div>
          <button
            onClick={fetchNetwork}
            className="p-2 rounded-xl bg-[#171D2B] hover:bg-[#20283A] text-[#929BB0] hover:text-white transition cursor-pointer"
            title="Refresh network status"
          >
            <RefreshCw size={15} />
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
          <div className="p-4 rounded-2xl bg-[#171D2B]/80 border border-[#20283A] space-y-1">
            <span className="text-[#929BB0] font-medium text-[11px] uppercase tracking-wider">
              Local IPv4 Address
            </span>
            <div className="text-sm font-mono text-[#32C5FF] font-bold">
              {networkInfo?.local_ip || "192.168.0.118"}
            </div>
            <p className="text-[11px] text-[#929BB0]">
              Android phone connects to this LAN address
            </p>
          </div>

          <div className="p-4 rounded-2xl bg-[#171D2B]/80 border border-[#20283A] space-y-1">
            <span className="text-[#929BB0] font-medium text-[11px] uppercase tracking-wider">
              TCP Port
            </span>
            <div className="text-sm font-mono text-[#35D49A] font-bold">
              {networkInfo?.port || 45876}
            </div>
            <p className="text-[11px] text-[#929BB0]">
              Listening and handling client data streams
            </p>
          </div>

          <div className="p-4 rounded-2xl bg-[#171D2B]/80 border border-[#20283A] space-y-1 md:col-span-2">
            <span className="text-[#929BB0] font-medium text-[11px] uppercase tracking-wider">
              mDNS Discovery Service Name
            </span>
            <div className="text-xs font-mono text-[#7C5CFC] font-semibold">
              {networkInfo?.mdns_service || "_aetherlink._tcp.local."}
            </div>
            <p className="text-[11px] text-[#929BB0]">
              Broadcasts via multicast DNS so Android automatically discovers this PC without typing manual IPs.
            </p>
          </div>
        </div>

        {/* Firewall helper */}
        <div className="p-4 rounded-2xl bg-[#171D2B]/80 border border-amber-500/20 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-[#FFB84D] flex items-center gap-1.5">
              <Terminal size={14} />
              Ubuntu Firewall (UFW) Configuration
            </span>
            <button
              onClick={copyUfw}
              className="flex items-center gap-1 text-[11px] bg-[#20283A] hover:bg-amber-500/20 text-[#FFB84D] px-2.5 py-1 rounded-lg font-mono transition cursor-pointer"
            >
              {copiedUfw ? <Check size={13} className="text-[#35D49A]" /> : <Copy size={13} />}
              <span>{copiedUfw ? "Copied!" : "Copy"}</span>
            </button>
          </div>
          <p className="text-[11px] text-[#929BB0]">
            If connections from your phone are blocked, execute this command in your Ubuntu bash terminal:
          </p>
          <div className="p-2.5 rounded-xl bg-[#090B14] font-mono text-[10px] text-zinc-300 border border-[#20283A] select-all">
            sudo ufw allow 45876/tcp && sudo ufw allow 5353/udp
          </div>
        </div>
      </div>

      {/* Storage & Transfer Location */}
      <div className="rounded-3xl bg-[#111522]/90 border border-[#20283A] p-6 space-y-5 shadow-xl">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-2xl bg-[#7C5CFC]/15 text-[#7C5CFC] border border-[#7C5CFC]/30">
            <FolderDown size={20} />
          </div>
          <div>
            <h2 className="text-base font-bold text-white">
              Storage & File Transfers
            </h2>
            <p className="text-xs text-[#929BB0]">
              Default directory for files received from connected phones
            </p>
          </div>
        </div>

        <div className="p-4 rounded-2xl bg-[#171D2B]/80 border border-[#20283A] flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="space-y-1">
            <span className="text-[11px] text-[#929BB0] font-medium uppercase tracking-wider">
              Download Destination Folder
            </span>
            <div className="text-xs font-mono text-white font-bold">
              ~/Downloads/
            </div>
          </div>
          <button
            onClick={copyDownloadPath}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-[#20283A] hover:bg-[#7C5CFC]/20 text-[#929BB0] hover:text-[#32C5FF] text-xs font-medium transition cursor-pointer self-start sm:self-auto"
          >
            {copiedPath ? <Check size={13} className="text-[#35D49A]" /> : <Copy size={13} />}
            <span>{copiedPath ? "Copied Path" : "Copy Path"}</span>
          </button>
        </div>
      </div>

      {/* Sync Preferences & Toggles */}
      <div className="rounded-3xl bg-[#111522]/90 border border-[#20283A] p-6 space-y-5 shadow-xl">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-2xl bg-[#35D49A]/15 text-[#35D49A] border border-[#35D49A]/30">
            <Layers size={20} />
          </div>
          <div>
            <h2 className="text-base font-bold text-white">
              Automation Preferences
            </h2>
            <p className="text-xs text-[#929BB0]">
              Control automatic actions upon receiving cross-device events
            </p>
          </div>
        </div>

        <div className="space-y-3">
          {/* Toggle 1 */}
          <div className="flex items-center justify-between p-4 rounded-2xl bg-[#171D2B]/80 border border-[#20283A]">
            <div className="space-y-0.5">
              <div className="text-xs font-bold text-white flex items-center gap-2">
                <Clipboard size={14} className="text-[#32C5FF]" />
                Auto-Copy Phone Clips to Desktop OS Clipboard
              </div>
              <p className="text-[11px] text-[#929BB0]">
                Automatically places text copied on your phone into the desktop clipboard
              </p>
            </div>
            <button
              onClick={() => setAutoCopyClipboard(!autoCopyClipboard)}
              className={`w-12 h-6 rounded-full transition-colors relative cursor-pointer ${
                autoCopyClipboard ? "bg-[#7C5CFC]" : "bg-[#20283A]"
              }`}
            >
              <span
                className={`absolute top-1 left-1 w-4 h-4 rounded-full bg-white transition-transform ${
                  autoCopyClipboard ? "translate-x-6" : "translate-x-0"
                }`}
              />
            </button>
          </div>

          {/* Toggle 2 */}
          <div className="flex items-center justify-between p-4 rounded-2xl bg-[#171D2B]/80 border border-[#20283A]">
            <div className="space-y-0.5">
              <div className="text-xs font-bold text-white flex items-center gap-2">
                <HardDrive size={14} className="text-[#35D49A]" />
                Auto-Accept File Transfers from Trusted Devices
              </div>
              <p className="text-[11px] text-[#929BB0]">
                Automatically save incoming files from paired devices into the download folder
              </p>
            </div>
            <button
              onClick={() => setAutoAcceptFiles(!autoAcceptFiles)}
              className={`w-12 h-6 rounded-full transition-colors relative cursor-pointer ${
                autoAcceptFiles ? "bg-[#7C5CFC]" : "bg-[#20283A]"
              }`}
            >
              <span
                className={`absolute top-1 left-1 w-4 h-4 rounded-full bg-white transition-transform ${
                  autoAcceptFiles ? "translate-x-6" : "translate-x-0"
                }`}
              />
            </button>
          </div>

          {/* Toggle 3 */}
          <div className="flex items-center justify-between p-4 rounded-2xl bg-[#171D2B]/80 border border-[#20283A]">
            <div className="space-y-0.5">
              <div className="text-xs font-bold text-white flex items-center gap-2">
                <Bell size={14} className="text-[#FFB84D]" />
                Show System Desktop Notifications
              </div>
              <p className="text-[11px] text-[#929BB0]">
                Trigger native Linux desktop popups for incoming phone alerts
              </p>
            </div>
            <button
              onClick={() => setSystemNotifications(!systemNotifications)}
              className={`w-12 h-6 rounded-full transition-colors relative cursor-pointer ${
                systemNotifications ? "bg-[#7C5CFC]" : "bg-[#20283A]"
              }`}
            >
              <span
                className={`absolute top-1 left-1 w-4 h-4 rounded-full bg-white transition-transform ${
                  systemNotifications ? "translate-x-6" : "translate-x-0"
                }`}
              />
            </button>
          </div>
        </div>
      </div>

      {/* System Information */}
      <div className="p-5 rounded-3xl bg-[#171D2B]/60 border border-[#20283A] text-xs text-[#929BB0] space-y-2">
        <div className="flex items-center justify-between text-white font-bold">
          <span className="flex items-center gap-2">
            <Cpu size={15} className="text-[#35D49A]" />
            AetherLink Engine Architecture
          </span>
          <span className="text-[11px] font-mono text-[#32C5FF]">v1.0.0-linux</span>
        </div>
        <p className="text-[11px] leading-relaxed">
          Powered by Tauri 2.0, Tokio asynchronous I/O runtime, mdns-sd multi-network advertisement, and Jetpack Compose Android client.
        </p>
      </div>
    </div>
  );
}
