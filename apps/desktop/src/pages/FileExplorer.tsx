import { useEffect, useState, useRef } from "react";
import { open } from "@tauri-apps/plugin-dialog";
import { listen } from "@tauri-apps/api/event";
import {
  FolderSync,
  HardDrive,
  QrCode,
  AlertCircle,
  CheckCircle2,
  UploadCloud,
  FolderOpen,
  File,
  FileText,
  Image as ImageIcon,
  Video,
  Music,
  Archive,
  Trash2,
  ExternalLink,
  RefreshCw,
  Search,
  Send,
  Zap,
  ArrowDownLeft,
  ArrowUpRight,
  Sparkles,
  Smartphone,
  Eye,
} from "lucide-react";
import { useAppStore } from "../store/appStore";
import {
  getReceivedFiles,
  openDownloadsFolder,
  openFile,
  deleteReceivedFile,
  sendFileToDevice,
  sendFileBytes,
  pickFilesDialog,
  ReceivedFile,
  listPhoneFiles,
  downloadPhoneFile,
  uploadPhoneFile,
  FileSendProgressEvent,
  FileSendCompleteEvent,
  FileReceivedEvent,
} from "../services/tauri";
import type { PhoneFile } from "../types/file";
import FileToolbar from "../components/file/FileToolbar";
import Breadcrumb from "../components/file/Breadcrumb";
import FileList from "../components/file/FileList";

interface OutgoingTransfer {
  fileId: string;
  fileName: string;
  progress: number;
  sentBytes: number;
  totalBytes: number;
  status: string;
  isComplete: boolean;
  isError?: boolean;
}

export default function FileExplorer() {
  const { devices, selectedDeviceSerial, loadDevices, openQrModal } = useAppStore();
  const [activeTab, setActiveTab] = useState<"transfers" | "storage">("transfers");
  const htmlFileInputRef = useRef<HTMLInputElement>(null);

  // Wireless Received Files
  const [receivedFiles, setReceivedFiles] = useState<ReceivedFile[]>([]);
  const [loadingReceived, setLoadingReceived] = useState(false);
  const [fileFilter, setFileFilter] = useState<string>("all");
  const [searchQuery, setSearchQuery] = useState("");

  // Live Transfers
  const [activeTransfers, setActiveTransfers] = useState<OutgoingTransfer[]>([]);
  const [statusMessage, setStatusMessage] = useState<{ text: string; type: "success" | "error" | "info" } | null>(null);

  // Phone Internal Storage (legacy / ADB fallback tab)
  const [phonePath, setPhonePath] = useState("/sdcard");
  const [phoneFiles, setPhoneFiles] = useState<PhoneFile[]>([]);
  const [loadingPhoneFiles, setLoadingPhoneFiles] = useState(false);
  const [isUploadingToPhone, setIsUploadingToPhone] = useState(false);

  // Active Device (only currently connected devices)
  const activeDevice =
    devices.find((d) => d.id === selectedDeviceSerial && d.connected) ||
    devices.find((d) => d.connected) ||
    null;

  const fetchReceivedFiles = async () => {
    setLoadingReceived(true);
    try {
      const files = await getReceivedFiles();
      setReceivedFiles(files || []);
    } catch (err) {
      console.error("Failed to load received files:", err);
    } finally {
      setLoadingReceived(false);
    }
  };

  const fetchPhoneStorage = async () => {
    if (!activeDevice || !activeDevice.connected) return;
    setLoadingPhoneFiles(true);
    try {
      const result = await listPhoneFiles(activeDevice.id, phonePath);
      setPhoneFiles(result || []);
    } catch (err) {
      console.error("Failed to list phone files:", err);
      setPhoneFiles([]);
    } finally {
      setLoadingPhoneFiles(false);
    }
  };

  useEffect(() => {
    void loadDevices();
    void fetchReceivedFiles();

    // Listen for device pairing and connection events
    const unlistenPaired = listen("device-paired", () => void loadDevices());
    const unlistenUpdated = listen("devices-updated", () => void loadDevices());
    const unlistenDisconnected = listen("device-disconnected", () => void loadDevices());

    // Listen for file send progress from Desktop -> Phone
    const unlistenProgress = listen<FileSendProgressEvent>("file-send-progress", (event) => {
      const { file_id, file_name, progress, sent_bytes, total_bytes, status } = event.payload;
      setActiveTransfers((prev) => {
        const idx = prev.findIndex((t) => t.fileId === file_id);
        const item: OutgoingTransfer = {
          fileId: file_id,
          fileName: file_name,
          progress,
          sentBytes: sent_bytes,
          totalBytes: total_bytes,
          status,
          isComplete: progress >= 1.0,
        };
        if (idx >= 0) {
          const next = [...prev];
          next[idx] = item;
          return next;
        }
        return [item, ...prev];
      });
    });

    // Listen for file send completion
    const unlistenComplete = listen<FileSendCompleteEvent>("file-send-complete", (event) => {
      const { file_id, file_name } = event.payload;
      setActiveTransfers((prev) =>
        prev.map((t) =>
          t.fileId === file_id ? { ...t, isComplete: true, progress: 1.0, status: "Sent to Phone" } : t
        )
      );
      setStatusMessage({
        text: `"${file_name}" transferred successfully to your phone!`,
        type: "success",
      });
      setTimeout(() => setStatusMessage(null), 4000);
    });

    // Listen for incoming files sent from Phone -> Desktop
    const unlistenReceived = listen<FileReceivedEvent>("file-received", (event) => {
      const { file_name } = event.payload;
      void fetchReceivedFiles();
      setStatusMessage({
        text: `Received "${file_name}" from Phone! Saved to Downloads`,
        type: "success",
      });
      setTimeout(() => setStatusMessage(null), 5000);
    });

    // Listen for send errors
    const unlistenError = listen<{ file_name: string; error: string }>("file-send-error", (event) => {
      setStatusMessage({
        text: `Error sending "${event.payload.file_name}": ${event.payload.error}`,
        type: "error",
      });
      setTimeout(() => setStatusMessage(null), 5000);
    });

    return () => {
      unlistenPaired.then((f) => f());
      unlistenUpdated.then((f) => f());
      unlistenDisconnected.then((f) => f());
      unlistenProgress.then((f) => f());
      unlistenComplete.then((f) => f());
      unlistenReceived.then((f) => f());
      unlistenError.then((f) => f());
    };
  }, []);

  useEffect(() => {
    if (activeTab === "storage" && activeDevice?.connected) {
      void fetchPhoneStorage();
    }
  }, [activeTab, activeDevice?.id, phonePath]);

  // Handle Send File to Phone
  const handleSendFile = async () => {
    try {
      // 1. Try native backend dialog (zenity on Linux, rfd on other OSes)
      let filePaths = await pickFilesDialog();

      // 2. Fallback to open dialog plugin if needed
      if (!filePaths || filePaths.length === 0) {
        try {
          const selected = await open({
            multiple: true,
            title: `Select files to beam to phone`,
          });
          if (selected) {
            filePaths = Array.isArray(selected) ? selected : [selected];
          }
        } catch (openErr) {
          console.warn("open plugin fallback:", openErr);
        }
      }

      // If paths obtained from native picker, beam them!
      if (filePaths && filePaths.length > 0) {
        const targetId = activeDevice?.id || "";
        for (const path of filePaths) {
          const fileName = path.split("/").pop() || "File";
          setStatusMessage({ text: `Streaming "${fileName}" to phone...`, type: "info" });
          await sendFileToDevice(targetId, path);
        }
        return;
      }

      // 3. Fallback: Trigger HTML file input picker
      htmlFileInputRef.current?.click();
    } catch (err) {
      console.warn("Native file picker failed, falling back to browser input:", err);
      htmlFileInputRef.current?.click();
    }
  };

  const handleHtmlFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    try {
      const targetId = activeDevice?.id || "";
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const fileName = file.name;
        setStatusMessage({ text: `Streaming "${fileName}" to phone...`, type: "info" });

        // If local system path is exposed
        const filePath = (file as any).path;
        if (filePath && typeof filePath === "string" && filePath.length > 0) {
          await sendFileToDevice(targetId, filePath);
        } else {
          // Stream raw bytes directly to phone
          const arrayBuffer = await file.arrayBuffer();
          const bytes = Array.from(new Uint8Array(arrayBuffer));
          await sendFileBytes(targetId, fileName, bytes);
        }
      }
    } catch (err) {
      console.error("Failed to beam file to phone:", err);
      setStatusMessage({ text: `Transfer failed: ${String(err)}`, type: "error" });
      setTimeout(() => setStatusMessage(null), 4000);
    } finally {
      e.target.value = "";
    }
  };

  const handleOpenFolder = async () => {
    try {
      await openDownloadsFolder();
    } catch (err) {
      console.error("Failed to open downloads folder:", err);
    }
  };

  const handleOpenFile = async (path: string) => {
    try {
      await openFile(path);
    } catch (err) {
      console.error("Failed to open file:", err);
    }
  };

  const handleDeleteFile = async (path: string) => {
    try {
      await deleteReceivedFile(path);
      await fetchReceivedFiles();
      setStatusMessage({ text: "File deleted successfully", type: "info" });
      setTimeout(() => setStatusMessage(null), 3000);
    } catch (err) {
      console.error("Failed to delete file:", err);
    }
  };

  const formatBytes = (bytes: number) => {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB", "TB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
  };

  const formatTime = (ms: number) => {
    if (!ms) return "";
    const date = new Date(ms);
    return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  };

  const getFileCategoryIcon = (category: string) => {
    switch (category) {
      case "image":
        return <ImageIcon size={18} className="text-[#32C5FF]" />;
      case "video":
        return <Video size={18} className="text-[#7C5CFC]" />;
      case "audio":
        return <Music size={18} className="text-[#35D49A]" />;
      case "document":
        return <FileText size={18} className="text-[#FFB84D]" />;
      case "archive":
        return <Archive size={18} className="text-[#FF5D73]" />;
      default:
        return <File size={18} className="text-[#929BB0]" />;
    }
  };

  const filteredReceived = receivedFiles.filter((f) => {
    const matchesFilter = fileFilter === "all" || f.category === fileFilter;
    const matchesSearch = f.name.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesFilter && matchesSearch;
  });

  return (
    <div className="space-y-6 max-w-6xl mx-auto pb-16">
      {/* Hidden Fallback File Input */}
      <input
        type="file"
        ref={htmlFileInputRef}
        multiple
        onChange={handleHtmlFileChange}
        className="hidden"
      />

      {/* Top Header & Overview */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 rounded-full bg-[#7C5CFC]/15 border border-[#7C5CFC]/30 px-3 py-1 text-xs font-semibold text-[#7C5CFC] mb-2">
            <Sparkles size={14} />
            <span>Wireless Ultra-Fast TCP File Hub</span>
          </div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight">
            File Transfers & Storage
          </h1>
          <p className="text-sm text-[#929BB0]">
            Bidirectional zero-cable wireless file exchange with your phone.
          </p>
        </div>

        {/* Global Action Buttons */}
        <div className="flex items-center gap-3">
          <button
            onClick={handleOpenFolder}
            className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-[#171D2B] hover:bg-[#20283A] text-white border border-[#20283A] text-xs font-semibold transition-all duration-200 cursor-pointer shadow-sm hover:border-[#7C5CFC]/50"
          >
            <FolderOpen size={15} className="text-[#32C5FF]" />
            <span>Open Downloads Folder</span>
          </button>

          <button
            onClick={handleSendFile}
            className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-[#7C5CFC] to-[#32C5FF] hover:opacity-90 text-white text-xs font-bold transition-all duration-200 cursor-pointer shadow-lg shadow-[#7C5CFC]/25"
          >
            <Send size={15} />
            <span>Send File to Phone</span>
          </button>
        </div>
      </div>

      {/* Floating Status Notification */}
      {statusMessage && (
        <div
          className={`px-4 py-3 rounded-2xl border text-xs font-medium flex items-center justify-between gap-3 animate-in fade-in slide-in-from-top-2 ${
            statusMessage.type === "success"
              ? "bg-[#35D49A]/10 border-[#35D49A]/30 text-[#35D49A]"
              : statusMessage.type === "error"
              ? "bg-[#FF5D73]/10 border-[#FF5D73]/30 text-[#FF5D73]"
              : "bg-[#7C5CFC]/10 border-[#7C5CFC]/30 text-[#32C5FF]"
          }`}
        >
          <div className="flex items-center gap-2">
            {statusMessage.type === "success" ? (
              <CheckCircle2 size={16} />
            ) : (
              <AlertCircle size={16} />
            )}
            <span>{statusMessage.text}</span>
          </div>
          <button
            onClick={() => setStatusMessage(null)}
            className="text-xs opacity-70 hover:opacity-100 cursor-pointer"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Mode Tabs */}
      <div className="flex items-center gap-2 p-1 bg-[#111522] rounded-2xl border border-[#20283A] w-fit">
        <button
          onClick={() => setActiveTab("transfers")}
          className={`flex items-center gap-2 px-5 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
            activeTab === "transfers"
              ? "bg-[#171D2B] text-white shadow-md border border-[#7C5CFC]/40"
              : "text-[#929BB0] hover:text-white"
          }`}
        >
          <FolderSync size={14} className={activeTab === "transfers" ? "text-[#32C5FF]" : ""} />
          <span>Wireless Transfers & Received Files</span>
          <span className="ml-1.5 px-1.5 py-0.5 rounded-full text-[10px] bg-[#20283A] text-[#32C5FF]">
            {receivedFiles.length}
          </span>
        </button>

        <button
          onClick={() => setActiveTab("storage")}
          className={`flex items-center gap-2 px-5 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
            activeTab === "storage"
              ? "bg-[#171D2B] text-white shadow-md border border-[#7C5CFC]/40"
              : "text-[#929BB0] hover:text-white"
          }`}
        >
          <HardDrive size={14} className={activeTab === "storage" ? "text-[#7C5CFC]" : ""} />
          <span>Phone Internal Storage Browser</span>
        </button>
      </div>

      {activeTab === "transfers" ? (
        <div className="space-y-6">
          {/* Top Send & Dropzone Banner */}
          <div
            onClick={handleSendFile}
            className="group relative overflow-hidden rounded-3xl bg-gradient-to-br from-[#171D2B]/90 to-[#111522]/90 border border-dashed border-[#7C5CFC]/40 hover:border-[#32C5FF] p-8 text-center cursor-pointer transition-all duration-300 shadow-xl hover:shadow-[#7C5CFC]/10"
          >
            <div className="pointer-events-none absolute inset-0 bg-radial from-[#7C5CFC]/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />

            <div className="max-w-md mx-auto space-y-3 relative z-10">
              <div className="w-14 h-14 mx-auto rounded-2xl bg-gradient-to-tr from-[#7C5CFC]/20 to-[#32C5FF]/20 border border-[#7C5CFC]/40 flex items-center justify-center text-[#32C5FF] group-hover:scale-105 transition-transform">
                <UploadCloud size={28} />
              </div>

              <div className="space-y-1">
                <h3 className="text-base font-bold text-white group-hover:text-[#32C5FF] transition-colors">
                  Send Files to {activeDevice?.connected ? activeDevice.name : "Android Phone"}
                </h3>
                <p className="text-xs text-[#929BB0]">
                  Click to select any files, videos, documents, or photos to instantly beam to your phone over local Wi-Fi.
                </p>
              </div>

              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#20283A] text-[11px] text-[#32C5FF] font-medium border border-[#20283A]">
                <Zap size={12} />
                <span>Zero Cloud &bull; Direct High-Speed TCP Stream &bull; Lossless</span>
              </div>
            </div>
          </div>

          {/* Active Live Transfers Queue */}
          {activeTransfers.length > 0 && (
            <div className="rounded-2xl bg-[#111522] border border-[#20283A] p-5 space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <ArrowUpRight size={16} className="text-[#32C5FF]" />
                  <h3 className="text-sm font-bold text-white">Live Outgoing Transfers</h3>
                </div>
                <button
                  onClick={() => setActiveTransfers([])}
                  className="text-[11px] text-[#929BB0] hover:text-white cursor-pointer"
                >
                  Clear Queue
                </button>
              </div>

              <div className="space-y-3">
                {activeTransfers.map((item) => (
                  <div
                    key={item.fileId}
                    className="p-3.5 rounded-xl bg-[#171D2B] border border-[#20283A] space-y-2"
                  >
                    <div className="flex items-center justify-between text-xs">
                      <div className="flex items-center gap-2 font-semibold text-white truncate max-w-sm">
                        <FileText size={14} className="text-[#7C5CFC]" />
                        <span className="truncate">{item.fileName}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="text-[#929BB0] text-[11px]">{item.status}</span>
                        <span className="font-mono font-bold text-[#32C5FF]">
                          {Math.round(item.progress * 100)}%
                        </span>
                      </div>
                    </div>

                    <div className="w-full bg-[#20283A] h-2 rounded-full overflow-hidden">
                      <div
                        className="bg-gradient-to-r from-[#7C5CFC] to-[#32C5FF] h-full rounded-full transition-all duration-150"
                        style={{ width: `${Math.min(item.progress * 100, 100)}%` }}
                      />
                    </div>

                    <div className="flex items-center justify-between text-[10px] text-[#929BB0] font-mono">
                      <span>{formatBytes(item.sentBytes)} transferred</span>
                      <span>Total: {formatBytes(item.totalBytes)}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Received Files Section */}
          <div className="rounded-3xl bg-[#111522] border border-[#20283A] p-6 space-y-6">
            {/* Filter Bar & Search */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex items-center gap-2 flex-wrap">
                {["all", "image", "video", "audio", "document", "archive"].map((cat) => (
                  <button
                    key={cat}
                    onClick={() => setFileFilter(cat)}
                    className={`px-3 py-1.5 rounded-xl text-xs font-semibold capitalize transition-all cursor-pointer ${
                      fileFilter === cat
                        ? "bg-[#7C5CFC] text-white shadow-md shadow-[#7C5CFC]/20"
                        : "bg-[#171D2B] text-[#929BB0] hover:text-white hover:bg-[#20283A]"
                    }`}
                  >
                    {cat}
                  </button>
                ))}
              </div>

              <div className="flex items-center gap-2">
                <div className="relative">
                  <Search
                    size={14}
                    className="absolute left-3 top-1/2 -translate-y-1/2 text-[#929BB0]"
                  />
                  <input
                    type="text"
                    placeholder="Search received files..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-56 pl-9 pr-3 py-1.5 rounded-xl bg-[#171D2B] border border-[#20283A] text-xs text-white placeholder-[#929BB0]/60 focus:outline-none focus:border-[#7C5CFC]"
                  />
                </div>

                <button
                  onClick={fetchReceivedFiles}
                  disabled={loadingReceived}
                  className="p-2 rounded-xl bg-[#171D2B] border border-[#20283A] text-[#929BB0] hover:text-white hover:bg-[#20283A] transition-colors cursor-pointer"
                  title="Refresh Files"
                >
                  <RefreshCw
                    size={14}
                    className={loadingReceived ? "animate-spin text-[#32C5FF]" : ""}
                  />
                </button>
              </div>
            </div>

            {/* Received Files List / Grid */}
            {filteredReceived.length === 0 ? (
              <div className="py-16 text-center space-y-3">
                <div className="w-12 h-12 mx-auto rounded-2xl bg-[#20283A] flex items-center justify-center text-[#929BB0]">
                  <ArrowDownLeft size={24} />
                </div>
                <div className="space-y-1">
                  <h4 className="text-sm font-bold text-white">No Received Files Yet</h4>
                  <p className="text-xs text-[#929BB0] max-w-sm mx-auto">
                    Files sent from your Android phone in AetherLink will appear here automatically and be saved directly to your Downloads folder.
                  </p>
                </div>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                {filteredReceived.map((file) => (
                  <div
                    key={file.path}
                    className="group relative p-4 rounded-2xl bg-[#171D2B] border border-[#20283A] hover:border-[#7C5CFC]/40 transition-all duration-200 space-y-3"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex items-center gap-3 min-w-0">
                        <div className="w-10 h-10 rounded-xl bg-[#111522] border border-[#20283A] flex items-center justify-center shrink-0">
                          {getFileCategoryIcon(file.category)}
                        </div>
                        <div className="min-w-0">
                          <h4
                            className="text-xs font-bold text-white truncate group-hover:text-[#32C5FF] transition-colors cursor-pointer"
                            title={file.name}
                            onClick={() => handleOpenFile(file.path)}
                          >
                            {file.name}
                          </h4>
                          <div className="flex items-center gap-2 text-[10px] text-[#929BB0] mt-0.5 font-mono">
                            <span>{formatBytes(file.size)}</span>
                            <span>&bull;</span>
                            <span>{formatTime(file.modified)}</span>
                          </div>
                        </div>
                      </div>
                    </div>

                    <div className="pt-2 border-t border-[#20283A]/60 flex items-center justify-between text-xs">
                      <button
                        onClick={() => handleOpenFile(file.path)}
                        className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg bg-[#20283A] hover:bg-[#7C5CFC]/20 text-[#32C5FF] text-[11px] font-semibold transition-colors cursor-pointer"
                      >
                        <Eye size={12} />
                        <span>Open</span>
                      </button>

                      <div className="flex items-center gap-1.5">
                        <button
                          onClick={handleOpenFolder}
                          className="p-1.5 rounded-lg bg-[#20283A] hover:bg-[#32C5FF]/20 text-[#929BB0] hover:text-[#32C5FF] transition-colors cursor-pointer"
                          title="Show in File Manager"
                        >
                          <ExternalLink size={13} />
                        </button>
                        <button
                          onClick={() => handleDeleteFile(file.path)}
                          className="p-1.5 rounded-lg bg-[#20283A] hover:bg-[#FF5D73]/20 text-[#929BB0] hover:text-[#FF5D73] transition-colors cursor-pointer"
                          title="Delete File"
                        >
                          <Trash2 size={13} />
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      ) : (
        /* Phone Internal Storage Browser Tab */
        <div className="space-y-6">
          {!activeDevice?.connected ? (
            <div className="rounded-3xl bg-[#111522] border border-[#20283A] p-12 text-center space-y-4">
              <div className="w-14 h-14 mx-auto rounded-2xl bg-[#7C5CFC]/15 border border-[#7C5CFC]/30 flex items-center justify-center text-[#7C5CFC]">
                <Smartphone size={28} />
              </div>
              <div className="space-y-1">
                <h3 className="text-base font-bold text-white">Phone Storage Offline</h3>
                <p className="text-xs text-[#929BB0] max-w-sm mx-auto">
                  Connect your phone via Wi-Fi or USB ADB to directly browse internal storage directories.
                </p>
              </div>
              <button
                onClick={openQrModal}
                className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-[#7C5CFC] to-[#32C5FF] text-white text-xs font-bold shadow-md cursor-pointer"
              >
                <QrCode size={16} />
                <span>Pair Smartphone</span>
              </button>
            </div>
          ) : (
            <div className="space-y-6">
              <FileToolbar
                onRefresh={fetchPhoneStorage}
                onUp={() => {
                  if (phonePath === "/sdcard" || phonePath === "/") return;
                  const parent = phonePath.substring(0, phonePath.lastIndexOf("/")) || "/";
                  setPhonePath(parent);
                }}
                isUpDisabled={phonePath === "/sdcard" || phonePath === "/"}
                onUpload={async () => {
                  const local = await open({
                    multiple: false,
                    title: "Select file to upload to Android device",
                  });
                  if (!local || Array.isArray(local)) return;
                  setIsUploadingToPhone(true);
                  try {
                    const fileName = local.split("/").pop() || "uploaded_file";
                    const remote = `${phonePath}/${fileName}`;
                    await uploadPhoneFile(activeDevice.id, local, remote);
                    setStatusMessage({ text: `Uploaded "${fileName}" successfully!`, type: "success" });
                    await fetchPhoneStorage();
                  } catch (e) {
                    setStatusMessage({ text: "Upload failed", type: "error" });
                  } finally {
                    setIsUploadingToPhone(false);
                    setTimeout(() => setStatusMessage(null), 3000);
                  }
                }}
                isUploading={isUploadingToPhone}
                search={searchQuery}
                onSearchChange={setSearchQuery}
              />

              <Breadcrumb path={phonePath} onNavigate={(p) => setPhonePath(p)} />

              {loadingPhoneFiles ? (
                <div className="p-16 text-center rounded-2xl bg-[#111522] border border-[#20283A] text-xs text-[#929BB0]">
                  Loading directory contents...
                </div>
              ) : (
                <FileList
                  files={phoneFiles.filter((f) => f.name.toLowerCase().includes(searchQuery.toLowerCase()))}
                  onOpen={(f) => {
                    if (f.is_directory) setPhonePath(f.path);
                  }}
                  onDownload={async (f) => {
                    try {
                      await downloadPhoneFile(activeDevice.id, f.path, f.name);
                      setStatusMessage({ text: `Downloaded "${f.name}"`, type: "success" });
                      setTimeout(() => setStatusMessage(null), 3000);
                    } catch (e) {
                      setStatusMessage({ text: "Download failed", type: "error" });
                    }
                  }}
                />
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
