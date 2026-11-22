import { useEffect, useState } from "react";
import { useAppStore } from "../store/appStore";
import { useNavigate } from "react-router-dom";
import { QRCodeSVG } from "qrcode.react";
import { listen } from "@tauri-apps/api/event";
import {
  Smartphone,
  Wifi,
  Trash2,
  RefreshCw,
  ShieldCheck,
  BatteryCharging,
  QrCode,
  Copy,
  Check,
  Radio,
  FolderOpen,
  ClipboardList,
  ChevronDown,
  ChevronUp,
} from "lucide-react";
import {
  getPairedDevices,
  unpairDevice,
  pingDevice,
  getNetworkInfo,
  PairedDevice,
  NetworkInfo,
} from "../services/tauri";
import { LinuxFirewallHelper } from "../components/LinuxFirewallHelper";

export default function Devices() {
  const navigate = useNavigate();
  const { devices, loadDevices, openQrModal } = useAppStore();
  const [pairedDevices, setPairedDevices] = useState<PairedDevice[]>([]);
  const [networkInfo, setNetworkInfo] = useState<NetworkInfo | null>(null);
  const [loading, setLoading] = useState(false);
  const [pingMap, setPingMap] = useState<Record<string, string>>({});
  const [removingId, setRemovingId] = useState<string | null>(null);
  const [copiedUfw, setCopiedUfw] = useState(false);
  const [copiedIp, setCopiedIp] = useState(false);
  const [showTrustedRegistry, setShowTrustedRegistry] = useState(false);

  const fetchAll = async () => {
    setLoading(true);
    try {
      const list = await getPairedDevices();
      // Deduplicate paired devices by device name (keep newest paired_at)
      const dedupList: PairedDevice[] = [];
      for (const d of list) {
        const existingIdx = dedupList.findIndex((item) => item.name === d.name);
        if (existingIdx !== -1) {
          if (d.paired_at > dedupList[existingIdx].paired_at) {
            dedupList[existingIdx] = d;
          }
        } else {
          dedupList.push(d);
        }
      }
      setPairedDevices(dedupList);
      await loadDevices();
      const net = await getNetworkInfo();
      setNetworkInfo(net);
    } catch (e) {
      console.error("Failed to load devices data:", e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void fetchAll();

    // Listen for live connection changes
    const unlistenUpdated = listen("devices-updated", () => void fetchAll());
    const unlistenConnected = listen("device-connected", () => void fetchAll());
    const unlistenDisconnected = listen("device-disconnected", () => void fetchAll());
    const unlistenPaired = listen("device-paired", () => void fetchAll());

    return () => {
      unlistenUpdated.then((f) => f());
      unlistenConnected.then((f) => f());
      unlistenDisconnected.then((f) => f());
      unlistenPaired.then((f) => f());
    };
  }, []);

  const handleUnpair = async (id: string, name: string) => {
    if (
      !confirm(
        `Are you sure you want to remove and unpair "${name}"? You will need to pair it again to connect.`
      )
    ) {
      return;
    }
    setRemovingId(id);
    try {
      await unpairDevice(id);
      await fetchAll();
    } catch (e) {
      console.error("Failed to unpair device:", e);
    } finally {
      setRemovingId(null);
    }
  };

  const handlePing = async (id: string) => {
    setPingMap((prev) => ({ ...prev, [id]: "Testing connection..." }));
    try {
      const ok = await pingDevice(id);
      setPingMap((prev) => ({
        ...prev,
        [id]: ok ? "Online (Ack <15ms)" : "No reply",
      }));
    } catch {
      setPingMap((prev) => ({ ...prev, [id]: "Ping failed" }));
    }
    setTimeout(() => {
      setPingMap((prev) => {
        const copy = { ...prev };
        delete copy[id];
        return copy;
      });
    }, 4000);
  };

  const localIp = networkInfo?.local_ip || "192.168.0.118";
  const port = networkInfo?.port || 45876;
  const qrPayload = `vexyron://${localIp}:${port}`;
  const ufwCommand = "sudo ufw allow 45876/tcp && sudo ufw allow 5353/udp";

  const copyUfw = () => {
    navigator.clipboard.writeText(ufwCommand);
    setCopiedUfw(true);
    setTimeout(() => setCopiedUfw(false), 2000);
  };

  const copyIp = () => {
    navigator.clipboard.writeText(`${localIp}:${port}`);
    setCopiedIp(true);
    setTimeout(() => setCopiedIp(false), 2000);
  };

  // Filter only devices that are CONNECTED NOW and deduplicate by name
  const connectedDevices = devices
    .filter((d) => d.connected)
    .filter((dev, idx, arr) => arr.findIndex((d) => d.name === dev.name) === idx);

  return (
    <div className="space-y-8 pb-12">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 rounded-full bg-[#35D49A]/15 border border-[#35D49A]/30 px-3 py-1 text-xs font-semibold text-[#35D49A] mb-2">
            <Radio size={14} className="animate-pulse" />
            Active Connected Devices
          </div>
          <h1 className="text-3xl font-extrabold text-white">Connected Devices</h1>
          <p className="text-sm text-[#929BB0]">
            Live view of actively connected smartphones. When devices disconnect, they are automatically removed.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={fetchAll}
            disabled={loading}
            className="flex items-center gap-2 rounded-xl bg-[#171D2B] hover:bg-[#20283A] border border-[#20283A] text-zinc-300 px-4 py-2.5 text-xs font-semibold transition cursor-pointer"
          >
            <RefreshCw size={14} className={loading ? "animate-spin text-[#32C5FF]" : ""} />
            <span>Refresh</span>
          </button>

          <button
            onClick={openQrModal}
            className="flex items-center gap-2 rounded-xl bg-gradient-to-r from-[#7C5CFC] to-[#32C5FF] hover:from-[#6b4bf0] hover:to-[#22b7f0] text-white px-4 py-2.5 text-xs font-bold transition shadow-md shadow-[#7C5CFC]/25 cursor-pointer"
          >
            <QrCode size={15} />
            <span>Pair New Device</span>
          </button>
        </div>
      </div>

      {/* Quick Pair & Troubleshooting Banner */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* QR Code Quick Box */}
        <div className="rounded-3xl bg-[#111522]/90 border border-[#20283A] p-6 shadow-xl flex flex-col sm:flex-row items-center gap-6 lg:col-span-2">
          <div className="p-3 bg-white rounded-2xl shadow-lg shadow-black/40 shrink-0">
            <QRCodeSVG
              value={qrPayload}
              size={130}
              level="M"
              includeMargin={false}
            />
          </div>

          <div className="space-y-3 flex-1 text-center sm:text-left">
            <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-[#35D49A]/10 border border-[#35D49A]/20 text-[11px] font-semibold text-[#35D49A]">
              <Radio size={12} className="animate-pulse" />
              mDNS Discovery & TCP Server Active
            </div>
            <h3 className="text-base font-bold text-white">
              Instant Phone Pairing
            </h3>
            <p className="text-xs text-[#929BB0] leading-relaxed">
              Open VEXYRON on Android &rarr; Tap <span className="text-white font-semibold">Scan QR</span> or connect to <span className="font-mono text-[#32C5FF] font-semibold">{localIp}:{port}</span>.
            </p>

            <div className="flex flex-wrap items-center gap-2 justify-center sm:justify-start">
              <button
                onClick={copyIp}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-[#171D2B] hover:bg-[#20283A] border border-[#20283A] text-xs font-mono text-[#32C5FF] transition cursor-pointer"
              >
                {copiedIp ? <Check size={13} className="text-[#35D49A]" /> : <Copy size={13} />}
                <span>{localIp}:{port}</span>
              </button>
            </div>
          </div>
        </div>

        {/* Linux Firewall Card (All Distros) */}
        <div className="flex flex-col justify-between">
          <LinuxFirewallHelper />
        </div>
      </div>

      {/* Live Connected Devices List */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-white flex items-center gap-2">
            <Smartphone size={18} className="text-[#35D49A]" />
            <span>Connected Devices Now ({connectedDevices.length})</span>
          </h2>
          <span className="text-xs text-[#929BB0]">
            Only actively connected devices are shown
          </span>
        </div>

        {connectedDevices.length === 0 ? (
          <div className="rounded-3xl bg-[#111522]/80 border border-[#20283A] p-10 text-center space-y-4">
            <div className="w-16 h-16 mx-auto rounded-2xl bg-[#20283A] border border-[#20283A] flex items-center justify-center text-[#929BB0]">
              <Radio size={28} className="text-[#929BB0]" />
            </div>
            <div className="space-y-1">
              <h3 className="text-base font-bold text-white">
                No Devices Connected Right Now
              </h3>
              <p className="text-xs text-[#929BB0] max-w-md mx-auto leading-relaxed">
                Open VEXYRON on your phone to connect. Once connected, your phone will appear here in real-time. When it disconnects, it will automatically disappear.
              </p>
            </div>
            <button
              onClick={openQrModal}
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-[#7C5CFC] to-[#32C5FF] text-white text-xs font-bold shadow-md cursor-pointer hover:opacity-95 transition"
            >
              <QrCode size={15} />
              <span>Show Pairing QR Code</span>
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {connectedDevices.map((device) => {
              const pairedEntry = pairedDevices.find((p) => p.name === device.name || p.id === device.id);
              const unpairId = pairedEntry?.id || device.id;

              return (
                <div
                  key={device.id}
                  className="rounded-2xl bg-[#171D2B]/95 border border-[#35D49A]/30 p-5 hover:border-[#35D49A]/60 transition-all space-y-4 shadow-lg shadow-[#35D49A]/5"
                >
                  <div className="flex items-start justify-between gap-4">
                    <div className="flex items-center gap-3.5">
                      <div className="w-12 h-12 rounded-2xl flex items-center justify-center shrink-0 border bg-[#35D49A]/15 text-[#35D49A] border-[#35D49A]/30">
                        <Smartphone size={22} />
                      </div>
                      <div className="space-y-0.5">
                        <h3 className="font-bold text-white text-base">
                          {device.name}
                        </h3>
                        <div className="flex items-center gap-2 text-xs">
                          <span className="inline-flex items-center gap-1 font-semibold text-[#35D49A]">
                            <span className="w-2 h-2 rounded-full bg-[#35D49A] animate-pulse" />
                            Connected Now
                          </span>
                          <span className="text-[#929BB0]">•</span>
                          <span className="text-[#929BB0] text-[11px] font-mono flex items-center gap-1">
                            <Wifi size={11} className="text-[#32C5FF]" />
                            {device.ip_address || "Wi-Fi"}
                          </span>
                        </div>
                      </div>
                    </div>

                    <button
                      onClick={() => handleUnpair(unpairId, device.name)}
                      disabled={removingId === unpairId}
                      className="p-2 rounded-xl text-[#929BB0] hover:text-[#FF5D73] hover:bg-[#FF5D73]/10 transition cursor-pointer"
                      title="Unpair Device"
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>

                  {/* Device Metadata */}
                  <div className="p-3 rounded-xl bg-[#090B14]/80 border border-[#20283A] text-xs text-[#929BB0] flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span className="text-zinc-400">Connection:</span>
                      <span className="text-zinc-200 font-semibold">{device.connection_type || "Wi-Fi TCP"}</span>
                    </div>
                    <div className="flex items-center gap-1.5 text-[#35D49A] font-semibold">
                      <BatteryCharging size={14} />
                      <span>{device.battery}%</span>
                    </div>
                  </div>

                  {/* Action buttons */}
                  <div className="flex items-center gap-2 pt-1">
                    <button
                      onClick={() => handlePing(device.id)}
                      className="flex-1 py-2 rounded-xl bg-[#20283A] hover:bg-[#7C5CFC]/20 text-[#929BB0] hover:text-[#32C5FF] text-xs font-semibold border border-[#20283A] transition cursor-pointer"
                    >
                      {pingMap[device.id] || "Ping Device"}
                    </button>

                    <button
                      onClick={() => navigate("/files")}
                      className="flex-1 py-2 rounded-xl bg-[#7C5CFC]/20 hover:bg-[#7C5CFC]/30 text-[#7C5CFC] text-xs font-bold border border-[#7C5CFC]/40 transition cursor-pointer flex items-center justify-center gap-1.5"
                    >
                      <FolderOpen size={13} />
                      <span>Browse Files</span>
                    </button>

                    <button
                      onClick={() => navigate("/clipboard")}
                      className="px-3 py-2 rounded-xl bg-[#32C5FF]/15 hover:bg-[#32C5FF]/25 text-[#32C5FF] text-xs font-bold border border-[#32C5FF]/30 transition cursor-pointer flex items-center justify-center gap-1"
                      title="Clipboard Sync"
                    >
                      <ClipboardList size={13} />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Collapsible Trusted Pairing Registry */}
      <div className="pt-4 border-t border-[#20283A]">
        <button
          onClick={() => setShowTrustedRegistry(!showTrustedRegistry)}
          className="w-full flex items-center justify-between p-4 rounded-2xl bg-[#111522]/60 hover:bg-[#111522] border border-[#20283A] transition cursor-pointer"
        >
          <div className="flex items-center gap-2.5">
            <ShieldCheck size={16} className="text-[#7C5CFC]" />
            <span className="text-sm font-bold text-white">
              Trusted Pairing Registry ({pairedDevices.length})
            </span>
            <span className="text-xs text-[#929BB0]">
              — Deduplicated trusted certificates
            </span>
          </div>

          <div className="text-[#929BB0]">
            {showTrustedRegistry ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
          </div>
        </button>

        {showTrustedRegistry && (
          <div className="mt-4 p-5 rounded-2xl bg-[#111522]/90 border border-[#20283A] space-y-3">
            {pairedDevices.length === 0 ? (
              <p className="text-xs text-[#929BB0] py-2 text-center">
                No trusted pairing records saved.
              </p>
            ) : (
              <div className="divide-y divide-[#20283A]">
                {pairedDevices.map((p) => {
                  const isCurrentlyConnected = connectedDevices.some(
                    (d) => d.name === p.name || d.id === p.id
                  );

                  return (
                    <div
                      key={p.id}
                      className="flex items-center justify-between py-3 first:pt-0 last:pb-0"
                    >
                      <div className="flex items-center gap-3">
                        <div
                          className={`w-8 h-8 rounded-xl flex items-center justify-center text-xs ${
                            isCurrentlyConnected
                              ? "bg-[#35D49A]/20 text-[#35D49A]"
                              : "bg-[#20283A] text-[#929BB0]"
                          }`}
                        >
                          <Smartphone size={15} />
                        </div>
                        <div>
                          <p className="text-sm font-bold text-white">{p.name}</p>
                          <p className="text-[11px] text-[#929BB0] font-mono">
                            Paired {new Date(p.paired_at).toLocaleDateString()} •{" "}
                            <span
                              className={
                                isCurrentlyConnected
                                  ? "text-[#35D49A] font-semibold"
                                  : "text-[#929BB0]"
                              }
                            >
                              {isCurrentlyConnected ? "Connected" : "Offline"}
                            </span>
                          </p>
                        </div>
                      </div>

                      <button
                        onClick={() => handleUnpair(p.id, p.name)}
                        disabled={removingId === p.id}
                        className="px-3 py-1.5 rounded-lg text-xs font-semibold text-[#FF5D73] hover:bg-[#FF5D73]/10 transition flex items-center gap-1.5 cursor-pointer"
                      >
                        <Trash2 size={13} />
                        <span>Unpair</span>
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
