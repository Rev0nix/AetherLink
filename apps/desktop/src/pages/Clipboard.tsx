import { useEffect, useState } from "react";
import { listen } from "@tauri-apps/api/event";
import {
  Clipboard as ClipboardIcon,
  Send,
  Copy,
  Check,
  Trash2,
  Pin,
  Search,
  Sparkles,
  Smartphone,
  Monitor,
  CheckCircle2,
  Filter,
} from "lucide-react";
import { sendClipboard, ClipboardItem } from "../services/tauri";

const STORAGE_KEY = "vexyron_clipboard_history";

export default function Clipboard() {
  const [inputText, setInputText] = useState("");
  const [history, setHistory] = useState<ClipboardItem[]>(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState<"all" | "pinned" | "phone" | "desktop">("all");
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [sending, setSending] = useState(false);
  const [sendSuccess, setSendSuccess] = useState(false);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(history));
  }, [history]);

  useEffect(() => {
    const unlisten = listen<{
      content: string;
      timestamp: number;
      source: string;
    }>("clipboard-received", (event) => {
      const newItem: ClipboardItem = {
        id: String(Date.now()),
        content: event.payload.content,
        timestamp: event.payload.timestamp || Date.now(),
        source: event.payload.source || "Android Phone",
        pinned: false,
      };

      setHistory((prev) => [newItem, ...prev.filter((i) => i.content !== newItem.content)]);

      // Auto-copy to desktop clipboard if permitted
      if (navigator.clipboard) {
        navigator.clipboard.writeText(newItem.content).catch(() => {});
      }
    });

    return () => {
      unlisten.then((f) => f());
    };
  }, []);

  const handleSend = async () => {
    if (!inputText.trim()) return;
    setSending(true);
    try {
      await sendClipboard(inputText);
      const newItem: ClipboardItem = {
        id: String(Date.now()),
        content: inputText,
        timestamp: Date.now(),
        source: "Desktop PC",
        pinned: false,
      };
      setHistory((prev) => [newItem, ...prev]);
      setInputText("");
      setSendSuccess(true);
      setTimeout(() => setSendSuccess(false), 2500);
    } catch (e) {
      console.error("Failed to push clipboard:", e);
    } finally {
      setSending(false);
    }
  };

  const handlePasteFromOS = async () => {
    try {
      const text = await navigator.clipboard.readText();
      if (text) {
        setInputText(text);
      }
    } catch (e) {
      console.error("Failed to read clipboard:", e);
    }
  };

  const copyToClipboard = async (item: ClipboardItem) => {
    try {
      await navigator.clipboard.writeText(item.content);
      setCopiedId(item.id);
      setTimeout(() => setCopiedId(null), 2000);
    } catch (e) {
      console.error("Copy failed:", e);
    }
  };

  const togglePin = (id: string) => {
    setHistory((prev) =>
      prev.map((item) =>
        item.id === id ? { ...item, pinned: !item.pinned } : item
      )
    );
  };

  const deleteItem = (id: string) => {
    setHistory((prev) => prev.filter((item) => item.id !== id));
  };

  const clearAll = () => {
    if (confirm("Clear all unpinned clipboard items?")) {
      setHistory((prev) => prev.filter((item) => item.pinned));
    }
  };

  const filteredHistory = history.filter((item) => {
    const matchesSearch = item.content.toLowerCase().includes(search.toLowerCase());
    if (!matchesSearch) return false;
    if (filter === "pinned") return item.pinned;
    if (filter === "phone") return item.source.toLowerCase().includes("android") || item.source.toLowerCase().includes("phone");
    if (filter === "desktop") return item.source.toLowerCase().includes("desktop");
    return true;
  });

  return (
    <div className="space-y-8 max-w-5xl mx-auto pb-12">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 rounded-full bg-[#32C5FF]/15 border border-[#32C5FF]/30 px-3 py-1 text-xs font-semibold text-[#32C5FF] mb-2">
            <Sparkles size={14} />
            Seamless Clipboard Sync
          </div>
          <h1 className="text-3xl font-extrabold text-white">Clipboard Hub</h1>
          <p className="text-sm text-[#929BB0]">
            Push text or links instantly to your Android smartphone, or copy incoming phone clips.
          </p>
        </div>

        <button
          onClick={handlePasteFromOS}
          className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-[#171D2B] hover:bg-[#20283A] border border-[#20283A] text-xs font-semibold text-[#32C5FF] transition cursor-pointer self-start sm:self-auto"
        >
          <ClipboardIcon size={15} />
          <span>Paste from OS Clipboard</span>
        </button>
      </div>

      {/* Push to Phone Input Box */}
      <div className="rounded-3xl bg-[#111522]/90 border border-[#20283A] p-6 shadow-xl space-y-4">
        <div className="flex items-center justify-between">
          <label className="text-xs font-bold text-[#F5F7FF] flex items-center gap-2">
            <Send size={14} className="text-[#7C5CFC]" />
            Push Text / Link to Phone
          </label>
          {sendSuccess && (
            <span className="text-xs text-[#35D49A] flex items-center gap-1 font-semibold animate-in fade-in">
              <CheckCircle2 size={13} />
              Synced with Phone!
            </span>
          )}
        </div>

        <textarea
          rows={3}
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          placeholder="Type or paste any text, URL, address, or code snippet to broadcast to your connected Android smartphone..."
          className="w-full rounded-2xl bg-[#090B14] border border-[#20283A] p-4 text-xs text-white placeholder-[#929BB0]/60 outline-none focus:border-[#7C5CFC] transition resize-none font-mono"
        />

        <div className="flex items-center justify-between">
          <span className="text-[11px] text-[#929BB0]">
            {inputText.length} characters
          </span>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setInputText("")}
              disabled={!inputText}
              className="px-3 py-2 rounded-xl text-xs font-semibold text-[#929BB0] hover:text-white disabled:opacity-40 transition cursor-pointer"
            >
              Clear
            </button>
            <button
              onClick={handleSend}
              disabled={sending || !inputText.trim()}
              className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-[#7C5CFC] to-[#32C5FF] hover:opacity-90 disabled:opacity-40 text-white text-xs font-bold transition shadow-md shadow-[#7C5CFC]/25 cursor-pointer active:scale-95"
            >
              <Send size={14} />
              <span>{sending ? "Sending..." : "Send to Phone"}</span>
            </button>
          </div>
        </div>
      </div>

      {/* History Controls & Search */}
      <div className="space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          {/* Filter Pills */}
          <div className="flex items-center gap-2 overflow-x-auto pb-1 sm:pb-0">
            {(["all", "pinned", "phone", "desktop"] as const).map((f) => (
              <button
                key={f}
                onClick={() => setFilter(f)}
                className={`px-3 py-1.5 rounded-xl text-xs font-semibold capitalize transition cursor-pointer ${
                  filter === f
                    ? "bg-[#7C5CFC] text-white shadow-sm shadow-[#7C5CFC]/30"
                    : "bg-[#171D2B] text-[#929BB0] hover:text-white hover:bg-[#20283A]"
                }`}
              >
                {f === "phone" ? "From Phone" : f === "desktop" ? "From Desktop" : f}
              </button>
            ))}
          </div>

          <div className="flex items-center gap-3">
            {/* Search Box */}
            <div className="relative">
              <Search
                size={14}
                className="absolute left-3 top-1/2 -translate-y-1/2 text-[#929BB0]"
              />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search clips..."
                className="pl-9 pr-3 py-1.5 rounded-xl bg-[#171D2B] border border-[#20283A] text-xs text-white placeholder-[#929BB0]/60 outline-none focus:border-[#7C5CFC] w-48 transition"
              />
            </div>

            <button
              onClick={clearAll}
              className="p-2 rounded-xl text-[#929BB0] hover:text-[#FF5D73] hover:bg-[#FF5D73]/10 transition cursor-pointer"
              title="Clear all unpinned clips"
            >
              <Trash2 size={16} />
            </button>
          </div>
        </div>

        {/* Clip List */}
        {filteredHistory.length === 0 ? (
          <div className="rounded-3xl bg-[#111522]/80 border border-[#20283A] p-10 text-center space-y-3">
            <ClipboardIcon size={32} className="mx-auto text-[#929BB0]/50" />
            <h3 className="text-base font-bold text-white">No Clipboard Items</h3>
            <p className="text-xs text-[#929BB0] max-w-sm mx-auto">
              Any text you copy on your phone or send from this app will appear here in real-time.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {filteredHistory.map((item) => {
              const isPhone =
                item.source.toLowerCase().includes("android") ||
                item.source.toLowerCase().includes("phone");

              return (
                <div
                  key={item.id}
                  className={`rounded-2xl border p-4 transition-all space-y-3 ${
                    item.pinned
                      ? "bg-[#171D2B] border-[#7C5CFC]/50 shadow-md shadow-[#7C5CFC]/10"
                      : "bg-[#111522]/90 border-[#20283A] hover:border-[#20283A]"
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span
                        className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                          isPhone
                            ? "bg-[#35D49A]/15 text-[#35D49A] border border-[#35D49A]/30"
                            : "bg-[#7C5CFC]/15 text-[#7C5CFC] border border-[#7C5CFC]/30"
                        }`}
                      >
                        {isPhone ? <Smartphone size={11} /> : <Monitor size={11} />}
                        {item.source}
                      </span>
                      <span className="text-[11px] text-[#929BB0]">
                        {new Date(item.timestamp).toLocaleTimeString([], {
                          hour: "2-digit",
                          minute: "2-digit",
                        })}
                      </span>
                    </div>

                    <div className="flex items-center gap-1.5">
                      <button
                        onClick={() => togglePin(item.id)}
                        className={`p-1.5 rounded-lg transition cursor-pointer ${
                          item.pinned
                            ? "text-[#FFB84D] bg-[#FFB84D]/15"
                            : "text-[#929BB0] hover:text-white hover:bg-[#20283A]"
                        }`}
                        title={item.pinned ? "Unpin clip" : "Pin clip"}
                      >
                        <Pin size={14} className={item.pinned ? "fill-current" : ""} />
                      </button>

                      <button
                        onClick={() => copyToClipboard(item)}
                        className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-[#20283A] hover:bg-[#7C5CFC]/20 text-[#929BB0] hover:text-[#32C5FF] text-xs font-medium transition cursor-pointer"
                        title="Copy to clipboard"
                      >
                        {copiedId === item.id ? (
                          <>
                            <Check size={13} className="text-[#35D49A]" />
                            <span className="text-[#35D49A]">Copied!</span>
                          </>
                        ) : (
                          <>
                            <Copy size={13} />
                            <span>Copy</span>
                          </>
                        )}
                      </button>

                      <button
                        onClick={() => deleteItem(item.id)}
                        className="p-1.5 rounded-lg text-[#929BB0] hover:text-[#FF5D73] hover:bg-[#FF5D73]/10 transition cursor-pointer"
                        title="Delete"
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </div>

                  <div className="p-3 rounded-xl bg-[#090B14] border border-[#20283A] font-mono text-xs text-zinc-200 whitespace-pre-wrap break-words leading-relaxed select-all">
                    {item.content}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
