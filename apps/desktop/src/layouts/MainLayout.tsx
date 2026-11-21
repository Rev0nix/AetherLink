import { useEffect } from "react";
import { Outlet } from "react-router-dom";
import { listen } from "@tauri-apps/api/event";
import Sidebar from "../components/Sidebar";
import TopBar from "../components/TopBar";
import PairingModal from "../components/PairingModal";
import UpdateModal from "../components/UpdateModal";
import { useUpdateStore } from "../services/updateService";
import { useThemeStore } from "../store/themeStore";

export default function MainLayout() {
  const { checkUpdates } = useUpdateStore();
  const { theme } = useThemeStore();

  useEffect(() => {
    // Check for application releases after app startup
    const timer = setTimeout(() => {
      void checkUpdates(false);
    }, 2500);

    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    // Global listener: Automatically write incoming clipboard text from phone to OS clipboard & history
    const unlisten = listen<{
      content: string;
      timestamp: number;
      source: string;
    }>("clipboard-received", async (event) => {
      const text = event.payload.content;
      if (!text) return;

      // 1. Write to laptop system clipboard
      try {
        if (navigator.clipboard && navigator.clipboard.writeText) {
          await navigator.clipboard.writeText(text);
          console.log("[AetherLink] Auto-synced phone text to OS clipboard:", text.slice(0, 30));
        }
      } catch (err) {
        console.warn("[AetherLink] navigator.clipboard write failed:", err);
      }

      // 2. Persist in clipboard history
      try {
        const stored = localStorage.getItem("aetherlink_clipboard_history");
        const list = stored ? JSON.parse(stored) : [];
        const updated = [
          {
            id: String(Date.now()),
            content: text,
            timestamp: event.payload.timestamp || Date.now(),
            source: event.payload.source || "Android Phone",
            pinned: false,
          },
          ...list.filter((item: any) => item.content !== text),
        ];
        localStorage.setItem("aetherlink_clipboard_history", JSON.stringify(updated.slice(0, 50)));
      } catch (_e) {}
    });

    return () => {
      unlisten.then((f) => f());
    };
  }, []);

  return (
    <div
      data-theme={theme}
      className="flex h-screen bg-[var(--bg-primary)] text-[var(--text-primary)] overflow-hidden antialiased font-sans selection:bg-[#7C5CFC]/30 selection:text-[#32C5FF]"
    >
      {/* Sidebar Navigation */}
      <Sidebar />

      {/* Main Content Area */}
      <div className="flex flex-1 flex-col overflow-hidden relative">
        <TopBar />

        <main className="flex-1 overflow-y-auto overflow-x-hidden p-8 bg-[var(--bg-primary)] bg-dot-matrix relative">
          {/* Subtle radial ambient glow at top */}
          <div className="pointer-events-none absolute top-0 left-1/3 w-[600px] h-[350px] bg-gradient-to-b from-[#7C5CFC]/10 via-[#32C5FF]/5 to-transparent blur-3xl" />
          
          <div className="relative z-10 max-w-7xl mx-auto">
            <Outlet />
          </div>
        </main>
      </div>

      {/* Global Pairing & QR Modal */}
      <PairingModal />

      {/* Global New Version Update Modal */}
      <UpdateModal />
    </div>
  );
}
