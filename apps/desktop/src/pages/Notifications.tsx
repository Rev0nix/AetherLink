import { useEffect, useState } from "react";
import { listen } from "@tauri-apps/api/event";
import {
  Bell,
  Trash2,
  CheckCircle2,
  Smartphone,
  Shield,
  MessageSquare,
  Search,
  Filter,
  X,
  Radio,
} from "lucide-react";
import { NotificationItem } from "../services/tauri";

const STORAGE_KEY = "vexyron_notifications_history";

export default function Notifications() {
  const [notifications, setNotifications] = useState<NotificationItem[]>(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  const [selectedApp, setSelectedApp] = useState<string>("all");
  const [search, setSearch] = useState("");

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(notifications));
  }, [notifications]);

  useEffect(() => {
    const unlisten = listen<{
      id: string;
      app_name: string;
      package_name: string;
      title: string;
      text: string;
      timestamp: number;
    }>("notification-received", (event) => {
      const newItem: NotificationItem = {
        id: event.payload.id || String(Date.now()),
        app_name: event.payload.app_name || "Android App",
        package_name: event.payload.package_name || "com.android",
        title: event.payload.title || "New Notification",
        text: event.payload.text || "",
        timestamp: event.payload.timestamp || Date.now(),
      };

      setNotifications((prev) => [newItem, ...prev]);

      // Native browser notification if permitted
      if ("Notification" in window && Notification.permission === "granted") {
        new Notification(`[${newItem.app_name}] ${newItem.title}`, {
          body: newItem.text,
        });
      }
    });

    return () => {
      unlisten.then((f) => f());
    };
  }, []);

  const dismiss = (id: string) => {
    setNotifications((prev) => prev.filter((n) => n.id !== id));
  };

  const clearAll = () => {
    if (confirm("Clear all notifications?")) {
      setNotifications([]);
    }
  };

  const apps = Array.from(new Set(notifications.map((n) => n.app_name)));

  const filtered = notifications.filter((n) => {
    const matchesApp = selectedApp === "all" || n.app_name === selectedApp;
    const matchesSearch =
      n.title.toLowerCase().includes(search.toLowerCase()) ||
      n.text.toLowerCase().includes(search.toLowerCase()) ||
      n.app_name.toLowerCase().includes(search.toLowerCase());
    return matchesApp && matchesSearch;
  });

  const getAppStyle = (name: string) => {
    const lower = name.toLowerCase();
    if (lower.includes("whatsapp"))
      return "bg-[#35D49A]/15 text-[#35D49A] border-[#35D49A]/30";
    if (lower.includes("slack"))
      return "bg-[#FFB84D]/15 text-[#FFB84D] border-[#FFB84D]/30";
    if (lower.includes("mail") || lower.includes("gmail"))
      return "bg-[#FF5D73]/15 text-[#FF5D73] border-[#FF5D73]/30";
    if (lower.includes("telegram"))
      return "bg-[#32C5FF]/15 text-[#32C5FF] border-[#32C5FF]/30";
    return "bg-[#7C5CFC]/15 text-[#7C5CFC] border-[#7C5CFC]/30";
  };

  return (
    <div className="space-y-8 max-w-5xl mx-auto pb-12">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 rounded-full bg-[#FFB84D]/15 border border-[#FFB84D]/30 px-3 py-1 text-xs font-semibold text-[#FFB84D] mb-2">
            <Radio size={12} className="animate-pulse" />
            Live Notification Mirroring
          </div>
          <h1 className="text-3xl font-extrabold text-white">
            Notification Stream
          </h1>
          <p className="text-sm text-[#929BB0]">
            Mirror incoming messages, alerts, and system notifications from your phone to this desktop.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={clearAll}
            disabled={notifications.length === 0}
            className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-[#171D2B] hover:bg-[#FF5D73]/15 border border-[#20283A] hover:border-[#FF5D73]/30 text-xs font-semibold text-[#929BB0] hover:text-[#FF5D73] disabled:opacity-40 transition cursor-pointer"
          >
            <Trash2 size={15} />
            <span>Clear Stream</span>
          </button>
        </div>
      </div>

      {/* Filter & Search Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        {/* App Filter Pills */}
        <div className="flex items-center gap-2 overflow-x-auto pb-1 sm:pb-0">
          <button
            onClick={() => setSelectedApp("all")}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition cursor-pointer ${
              selectedApp === "all"
                ? "bg-[#7C5CFC] text-white shadow-sm shadow-[#7C5CFC]/30"
                : "bg-[#171D2B] text-[#929BB0] hover:text-white hover:bg-[#20283A]"
            }`}
          >
            All Apps ({notifications.length})
          </button>
          {apps.map((app) => (
            <button
              key={app}
              onClick={() => setSelectedApp(app)}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition cursor-pointer ${
                selectedApp === app
                  ? "bg-[#7C5CFC] text-white shadow-sm shadow-[#7C5CFC]/30"
                  : "bg-[#171D2B] text-[#929BB0] hover:text-white hover:bg-[#20283A]"
              }`}
            >
              {app}
            </button>
          ))}
        </div>

        {/* Search */}
        <div className="relative">
          <Search
            size={14}
            className="absolute left-3 top-1/2 -translate-y-1/2 text-[#929BB0]"
          />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search notifications..."
            className="pl-9 pr-3 py-1.5 rounded-xl bg-[#171D2B] border border-[#20283A] text-xs text-white placeholder-[#929BB0]/60 outline-none focus:border-[#7C5CFC] w-56 transition"
          />
        </div>
      </div>

      {/* Notifications List */}
      {filtered.length === 0 ? (
        <div className="rounded-3xl bg-[#111522]/80 border border-[#20283A] p-12 text-center space-y-3">
          <Bell size={36} className="mx-auto text-[#929BB0]/40" />
          <h3 className="text-base font-bold text-white">No Notifications</h3>
          <p className="text-xs text-[#929BB0] max-w-sm mx-auto">
            When apps on your phone receive notifications, they will be mirrored here in real-time over the secure socket.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map((item) => (
            <div
              key={item.id}
              className="rounded-2xl bg-[#171D2B]/90 border border-[#20283A] p-5 hover:border-[#7C5CFC]/40 transition-all flex items-start justify-between gap-4 shadow-sm"
            >
              <div className="flex items-start gap-3.5">
                <div
                  className={`px-2.5 py-1 rounded-xl text-[10px] font-bold border uppercase tracking-wider shrink-0 ${getAppStyle(
                    item.app_name
                  )}`}
                >
                  {item.app_name}
                </div>

                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <h3 className="font-bold text-white text-sm">
                      {item.title}
                    </h3>
                    <span className="text-[11px] text-[#929BB0]">
                      {new Date(item.timestamp).toLocaleTimeString([], {
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </span>
                  </div>
                  {item.text && (
                    <p className="text-xs text-[#929BB0] leading-relaxed break-words">
                      {item.text}
                    </p>
                  )}
                  <div className="text-[10px] font-mono text-[#929BB0]/60">
                    {item.package_name}
                  </div>
                </div>
              </div>

              <button
                onClick={() => dismiss(item.id)}
                className="p-1.5 rounded-lg text-[#929BB0] hover:text-white hover:bg-[#20283A] transition cursor-pointer"
                title="Dismiss"
              >
                <X size={15} />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
