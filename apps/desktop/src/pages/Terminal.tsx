import { useEffect, useRef, useState } from "react";
import {
  Terminal as TerminalIcon,
  Trash2,
  Send,
  CornerDownLeft,
  KeyRound,
  ShieldAlert,
  Smartphone,
  Laptop,
  CheckCircle2,
  XCircle,
  Copy,
  Sparkles,
} from "lucide-react";
import { listen } from "@tauri-apps/api/event";
import {
  executeTerminalCommand,
  TerminalCommandOutput,
} from "../services/tauri";

interface TerminalLogEntry {
  id: string;
  command: string;
  stdout: string;
  stderr: string;
  exit_code: number;
  cwd: string;
  timestamp: string;
  origin: "local" | "remote";
}

export default function TerminalPage() {
  const [entries, setEntries] = useState<TerminalLogEntry[]>([]);
  const [currentCwd, setCurrentCwd] = useState<string>("~");
  const [hostOs, setHostOs] = useState<string>("Detecting...");
  const [hostname, setHostname] = useState<string>("localhost");
  const [inputCommand, setInputCommand] = useState<string>("");
  const [sudoPassword, setSudoPassword] = useState<string>("");
  const [showSudoInput, setShowSudoInput] = useState<boolean>(false);
  const [isExecuting, setIsExecuting] = useState<boolean>(false);
  const [history, setHistory] = useState<string[]>([]);
  const [historyIndex, setHistoryIndex] = useState<number>(-1);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const scrollBottomRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Initialize session and listen for live events from phone or local execution
  useEffect(() => {
    // Initial banner execution
    void executeTerminalCommand("").then((res) => {
      if (res) {
        setCurrentCwd(res.cwd);
        setHostOs(res.os);
        setHostname(res.hostname);
        if (res.stdout) {
          setEntries([
            {
              id: "init",
              command: "",
              stdout: res.stdout,
              stderr: "",
              exit_code: 0,
              cwd: res.cwd,
              timestamp: new Date().toLocaleTimeString(),
              origin: "local",
            },
          ]);
        }
      }
    }).catch(() => {});

    // Listen to real-time events emitted when the phone sends a command
    const unlistenPromise = listen<TerminalCommandOutput>(
      "terminal-command-executed",
      (event) => {
        const payload = event.payload;
        if (!payload) return;

        if (payload.cwd) {
          setCurrentCwd(payload.cwd);
        }
        if (payload.os) {
          setHostOs(payload.os);
        }
        if (payload.hostname) {
          setHostname(payload.hostname);
        }

        if (payload.stdout === "[CLEAR]") {
          setEntries([]);
          return;
        }

        const newEntry: TerminalLogEntry = {
          id: Math.random().toString(36).substring(2, 9),
          command: payload.command,
          stdout: payload.stdout,
          stderr: payload.stderr,
          exit_code: payload.exit_code,
          cwd: payload.cwd,
          timestamp: new Date().toLocaleTimeString(),
          origin: payload.origin || "remote",
        };

        setEntries((prev) => [...prev.slice(-150), newEntry]);
      }
    );

    return () => {
      unlistenPromise.then((unlisten) => unlisten()).catch(() => {});
    };
  }, []);

  // Auto-scroll on new entries
  useEffect(() => {
    scrollBottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [entries]);

  const handleRunCommand = async (cmdToRun?: string) => {
    const raw = cmdToRun !== undefined ? cmdToRun : inputCommand;
    const trimmed = raw.trim();
    if (!trimmed || isExecuting) return;

    if (trimmed === "clear" || trimmed === "cls") {
      setEntries([]);
      setInputCommand("");
      setHistoryIndex(-1);
      return;
    }

    // Add to command history
    setHistory((prev) => [...prev.filter((h) => h !== trimmed), trimmed]);
    setHistoryIndex(-1);
    setInputCommand("");
    setIsExecuting(true);

    try {
      const res = await executeTerminalCommand(
        trimmed,
        currentCwd,
        showSudoInput && sudoPassword ? sudoPassword : undefined
      );

      if (res) {
        if (res.cwd) setCurrentCwd(res.cwd);
        if (res.os) setHostOs(res.os);
        if (res.hostname) setHostname(res.hostname);

        if (res.stdout === "[CLEAR]") {
          setEntries([]);
        } else {
          const entry: TerminalLogEntry = {
            id: Math.random().toString(36).substring(2, 9),
            command: trimmed,
            stdout: res.stdout,
            stderr: res.stderr,
            exit_code: res.exit_code,
            cwd: res.cwd,
            timestamp: new Date().toLocaleTimeString(),
            origin: "local",
          };
          setEntries((prev) => [...prev.slice(-150), entry]);
        }
      }
    } catch (err: any) {
      setEntries((prev) => [
        ...prev,
        {
          id: Math.random().toString(36).substring(2, 9),
          command: trimmed,
          stdout: "",
          stderr: `Execution error: ${err?.message || err}`,
          exit_code: 1,
          cwd: currentCwd,
          timestamp: new Date().toLocaleTimeString(),
          origin: "local",
        },
      ]);
    } finally {
      setIsExecuting(false);
      inputRef.current?.focus();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      void handleRunCommand();
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      if (history.length > 0) {
        const nextIdx = historyIndex + 1;
        if (nextIdx < history.length) {
          setHistoryIndex(nextIdx);
          setInputCommand(history[history.length - 1 - nextIdx]);
        }
      }
    } else if (e.key === "ArrowDown") {
      e.preventDefault();
      if (historyIndex > 0) {
        const nextIdx = historyIndex - 1;
        setHistoryIndex(nextIdx);
        setInputCommand(history[history.length - 1 - nextIdx]);
      } else if (historyIndex === 0) {
        setHistoryIndex(-1);
        setInputCommand("");
      }
    }
  };

  const copyToClipboard = (text: string, id: string) => {
    void navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 1800);
  };

  const isWindows = hostOs.toLowerCase().includes("windows");
  const quickCommands = isWindows
    ? [
        "dir",
        "pwd",
        "ipconfig",
        "whoami",
        "systeminfo",
        "Get-Process",
        "cd ..",
        "cls",
      ]
    : [
        "ls -la",
        "pwd",
        "whoami",
        "sudo apt update",
        "uname -a",
        "df -h",
        "free -m",
        "git status",
        "cd ..",
        "clear",
      ];

  return (
    <div className="flex flex-col h-[calc(100vh-4rem)] p-4 md:p-6 bg-[var(--bg-main)]">
      {/* 1. Header Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-3 mb-4 p-4 rounded-2xl bg-[var(--bg-card)] border border-[var(--border-color)]">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-gradient-to-br from-[#7C5CFC]/20 to-[#32C5FF]/20 border border-[#7C5CFC]/30 text-[#32C5FF]">
            <TerminalIcon size={20} />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-base font-bold text-[var(--text-primary)]">
                Laptop Terminal & Remote Shell
              </h1>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-[#10B981]/15 text-[#10B981] border border-[#10B981]/30">
                Live Sync
              </span>
            </div>
            <p className="text-xs text-[var(--text-secondary)] font-mono">
              Host: <span className="text-[#32C5FF]">{hostname}</span> ({hostOs}) • CWD:{" "}
              <span className="text-[var(--text-primary)] font-medium">{currentCwd}</span>
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {/* Sudo Toggle Button */}
          <button
            onClick={() => setShowSudoInput(!showSudoInput)}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold transition border cursor-pointer ${
              showSudoInput
                ? "bg-[#EF4444]/15 border-[#EF4444]/40 text-[#EF4444]"
                : "bg-[var(--bg-surface)] hover:bg-[var(--bg-card)] border-[var(--border-color)] text-[var(--text-secondary)]"
            }`}
            title="Configure sudo / admin password for elevated commands"
          >
            <KeyRound size={14} />
            <span>{showSudoInput ? "Sudo Mode Active" : "🔑 Sudo Key"}</span>
          </button>

          {/* Clear Console */}
          <button
            onClick={() => setEntries([])}
            className="flex items-center gap-1 px-3 py-1.5 rounded-xl text-xs font-medium bg-[var(--bg-surface)] hover:bg-[var(--bg-card)] border border-[var(--border-color)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition cursor-pointer"
            title="Clear terminal output"
          >
            <Trash2 size={14} />
            <span>Clear</span>
          </button>
        </div>
      </div>

      {/* Sudo Password Entry Banner */}
      {showSudoInput && (
        <div className="mb-3 p-3 rounded-xl bg-[#EF4444]/10 border border-[#EF4444]/30 flex items-center gap-3">
          <ShieldAlert size={18} className="text-[#EF4444] shrink-0" />
          <div className="flex-1 flex flex-wrap items-center gap-2">
            <span className="text-xs font-medium text-[#EF4444]">
              Elevated Sudo Password:
            </span>
            <input
              type="password"
              value={sudoPassword}
              onChange={(e) => setSudoPassword(e.target.value)}
              placeholder="Enter laptop sudo password for session..."
              className="px-3 py-1 text-xs rounded-lg bg-[var(--bg-surface)] border border-[var(--border-color)] text-[var(--text-primary)] font-mono focus:outline-none focus:border-[#EF4444] w-64"
            />
            <span className="text-[11px] text-[var(--text-muted)]">
              (Passed safely via stdin to sudo -S without tty blocks)
            </span>
          </div>
        </div>
      )}

      {/* 2. Terminal Screen Area */}
      <div className="flex-1 flex flex-col min-h-0 rounded-2xl bg-[#090D16] border border-[var(--border-color)] overflow-hidden shadow-inner font-mono">
        {/* Terminal Header Bar */}
        <div className="flex items-center justify-between px-4 py-2.5 bg-[#0F172A] border-b border-[#1E293B] select-none">
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded-full bg-[#EF4444]/80" />
            <div className="w-3 h-3 rounded-full bg-[#F59E0B]/80" />
            <div className="w-3 h-3 rounded-full bg-[#10B981]/80" />
            <span className="ml-2 text-xs text-[#94A3B8]">
              vexyron-shell • {isWindows ? "PowerShell" : "bash"}
            </span>
          </div>
          <div className="flex items-center gap-2 text-[11px] text-[#64748B]">
            <span>{entries.length} outputs</span>
          </div>
        </div>

        {/* Console Outputs Scroll Window */}
        <div className="flex-1 p-4 overflow-y-auto space-y-3 text-xs leading-relaxed select-text">
          {entries.length === 0 && (
            <div className="flex flex-col items-center justify-center h-full text-center text-[#64748B] space-y-2 py-10">
              <TerminalIcon size={36} className="text-[#32C5FF]/40 mb-1" />
              <p className="font-semibold text-sm text-[#94A3B8]">
                Remote Terminal Ready & Synchronized
              </p>
              <p className="text-xs max-w-md">
                Commands sent from your phone will appear here in real-time, or you can type commands below to run locally.
              </p>
            </div>
          )}

          {entries.map((entry) => (
            <div key={entry.id} className="group relative border-l-2 border-[#1E293B] pl-3 py-1">
              {/* Command Prompt Line */}
              {entry.command && (
                <div className="flex items-center justify-between gap-2 mb-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    {/* Origin Badge */}
                    {entry.origin === "remote" ? (
                      <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-bold bg-[#7C5CFC]/20 text-[#A78BFA] border border-[#7C5CFC]/40">
                        <Smartphone size={11} />
                        <span>PHONE</span>
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-bold bg-[#32C5FF]/20 text-[#32C5FF] border border-[#32C5FF]/40">
                        <Laptop size={11} />
                        <span>LAPTOP</span>
                      </span>
                    )}

                    <span className="text-[#32C5FF] font-bold">
                      {isWindows ? `PS ${entry.cwd}>` : `${hostname}:${entry.cwd}$`}
                    </span>
                    <span className="text-[#F8FAFC] font-semibold">
                      {entry.command}
                    </span>
                  </div>

                  <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition">
                    <span className="text-[10px] text-[#64748B]">
                      {entry.timestamp}
                    </span>
                    <button
                      onClick={() =>
                        copyToClipboard(
                          `${entry.command}\n${entry.stdout}\n${entry.stderr}`,
                          entry.id
                        )
                      }
                      className="p-1 rounded hover:bg-[#1E293B] text-[#94A3B8] hover:text-[#F8FAFC] cursor-pointer"
                      title="Copy output"
                    >
                      {copiedId === entry.id ? (
                        <CheckCircle2 size={13} className="text-[#10B981]" />
                      ) : (
                        <Copy size={13} />
                      )}
                    </button>
                  </div>
                </div>
              )}

              {/* STDOUT Output */}
              {entry.stdout && (
                <pre className="text-[#E2E8F0] whitespace-pre-wrap font-mono break-all bg-[#0B132B]/40 p-2 rounded-lg border border-[#1E293B]/60">
                  {entry.stdout}
                </pre>
              )}

              {/* STDERR Output */}
              {entry.stderr && (
                <pre className="text-[#FF6B6B] whitespace-pre-wrap font-mono break-all bg-[#EF4444]/10 p-2 rounded-lg border border-[#EF4444]/30 mt-1">
                  {entry.stderr}
                </pre>
              )}

              {/* Exit Code Indicator for Non-zero */}
              {entry.exit_code !== 0 && (
                <div className="flex items-center gap-1 mt-1 text-[11px] text-[#FF6B6B]">
                  <XCircle size={12} />
                  <span>Exit code: {entry.exit_code}</span>
                </div>
              )}
            </div>
          ))}
          <div ref={scrollBottomRef} />
        </div>

        {/* Quick Command Chips Toolbar */}
        <div className="flex items-center gap-1.5 px-3 py-2 bg-[#0F172A] border-t border-[#1E293B] overflow-x-auto select-none">
          <span className="text-[11px] font-semibold text-[#64748B] uppercase tracking-wider shrink-0 mr-1">
            Quick:
          </span>
          {quickCommands.map((cmd) => (
            <button
              key={cmd}
              onClick={() => void handleRunCommand(cmd)}
              disabled={isExecuting}
              className="px-2.5 py-1 rounded-lg text-xs bg-[#1E293B] hover:bg-[#334155] text-[#94A3B8] hover:text-[#F8FAFC] font-mono transition shrink-0 cursor-pointer disabled:opacity-50"
            >
              {cmd}
            </button>
          ))}
        </div>

        {/* Input Prompt Row */}
        <div className="flex items-center gap-2 p-3 bg-[#090D16] border-t border-[#1E293B]">
          <span className="text-xs font-bold text-[#32C5FF] shrink-0 pl-1">
            {isWindows ? "PS>" : "$"}
          </span>

          <input
            ref={inputRef}
            type="text"
            value={inputCommand}
            onChange={(e) => setInputCommand(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={
              isWindows
                ? "Enter PowerShell command (e.g. dir, ipconfig, whoami)..."
                : "Enter Linux command (e.g. ls, sudo apt update, git status)..."
            }
            className="flex-1 bg-transparent text-xs text-[#F8FAFC] font-mono placeholder-[#475569] focus:outline-none"
            disabled={isExecuting}
          />

          <button
            onClick={() => void handleRunCommand()}
            disabled={!inputCommand.trim() || isExecuting}
            className="flex items-center justify-center p-2 rounded-xl bg-gradient-to-r from-[#7C5CFC] to-[#32C5FF] text-white hover:opacity-90 disabled:opacity-40 transition cursor-pointer shadow-md shadow-[#7C5CFC]/20"
            title="Execute command"
          >
            {isExecuting ? (
              <Sparkles size={14} className="animate-spin" />
            ) : (
              <Send size={14} />
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
