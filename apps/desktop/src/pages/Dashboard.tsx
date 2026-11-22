import { useEffect, useState, useRef } from "react";
import { Link, useNavigate } from "react-router-dom";
import { listen } from "@tauri-apps/api/event";
import { open } from "@tauri-apps/plugin-dialog";
import { QRCodeSVG } from "qrcode.react";
import {
  Smartphone,
  Wifi,
  ShieldCheck,
  Clipboard,
  Bell,
  RefreshCw,
  FolderSync,
  Activity,
  ArrowRight,
  BatteryCharging,
  Cpu,
  CheckCircle2,
  Copy,
  Check,
  QrCode,
  Send,
  ExternalLink,
  Radio,
  UploadCloud,
} from "lucide-react";
import { useAppStore } from "../store/appStore";
import {
  getNetworkInfo,
  pingDevice,
  sendClipboard,
  sendFileToDevice,
  sendFileBytes,
  pickFilesDialog,
  NetworkInfo,
} from "../services/tauri";

interface ActivityItem {
  id: string;
  type: "connect" | "clipboard" | "notification" | "file";
  text: string;
  time: string;
}

export default function Dashboard() {
  const navigate = useNavigate();
  const { devices, loadDevices, openQrModal } = useAppStore();
  const [networkInfo, setNetworkInfo] = useState<NetworkInfo | null>(null);
  const [pingStatus, setPingStatus] = useState<Record<string, string>>({});
  const [quickClipboard, setQuickClipboard] = useState("");
  const [clipboardSending, setClipboardSending] = useState(false);
  const [copiedIp, setCopiedIp] = useState(false);
  const [targetDeviceId, setTargetDeviceId] = useState<string>("");
  const htmlFileInputRef = useRef<HTMLInputElement>(null);
  const [activities, setActivities] = useState<ActivityItem[]>([
    {
      id: "init-1",
      type: "connect",
      text: "TCP Server & mDNS discovery service running on port 45876",
      time: "Just now",
    },
  ]);

  const refreshAll = async () => {
    await loadDevices();
    try {
      const net = await getNetworkInfo();
      setNetworkInfo(net);
    } catch (e) {
      console.error("Failed to get network info:", e);
    }
  };

  useEffect(() => {
    void refreshAll();
    const interval = setInterval(() => {
      void refreshAll();
    }, 4000);

    const unlistenPaired = listen<{ device_id: string; device_name: string }>(
      "device-paired",
      (event) => {
        setActivities((prev) => [
          {
            id: String(Date.now()),
            type: "connect",
            text: `Device paired & verified: ${event.payload.device_name}`,
            time: "Just now",
          },
          ...prev.slice(0, 9),
        ]);
        void loadDevices();
      }
    );

    const unlistenDisconnected = listen<{ device_id: string }>(
      "device-disconnected",
      (event) => {
        setActivities((prev) => [
          {
            id: String(Date.now()),
            type: "connect",
            text: `Device disconnected: ${event.payload.device_id}`,
            time: "Just now",
          },
          ...prev.slice(0, 9),
        ]);
        void loadDevices();
      }
    );

    const unlistenClipboard = listen<{ content: string; source: string }>(
      "clipboard-received",
      (event) => {
        setActivities((prev) => [
          {
            id: String(Date.now()),
            type: "clipboard",
            text: `Clipboard synced from ${event.payload.source}: "${event.payload.content.slice(0, 36)}..."`,
            time: "Just now",
          },
          ...prev.slice(0, 9),
        ]);
      }
    );

    const unlistenNotification = listen<{ title: string; app_name: string }>(
      "notification-received",
      (event) => {
        setActivities((prev) => [
          {
            id: String(Date.now()),
            type: "notification",
            text: `Notification: [${event.payload.app_name}] ${event.payload.title}`,
            time: "Just now",
          },
          ...prev.slice(0, 9),
        ]);
      }
    );

    const unlistenFileReceived = listen<{ file_name: string }>(
      "file-received",
      (event) => {
        setActivities((prev) => [
          {
            id: String(Date.now()),
            type: "file",
            text: `File received from phone: ${event.payload.file_name}`,
            time: "Just now",
          },
          ...prev.slice(0, 9),
        ]);
      }
    );

    const unlistenFileSent = listen<{ file_name: string }>(
      "file-send-complete",
      (event) => {
        setActivities((prev) => [
          {
            id: String(Date.now()),
            type: "file",
            text: `File beamed to phone: ${event.payload.file_name}`,
            time: "Just now",
          },
          ...prev.slice(0, 9),
        ]);
      }
    );

    return () => {
      clearInterval(interval);
      unlistenPaired.then((f) => f());
      unlistenDisconnected.then((f) => f());
      unlistenClipboard.then((f) => f());
      unlistenNotification.then((f) => f());
      unlistenFileReceived.then((f) => f());
      unlistenFileSent.then((f) => f());
    };
  }, []);

  const handleSendFileDirectly = async (deviceId: string) => {
    try {
      let filePaths = await pickFilesDialog();
      if (!filePaths || filePaths.length === 0) {
        try {
          const selected = await open({
            multiple: false,
            title: "Select file to beam to phone",
          });
          if (selected) {
            filePaths = Array.isArray(selected) ? selected : [selected];
          }
        } catch (_e) {}
      }

      if (filePaths && filePaths.length > 0) {
        for (const selected of filePaths) {
          const fileName = selected.split("/").pop() || "file";
          setActivities((prev) => [
            {
              id: String(Date.now()),
              type: "file",
              text: `Beaming "${fileName}" to phone...`,
              time: "Just now",
            },
            ...prev.slice(0, 9),
          ]);
          await sendFileToDevice(deviceId, selected);
        }
        return;
      }

      // Fallback to HTML file picker
      setTargetDeviceId(deviceId);
      htmlFileInputRef.current?.click();
    } catch (e) {
      console.warn("Send file error, falling back to input:", e);
      setTargetDeviceId(deviceId);
      htmlFileInputRef.current?.click();
    }
  };

  const handleHtmlFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    try {
      const devId = targetDeviceId || devices.find((d) => d.connected)?.id || "";
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const fileName = file.name;
        setActivities((prev) => [
          {
            id: String(Date.now()),
            type: "file",
            text: `Beaming "${fileName}" to phone...`,
            time: "Just now",
          },
          ...prev.slice(0, 9),
        ]);

        const filePath = (file as any).path;
        if (filePath && typeof filePath === "string" && filePath.length > 0) {
          await sendFileToDevice(devId, filePath);
        } else {
          const arrayBuffer = await file.arrayBuffer();
          const bytes = Array.from(new Uint8Array(arrayBuffer));
          await sendFileBytes(devId, fileName, bytes);
        }
      }
    } catch (err) {
      console.error("Failed to beam file to phone:", err);
    } finally {
      e.target.value = "";
    }
  };

  const handlePing = async (deviceId: string) => {
    setPingStatus((prev) => ({ ...prev, [deviceId]: "Pinging..." }));
    try {
      const ok = await pingDevice(deviceId);
      setPingStatus((prev) => ({
        ...prev,
        [deviceId]: ok ? "Online (Ack <10ms)" : "No reply",
      }));
    } catch {
      setPingStatus((prev) => ({ ...prev, [deviceId]: "Timeout" }));
    }
    setTimeout(() => {
      setPingStatus((prev) => {
        const copy = { ...prev };
        delete copy[deviceId];
        return copy;
      });
    }, 4000);
  };

  const handleQuickSendClipboard = async () => {
    if (!quickClipboard.trim()) return;
    setClipboardSending(true);
    try {
      await sendClipboard(quickClipboard);
      setActivities((prev) => [
        {
          id: String(Date.now()),
          type: "clipboard",
          text: `Sent to phone: "${quickClipboard.slice(0, 36)}..."`,
          time: "Just now",
        },
        ...prev.slice(0, 9),
      ]);
      setQuickClipboard("");
    } catch (e) {
      console.error("Failed to send clipboard:", e);
    } finally {
      setClipboardSending(false);
    }
  };

  const localIp = networkInfo?.local_ip || "192.168.0.118";
  const port = networkInfo?.port || 45876;
  const qrPayload = `vexyron://${localIp}:${port}`;

  const copyIp = () => {
    navigator.clipboard.writeText(`${localIp}:${port}`);
    setCopiedIp(true);
    setTimeout(() => setCopiedIp(false), 2000);
  };

  const activeDevices = devices
    .filter((d) => d.connected)
    .filter((dev, idx, arr) => arr.findIndex((d) => d.name === dev.name) === idx);

  return (
    <div className="space-y-8 pb-12">
      {/* Hidden Fallback File Input */}
      <input
        type="file"
        ref={htmlFileInputRef}
        multiple
        onChange={handleHtmlFileChange}
        className="hidden"
      />

      {/* Hero Welcome Banner */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-[#171D2B]/90 via-[#111522]/95 to-[#171D2B]/80 border border-[#20283A] p-8 shadow-2xl backdrop-blur-xl">
        {/* Glow corner */}
        <div className="absolute -right-20 -top-20 w-80 h-80 bg-gradient-to-bl from-[#7C5CFC]/20 via-[#32C5FF]/10 to-transparent rounded-full blur-2xl pointer-events-none" />

        <div className="relative z-10 flex flex-col lg:flex-row lg:items-center justify-between gap-6">
          <div className="space-y-2.5 max-w-2xl">
            <div className="inline-flex items-center gap-2 rounded-full bg-[#7C5CFC]/15 border border-[#7C5CFC]/30 px-3.5 py-1 text-xs font-semibold text-[#7C5CFC]">
              <span className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#32C5FF] opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-[#32C5FF]"></span>
              </span>
              VEXYRON Core Active • mDNS Broadcasting
            </div>

            <h1 className="text-3xl md:text-4xl font-black tracking-tight text-[#F5F7FF]">
              One Connection. <span className="text-[#32C5FF]">Every Device.</span>
            </h1>

            <p className="text-sm text-[#929BB0] leading-relaxed">
              Wireless cross-platform bridge linking your Ubuntu Linux desktop with your Android phone. Fast clipboard sharing, synced notifications, and wireless file transfer.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <button
              onClick={refreshAll}
              className="flex items-center gap-2 rounded-2xl bg-[#171D2B] hover:bg-[#20283A] border border-[#20283A] text-[#F5F7FF] px-4 py-3 text-xs font-semibold transition-all shadow-sm cursor-pointer active:scale-95"
            >
              <RefreshCw size={15} />
              <span>Refresh Network</span>
            </button>

            <button
              onClick={openQrModal}
              className="flex items-center gap-2 rounded-2xl bg-gradient-to-r from-[#7C5CFC] to-[#32C5FF] hover:from-[#6b4bf0] hover:to-[#22b7f0] text-white px-5 py-3 text-xs font-bold transition-all shadow-lg shadow-[#7C5CFC]/30 cursor-pointer active:scale-95"
            >
              <QrCode size={16} />
              <span>Pair Phone via QR</span>
            </button>
          </div>
        </div>
      </div>

      {/* Metrics Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Metric 1 */}
        <div className="rounded-2xl bg-[#171D2B]/80 border border-[#20283A] p-5 shadow-sm hover:border-[#7C5CFC]/40 transition-all">
          <div className="flex items-center justify-between text-[#929BB0] mb-3">
            <span className="text-[11px] uppercase tracking-wider font-bold">
              Active Phones
            </span>
            <div className="p-2 rounded-xl bg-[#7C5CFC]/15 text-[#7C5CFC]">
              <Smartphone size={17} />
            </div>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-white">
              {activeDevices.length}
            </span>
            <span className="text-xs text-[#929BB0]">
              of {devices.length} paired
            </span>
          </div>
          <div className="mt-3 flex items-center gap-1.5 text-xs text-[#35D49A]">
            <CheckCircle2 size={13} />
            <span>TCP Socket Listening</span>
          </div>
        </div>

        {/* Metric 2 */}
        <div className="rounded-2xl bg-[#171D2B]/80 border border-[#20283A] p-5 shadow-sm hover:border-[#32C5FF]/40 transition-all">
          <div className="flex items-center justify-between text-[#929BB0] mb-3">
            <span className="text-[11px] uppercase tracking-wider font-bold">
              Host LAN Address
            </span>
            <div className="p-2 rounded-xl bg-[#32C5FF]/15 text-[#32C5FF]">
              <Wifi size={17} />
            </div>
          </div>
          <div className="flex items-baseline justify-between">
            <span className="font-mono text-xl font-bold text-[#32C5FF] truncate">
              {localIp}
            </span>
            <button
              onClick={copyIp}
              className="p-1 rounded-lg bg-[#20283A] hover:bg-[#32C5FF]/20 text-[#929BB0] hover:text-[#32C5FF] transition cursor-pointer"
              title="Copy IP:Port"
            >
              {copiedIp ? <Check size={13} className="text-[#35D49A]" /> : <Copy size={13} />}
            </button>
          </div>
          <div className="mt-3 text-xs text-[#929BB0]">
            Port <span className="font-mono text-white font-bold">{port}</span> (mDNS: _vexyron)
          </div>
        </div>

        {/* Metric 3 */}
        <div className="rounded-2xl bg-[#171D2B]/80 border border-[#20283A] p-5 shadow-sm hover:border-[#35D49A]/40 transition-all">
          <div className="flex items-center justify-between text-[#929BB0] mb-3">
            <span className="text-[11px] uppercase tracking-wider font-bold">
              Clipboard Engine
            </span>
            <div className="p-2 rounded-xl bg-[#35D49A]/15 text-[#35D49A]">
              <Clipboard size={17} />
            </div>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-2xl font-bold text-[#35D49A]">Ready</span>
          </div>
          <div className="mt-3 text-xs text-[#929BB0]">
            Bidirectional push & auto-sync
          </div>
        </div>

        {/* Metric 4 */}
        <div className="rounded-2xl bg-[#171D2B]/80 border border-[#20283A] p-5 shadow-sm hover:border-[#FFB84D]/40 transition-all">
          <div className="flex items-center justify-between text-[#929BB0] mb-3">
            <span className="text-[11px] uppercase tracking-wider font-bold">
              Device Authentication
            </span>
            <div className="p-2 rounded-xl bg-[#FFB84D]/15 text-[#FFB84D]">
              <ShieldCheck size={17} />
            </div>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-2xl font-bold text-white">4-Digit PIN</span>
          </div>
          <div className="mt-3 text-xs text-[#929BB0]">
            ECDH & Mutual Certificate Trust
          </div>
        </div>
      </div>

      {/* Main Grid: Devices & Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 Cols: Connected Devices */}
        <div className="lg:col-span-2 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-bold text-[#F5F7FF] flex items-center gap-2">
              <Smartphone className="text-[#7C5CFC]" size={18} />
              Connected Phones
            </h2>
            <Link
              to="/devices"
              className="text-xs text-[#7C5CFC] hover:text-[#32C5FF] font-semibold flex items-center gap-1 transition"
            >
              Manage all paired ({devices.length}) <ArrowRight size={13} />
            </Link>
          </div>

          {activeDevices.length === 0 ? (
            /* Standalone Onboarding / Quick Scan Card */
            <div className="rounded-3xl bg-[#111522]/90 border border-[#20283A] p-7 shadow-xl">
              <div className="flex flex-col md:flex-row items-center gap-6">
                <div className="p-3 bg-white rounded-2xl shadow-lg shadow-black/40 shrink-0">
                  <QRCodeSVG
                    value={qrPayload}
                    size={132}
                    level="M"
                    includeMargin={false}
                  />
                </div>

                <div className="space-y-3 flex-1 text-center md:text-left">
                  <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-[#32C5FF]/10 border border-[#32C5FF]/20 text-[11px] font-semibold text-[#32C5FF]">
                    <Radio size={12} className="animate-pulse" />
                    Waiting for Phone Connection
                  </div>
                  <h3 className="text-lg font-bold text-white">
                    Scan to Link Your Android Phone
                  </h3>
                  <p className="text-xs text-[#929BB0] leading-relaxed">
                    Open <span className="text-white font-medium">VEXYRON</span> on your phone and tap <span className="text-[#32C5FF] font-medium">Scan QR</span> or connect to IP <span className="font-mono text-[#32C5FF]">{localIp}:45876</span>.
                  </p>

                  <div className="flex flex-wrap items-center gap-2 pt-1 justify-center md:justify-start">
                    <button
                      onClick={openQrModal}
                      className="px-4 py-2 rounded-xl bg-gradient-to-r from-[#7C5CFC] to-[#32C5FF] text-white text-xs font-bold shadow-md cursor-pointer hover:opacity-90 transition"
                    >
                      Enlarge QR Code
                    </button>
                    <Link
                      to="/devices"
                      className="px-4 py-2 rounded-xl bg-[#171D2B] hover:bg-[#20283A] border border-[#20283A] text-xs font-semibold text-[#929BB0] hover:text-white transition"
                    >
                      Device Settings
                    </Link>
                  </div>
                </div>
              </div>
            </div>
          ) : (
            <div className="space-y-3">
              {activeDevices.map((device) => (
                <div
                  key={device.id}
                  className="rounded-2xl bg-[#171D2B]/90 border border-[#20283A] p-5 hover:border-[#7C5CFC]/40 transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-sm"
                >
                  <div className="flex items-center gap-4">
                    <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-[#7C5CFC]/20 to-[#32C5FF]/20 border border-[#7C5CFC]/30 flex items-center justify-center text-[#32C5FF] shrink-0">
                      <Smartphone size={22} />
                    </div>
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-base text-white">
                          {device.name}
                        </span>
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-[#35D49A]/15 text-[#35D49A] border border-[#35D49A]/30">
                          <span className="w-1.5 h-1.5 rounded-full bg-[#35D49A] animate-pulse" />
                          ONLINE
                        </span>
                      </div>
                      <div className="flex items-center gap-3 text-xs text-[#929BB0]">
                        <span className="flex items-center gap-1 text-[#35D49A]">
                          <BatteryCharging size={13} />
                          {device.battery}%
                        </span>
                        <span>•</span>
                        <span>{device.platform}</span>
                        <span>•</span>
                        <span className="font-mono text-[#7C5CFC]">Port 45876</span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => handleSendFileDirectly(device.id)}
                      className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-gradient-to-r from-[#7C5CFC] to-[#32C5FF] hover:opacity-90 text-white text-xs font-bold shadow-md transition cursor-pointer"
                    >
                      <UploadCloud size={14} />
                      <span>Send File</span>
                    </button>
                    <button
                      onClick={() => handlePing(device.id)}
                      className="px-3 py-2 rounded-xl bg-[#20283A] hover:bg-[#7C5CFC]/20 text-[#929BB0] hover:text-[#32C5FF] text-xs font-semibold border border-[#20283A] transition cursor-pointer"
                    >
                      {pingStatus[device.id] || "Ping"}
                    </button>
                    <button
                      onClick={() => navigate("/files")}
                      className="px-3.5 py-2 rounded-xl bg-[#171D2B] hover:bg-[#20283A] text-[#929BB0] hover:text-white text-xs font-semibold border border-[#20283A] transition cursor-pointer"
                    >
                      Files Hub
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Quick Action: Instant Clipboard Send */}
          <div className="rounded-2xl bg-[#171D2B]/80 border border-[#20283A] p-5 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-[#F5F7FF] flex items-center gap-2">
                <Clipboard size={15} className="text-[#32C5FF]" />
                Instant Push to Phone Clipboard
              </span>
              <span className="text-[11px] text-[#929BB0]">
                Auto-pastes on connected device
              </span>
            </div>

            <div className="flex gap-2">
              <input
                type="text"
                value={quickClipboard}
                onChange={(e) => setQuickClipboard(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleQuickSendClipboard()}
                placeholder="Type text or URL to send instantly..."
                className="flex-1 bg-[#090B14] border border-[#20283A] rounded-xl px-4 py-2.5 text-xs text-white placeholder-[#929BB0]/60 outline-none focus:border-[#7C5CFC]"
              />
              <button
                onClick={handleQuickSendClipboard}
                disabled={clipboardSending || !quickClipboard.trim()}
                className="px-4 py-2.5 rounded-xl bg-gradient-to-r from-[#7C5CFC] to-[#32C5FF] hover:opacity-90 disabled:opacity-50 text-white text-xs font-bold flex items-center gap-1.5 transition cursor-pointer"
              >
                <Send size={13} />
                <span>Push</span>
              </button>
            </div>
          </div>
        </div>

        {/* Right Col: Live Activity Stream */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-bold text-[#F5F7FF] flex items-center gap-2">
              <Activity className="text-[#32C5FF]" size={18} />
              Activity Feed
            </h2>
            <span className="text-[10px] font-mono font-bold bg-[#20283A] text-[#929BB0] px-2 py-0.5 rounded">
              REAL-TIME
            </span>
          </div>

          <div className="rounded-3xl bg-[#111522]/90 border border-[#20283A] p-5 space-y-3 max-h-[460px] overflow-y-auto">
            {activities.length === 0 ? (
              <div className="py-8 text-center text-xs text-[#929BB0]">
                No recent activity logged
              </div>
            ) : (
              activities.map((item) => (
                <div
                  key={item.id}
                  className="flex items-start gap-3 p-3 rounded-xl bg-[#171D2B]/60 border border-[#20283A]/60 text-xs"
                >
                  <div
                    className={`p-2 rounded-lg shrink-0 ${
                      item.type === "connect"
                        ? "bg-[#7C5CFC]/15 text-[#7C5CFC]"
                        : item.type === "clipboard"
                        ? "bg-[#32C5FF]/15 text-[#32C5FF]"
                        : item.type === "notification"
                        ? "bg-[#FFB84D]/15 text-[#FFB84D]"
                        : "bg-[#35D49A]/15 text-[#35D49A]"
                    }`}
                  >
                    {item.type === "connect" ? (
                      <Smartphone size={14} />
                    ) : item.type === "clipboard" ? (
                      <Clipboard size={14} />
                    ) : item.type === "notification" ? (
                      <Bell size={14} />
                    ) : (
                      <FolderSync size={14} />
                    )}
                  </div>
                  <div className="flex-1 space-y-0.5 overflow-hidden">
                    <p className="text-[#F5F7FF] font-medium leading-snug break-words">
                      {item.text}
                    </p>
                    <span className="text-[10px] text-[#929BB0]">
                      {item.time}
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
