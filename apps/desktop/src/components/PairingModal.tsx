import { useEffect, useState } from "react";
import { listen } from "@tauri-apps/api/event";
import { QRCodeSVG } from "qrcode.react";
import {
  Smartphone,
  ShieldCheck,
  X,
  Check,
  Copy,
  Terminal,
  Wifi,
  Sparkles,
  Info,
  KeyRound,
} from "lucide-react";
import {
  confirmPairing,
  rejectPairing,
  getNetworkInfo,
  type PairingRequestEvent,
  type NetworkInfo,
} from "../services/tauri";
import { useAppStore } from "../store/appStore";

export default function PairingModal() {
  const { isQrModalOpen, closeQrModal } = useAppStore();
  const [request, setRequest] = useState<PairingRequestEvent | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [networkInfo, setNetworkInfo] = useState<NetworkInfo | null>(null);
  const [copiedCmd, setCopiedCmd] = useState(false);
  const [copiedIp, setCopiedIp] = useState(false);

  useEffect(() => {
    void getNetworkInfo().then(setNetworkInfo).catch(() => {});
  }, [isQrModalOpen]);

  useEffect(() => {
    let unlistenPairingRequested: (() => void) | undefined;
    let unlistenDevicePaired: (() => void) | undefined;

    listen<PairingRequestEvent>("pairing-requested", (event) => {
      console.log("Pairing requested:", event.payload);
      setRequest(event.payload);
    }).then((unlisten) => {
      unlistenPairingRequested = unlisten;
    });

    listen<{ device_id: string; device_name: string }>("device-paired", () => {
      setRequest(null);
      setIsProcessing(false);
      closeQrModal();
    }).then((unlisten) => {
      unlistenDevicePaired = unlisten;
    });

    return () => {
      if (unlistenPairingRequested) unlistenPairingRequested();
      if (unlistenDevicePaired) unlistenDevicePaired();
    };
  }, [closeQrModal]);

  const handleAccept = async () => {
    if (!request) return;
    setIsProcessing(true);
    try {
      await confirmPairing(request.device_id, request.pin);
      setRequest(null);
      closeQrModal();
    } catch (err) {
      console.error("Failed to accept pairing:", err);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleDecline = async () => {
    if (!request) return;
    setIsProcessing(true);
    try {
      await rejectPairing(request.device_id);
      setRequest(null);
    } catch (err) {
      console.error("Failed to decline pairing:", err);
    } finally {
      setIsProcessing(false);
    }
  };

  const localIp = networkInfo?.local_ip || "192.168.0.118";
  const port = networkInfo?.port || 45876;
  const qrPayload = `aetherlink://${localIp}:${port}`;
  const ufwCommand = "sudo ufw allow 45876/tcp && sudo ufw allow 5353/udp";

  const copyUfw = () => {
    navigator.clipboard.writeText(ufwCommand);
    setCopiedCmd(true);
    setTimeout(() => setCopiedCmd(false), 2000);
  };

  const copyIp = () => {
    navigator.clipboard.writeText(localIp);
    setCopiedIp(true);
    setTimeout(() => setCopiedIp(false), 2000);
  };

  // If there's an incoming PIN pairing request, show that directly
  if (request) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-md p-4 animate-in fade-in duration-200">
        <div className="w-full max-w-md rounded-3xl border border-[#7C5CFC]/40 bg-[#111522] p-7 shadow-2xl shadow-[#7C5CFC]/20 relative overflow-hidden">
          {/* Subtle top glow */}
          <div className="absolute top-0 left-1/2 -translate-x-1/2 w-48 h-1 bg-gradient-to-r from-transparent via-[#7C5CFC] to-transparent" />

          <div className="flex items-center space-x-3.5">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[#7C5CFC]/15 border border-[#7C5CFC]/30 text-[#7C5CFC]">
              <Smartphone className="h-6 w-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-lg font-bold text-[#F5F7FF]">
                  Device Pairing Request
                </h2>
                <span className="flex h-2 w-2 rounded-full bg-[#35D49A] animate-pulse" />
              </div>
              <p className="text-xs text-[#929BB0]">
                An Android phone wants to link with this desktop
              </p>
            </div>
          </div>

          <div className="mt-5 rounded-2xl border border-[#20283A] bg-[#171D2B]/90 p-5 space-y-4">
            <div>
              <div className="text-[11px] font-bold uppercase tracking-wider text-[#929BB0]">
                Device Name
              </div>
              <div className="mt-1 font-semibold text-white text-base">
                {request.device_name || "Android Smartphone"}
              </div>
            </div>

            <div>
              <div className="text-[11px] font-bold uppercase tracking-wider text-[#929BB0] flex items-center justify-between">
                <span>Security PIN</span>
                <span className="text-[10px] text-[#32C5FF] font-mono">ENCRYPTED</span>
              </div>
              <div className="mt-2 flex items-center justify-center rounded-xl border border-[#35D49A]/30 bg-[#35D49A]/10 py-3.5">
                <span className="font-mono text-3xl font-extrabold tracking-[0.3em] text-[#35D49A]">
                  {request.pin}
                </span>
              </div>
              <p className="mt-2 text-center text-xs text-[#929BB0]">
                Confirm that this 4-digit code matches the number on your smartphone
              </p>
            </div>
          </div>

          <div className="mt-6 flex space-x-3">
            <button
              onClick={handleDecline}
              disabled={isProcessing}
              className="flex flex-1 items-center justify-center space-x-2 rounded-xl border border-[#20283A] bg-[#171D2B] px-4 py-3 text-xs font-semibold text-[#929BB0] hover:text-white hover:bg-[#20283A] transition disabled:opacity-50 cursor-pointer"
            >
              <X className="h-4 w-4" />
              <span>Decline</span>
            </button>

            <button
              onClick={handleAccept}
              disabled={isProcessing}
              className="flex flex-1 items-center justify-center space-x-2 rounded-xl bg-gradient-to-r from-[#7C5CFC] to-[#32C5FF] hover:from-[#6b4bf0] hover:to-[#22b7f0] px-4 py-3 text-xs font-bold text-white shadow-lg shadow-[#7C5CFC]/30 transition disabled:opacity-50 cursor-pointer"
            >
              <ShieldCheck className="h-4 w-4" />
              <span>Accept & Trust</span>
            </button>
          </div>
        </div>
      </div>
    );
  }

  // If user clicked "Pair Phone" or QR modal is open
  if (isQrModalOpen) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-md p-4 animate-in fade-in duration-200">
        <div className="w-full max-w-lg rounded-3xl border border-[#20283A] bg-[#111522] p-7 shadow-2xl shadow-[#7C5CFC]/20 relative overflow-hidden">
          {/* Close button */}
          <button
            onClick={closeQrModal}
            className="absolute top-5 right-5 p-2 rounded-xl bg-[#171D2B] text-[#929BB0] hover:text-white hover:bg-[#20283A] transition cursor-pointer"
          >
            <X size={18} />
          </button>

          <div className="flex items-center gap-3 mb-5">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-tr from-[#7C5CFC]/20 to-[#32C5FF]/20 border border-[#7C5CFC]/30 text-[#32C5FF]">
              <Sparkles size={24} />
            </div>
            <div>
              <h2 className="text-lg font-bold text-[#F5F7FF]">
                Pair Android Smartphone
              </h2>
              <p className="text-xs text-[#929BB0]">
                Scan the QR code or connect directly via local network IP
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-5 items-center">
            {/* QR Code Container */}
            <div className="flex flex-col items-center justify-center p-5 rounded-2xl bg-[#090B14] border border-[#20283A] text-center">
              <div className="p-3 bg-white rounded-xl shadow-lg shadow-black/50">
                <QRCodeSVG
                  value={qrPayload}
                  size={148}
                  level="M"
                  includeMargin={false}
                />
              </div>
              <div className="mt-3 text-[11px] font-mono text-[#32C5FF] font-semibold flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-[#35D49A] animate-pulse" />
                Listening on :45876
              </div>
              <span className="text-[10px] text-[#929BB0] mt-0.5">
                Scan with VEXYRON Camera
              </span>
            </div>

            {/* Direct Connect & Instructions */}
            <div className="space-y-3.5 text-xs">
              <div className="space-y-1">
                <span className="text-[11px] font-bold text-[#929BB0] uppercase tracking-wider">
                  Option 1: Quick Scan
                </span>
                <p className="text-[#F5F7FF] leading-relaxed">
                  Open VEXYRON on your phone, navigate to <span className="text-[#32C5FF] font-semibold">Devices</span> &rarr; tap <span className="text-[#7C5CFC] font-semibold">Scan QR</span>.
                </p>
              </div>

              <div className="space-y-1.5">
                <span className="text-[11px] font-bold text-[#929BB0] uppercase tracking-wider">
                  Option 2: Direct IP Connect
                </span>
                <div className="p-2.5 rounded-xl bg-[#171D2B] border border-[#20283A] flex items-center justify-between">
                  <div>
                    <div className="text-[10px] text-[#929BB0]">Host Address</div>
                    <div className="font-mono text-xs font-bold text-[#32C5FF]">
                      {localIp}:{port}
                    </div>
                  </div>
                  <button
                    onClick={copyIp}
                    className="p-1.5 rounded-lg bg-[#20283A] hover:bg-[#7C5CFC]/20 text-[#929BB0] hover:text-[#32C5FF] transition cursor-pointer"
                    title="Copy IP"
                  >
                    {copiedIp ? <Check size={14} className="text-[#35D49A]" /> : <Copy size={14} />}
                  </button>
                </div>
              </div>

              <div className="text-[11px] text-[#929BB0]">
                Ensure your phone is connected to the same Wi-Fi network.
              </div>
            </div>
          </div>

          {/* Ubuntu Firewall Troubleshooter */}
          <div className="mt-5 p-3.5 rounded-2xl bg-[#171D2B]/80 border border-amber-500/20 text-xs space-y-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-1.5 font-semibold text-[#FFB84D] text-[11px]">
                <Terminal size={14} />
                Ubuntu Firewall (UFW) Notice
              </div>
              <button
                onClick={copyUfw}
                className="flex items-center gap-1 text-[10px] font-mono bg-[#20283A] hover:bg-amber-500/20 text-[#FFB84D] px-2 py-1 rounded-lg transition cursor-pointer"
              >
                {copiedCmd ? (
                  <>
                    <Check size={12} className="text-[#35D49A]" />
                    <span>Copied!</span>
                  </>
                ) : (
                  <>
                    <Copy size={12} />
                    <span>Copy Command</span>
                  </>
                )}
              </button>
            </div>
            <p className="text-[11px] text-[#929BB0] leading-snug">
              If pairing times out on physical phone, allow port 45876 in Ubuntu terminal:
            </p>
            <div className="p-2 rounded-lg bg-[#090B14] font-mono text-[10px] text-zinc-300 border border-[#20283A] select-all overflow-x-auto">
              sudo ufw allow 45876/tcp && sudo ufw allow 5353/udp
            </div>
          </div>
        </div>
      </div>
    );
  }

  return null;
}
