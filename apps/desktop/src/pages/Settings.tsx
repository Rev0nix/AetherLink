import { useEffect, useState } from "react";
import {
  Palette,
  Globe,
  Wifi,
  Terminal,
  Shield,
  Layers,
  FolderDown,
  FolderOpen,
  Info,
  Sparkles,
  RefreshCw,
  Check,
  Copy,
  ExternalLink,
  Bell,
  Clipboard,
  HardDrive,
  Cpu,
  Laptop,
  CheckCircle2,
  ArrowUpRight,
  Download,
} from "lucide-react";
import {
  getNetworkInfo,
  NetworkInfo,
  getAppInfo,
  AppInfo,
  openDownloadsFolder,
  openUrl,
} from "../services/tauri";
import { useTranslation, SUPPORTED_LANGUAGES, LanguageCode } from "../i18n";
import { useThemeStore, THEME_OPTIONS, ThemeMode } from "../store/themeStore";
import { LinuxFirewallHelper } from "../components/LinuxFirewallHelper";
import { useUpdateStore } from "../services/updateService";

type SettingsTab = "general" | "network" | "automation" | "about";

export default function Settings() {
  const { t, language, setLanguage } = useTranslation();
  const { theme, setTheme } = useThemeStore();
  const {
    isChecking,
    isUpdateAvailable,
    latestVersion,
    currentVersion,
    statusMessage,
    checkUpdates,
    openModal,
  } = useUpdateStore();

  const [activeTab, setActiveTab] = useState<SettingsTab>("general");
  const [networkInfo, setNetworkInfo] = useState<NetworkInfo | null>(null);
  const [appInfo, setAppInfo] = useState<AppInfo | null>(null);
  const [autoCopyClipboard, setAutoCopyClipboard] = useState(true);
  const [autoAcceptFiles, setAutoAcceptFiles] = useState(true);
  const [systemNotifications, setSystemNotifications] = useState(true);
  const [copiedPath, setCopiedPath] = useState(false);
  const [copiedUfw, setCopiedUfw] = useState(false);
  const [isRefreshingNet, setIsRefreshingNet] = useState(false);

  useEffect(() => {
    void fetchNetwork();
    void fetchAppInfo();
  }, []);

  const fetchNetwork = async () => {
    setIsRefreshingNet(true);
    try {
      const info = await getNetworkInfo();
      setNetworkInfo(info);
    } catch (e) {
      console.error("Failed to fetch network info:", e);
    } finally {
      setTimeout(() => setIsRefreshingNet(false), 500);
    }
  };

  const fetchAppInfo = async () => {
    try {
      const info = await getAppInfo();
      setAppInfo(info);
    } catch (e) {
      console.error("Failed to fetch app info:", e);
    }
  };

  const copyDownloadPath = () => {
    navigator.clipboard.writeText("~/Downloads");
    setCopiedPath(true);
    setTimeout(() => setCopiedPath(false), 2000);
  };

  const copyUfw = () => {
    navigator.clipboard.writeText(
      "sudo ufw allow 45876/tcp && sudo ufw allow 5353/udp"
    );
    setCopiedUfw(true);
    setTimeout(() => setCopiedUfw(false), 2000);
  };

  const handleOpenDownloads = async () => {
    try {
      await openDownloadsFolder();
    } catch (e) {
      console.warn("Could not open downloads folder:", e);
    }
  };

  const tabs: { id: SettingsTab; label: string; icon: any }[] = [
    { id: "general", label: t("settings.tab_general"), icon: Palette },
    { id: "network", label: t("settings.tab_network"), icon: Wifi },
    { id: "automation", label: t("settings.tab_automation"), icon: Layers },
    { id: "about", label: t("settings.tab_about"), icon: Info },
  ];

  return (
    <div className="space-y-8 max-w-4xl mx-auto pb-20 animate-fade-in text-[var(--text-primary)]">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 rounded-full bg-[#7C5CFC]/15 border border-[#7C5CFC]/30 px-3 py-1 text-xs font-semibold text-[#7C5CFC] mb-2">
            <Shield size={14} />
            {t("settings.badge")}
          </div>
          <h1 className="text-3xl font-extrabold tracking-tight text-[var(--text-primary)]">
            {t("settings.title")}
          </h1>
          <p className="text-sm text-[var(--text-secondary)] mt-1">
            {t("settings.subtitle")}
          </p>
        </div>

        {/* Quick App Version Pill */}
        <div className="flex items-center gap-2 self-start sm:self-auto px-3.5 py-1.5 rounded-2xl bg-[var(--bg-card)] border border-[var(--border-color)] text-xs font-mono">
          <span className="w-2 h-2 rounded-full bg-[#35D49A] animate-pulse" />
          <span className="text-[var(--text-secondary)]">v{appInfo?.version || currentVersion}</span>
          {isUpdateAvailable && (
            <button
              onClick={openModal}
              className="ml-1 px-2 py-0.5 rounded-md bg-[#32C5FF]/20 text-[#32C5FF] font-bold text-[10px] hover:bg-[#32C5FF]/30 transition cursor-pointer"
            >
              Update
            </button>
          )}
        </div>
      </div>

      {/* Modern Tab Bar */}
      <div className="flex items-center gap-2 p-1.5 rounded-2xl bg-[var(--bg-secondary)] border border-[var(--border-color)] overflow-x-auto">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition whitespace-nowrap cursor-pointer ${
                isActive
                  ? "bg-gradient-to-r from-[#7C5CFC] to-[#32C5FF] text-white shadow-md shadow-[#7C5CFC]/20"
                  : "text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-[var(--bg-surface)]"
              }`}
            >
              <Icon size={15} />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </div>

      {/* TAB 1: APPEARANCE & LANGUAGE */}
      {activeTab === "general" && (
        <div className="space-y-6">
          {/* Theme Selector */}
          <div className="rounded-3xl bg-[var(--bg-secondary)] border border-[var(--border-color)] p-6 space-y-5 shadow-xl">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-2xl bg-[#7C5CFC]/15 text-[#7C5CFC] border border-[#7C5CFC]/30">
                <Palette size={20} />
              </div>
              <div>
                <h2 className="text-base font-bold text-[var(--text-primary)]">
                  {t("appearance.title")}
                </h2>
                <p className="text-xs text-[var(--text-secondary)]">
                  {t("appearance.subtitle")}
                </p>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {THEME_OPTIONS.map((item) => {
                const isSelected = theme === item.id;
                return (
                  <button
                    key={item.id}
                    onClick={() => setTheme(item.id)}
                    className={`relative flex flex-col items-start p-4 rounded-2xl border text-left transition-all cursor-pointer ${
                      isSelected
                        ? "bg-[var(--bg-card)] border-[#7C5CFC] shadow-lg shadow-[#7C5CFC]/15 ring-2 ring-[#7C5CFC]/30"
                        : "bg-[var(--bg-card)]/70 hover:bg-[var(--bg-card)] border-[var(--border-color)] hover:border-[var(--text-secondary)]/40"
                    }`}
                  >
                    <div className="flex items-center justify-between w-full mb-3">
                      <div className="flex items-center gap-2">
                        {/* Swatch */}
                        <div
                          className="w-6 h-6 rounded-lg border border-white/20 shadow-sm flex items-center justify-center"
                          style={{ backgroundColor: item.primaryColor }}
                        >
                          <div
                            className="w-2.5 h-2.5 rounded-full"
                            style={{ backgroundColor: item.accentColor }}
                          />
                        </div>
                        <span className="text-xs font-bold text-[var(--text-primary)]">
                          {t(item.nameKey)}
                        </span>
                      </div>
                      {isSelected ? (
                        <div className="w-5 h-5 rounded-full bg-[#7C5CFC] text-white flex items-center justify-center">
                          <Check size={12} />
                        </div>
                      ) : (
                        <span className="text-[10px] font-mono px-2 py-0.5 rounded-md bg-[var(--bg-surface)] text-[var(--text-secondary)]">
                          {item.badge}
                        </span>
                      )}
                    </div>
                    <p className="text-[11px] text-[var(--text-secondary)] leading-relaxed">
                      {t(item.descKey)}
                    </p>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Language Selector */}
          <div className="rounded-3xl bg-[var(--bg-secondary)] border border-[var(--border-color)] p-6 space-y-5 shadow-xl">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-2xl bg-[#32C5FF]/15 text-[#32C5FF] border border-[#32C5FF]/30">
                <Globe size={20} />
              </div>
              <div>
                <h2 className="text-base font-bold text-[var(--text-primary)]">
                  {t("language.title")}
                </h2>
                <p className="text-xs text-[var(--text-secondary)]">
                  {t("language.subtitle")}
                </p>
              </div>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              {SUPPORTED_LANGUAGES.map((lang) => {
                const isSelected = language === lang.code;
                return (
                  <button
                    key={lang.code}
                    onClick={() => setLanguage(lang.code as LanguageCode)}
                    className={`flex items-center gap-3 p-3.5 rounded-2xl border text-left transition cursor-pointer ${
                      isSelected
                        ? "bg-[var(--bg-card)] border-[#32C5FF] shadow-md shadow-[#32C5FF]/10 ring-2 ring-[#32C5FF]/30"
                        : "bg-[var(--bg-card)]/70 hover:bg-[var(--bg-card)] border-[var(--border-color)] hover:border-[var(--text-secondary)]/40"
                    }`}
                  >
                    <span className="text-2xl">{lang.flag}</span>
                    <div className="truncate">
                      <div className="text-xs font-bold text-[var(--text-primary)] truncate">
                        {lang.nativeName}
                      </div>
                      <div className="text-[10px] text-[var(--text-secondary)] truncate">
                        {lang.name}
                      </div>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: NETWORK & FIREWALL */}
      {activeTab === "network" && (
        <div className="space-y-6">
          <div className="rounded-3xl bg-[var(--bg-secondary)] border border-[var(--border-color)] p-6 space-y-5 shadow-xl">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-2xl bg-[#32C5FF]/15 text-[#32C5FF] border border-[#32C5FF]/30">
                  <Wifi size={20} />
                </div>
                <div>
                  <h2 className="text-base font-bold text-[var(--text-primary)]">
                    {t("network.title")}
                  </h2>
                  <p className="text-xs text-[var(--text-secondary)]">
                    {t("network.subtitle")}
                  </p>
                </div>
              </div>
              <button
                onClick={fetchNetwork}
                className="p-2.5 rounded-xl bg-[var(--bg-card)] hover:bg-[var(--bg-surface)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition cursor-pointer"
                title="Refresh network details"
              >
                <RefreshCw
                  size={16}
                  className={isRefreshingNet ? "animate-spin text-[#32C5FF]" : ""}
                />
              </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
              <div className="p-4 rounded-2xl bg-[var(--bg-card)] border border-[var(--border-color)] space-y-1">
                <span className="text-[var(--text-secondary)] font-medium text-[11px] uppercase tracking-wider">
                  {t("network.ip")}
                </span>
                <div className="text-base font-mono text-[#32C5FF] font-bold">
                  {networkInfo?.local_ip || "192.168.0.118"}
                </div>
                <p className="text-[11px] text-[var(--text-secondary)]">
                  {t("network.ip_desc")}
                </p>
              </div>

              <div className="p-4 rounded-2xl bg-[var(--bg-card)] border border-[var(--border-color)] space-y-1">
                <span className="text-[var(--text-secondary)] font-medium text-[11px] uppercase tracking-wider">
                  {t("network.port")}
                </span>
                <div className="text-base font-mono text-[#35D49A] font-bold">
                  {networkInfo?.port || 45876}
                </div>
                <p className="text-[11px] text-[var(--text-secondary)]">
                  {t("network.port_desc")}
                </p>
              </div>

              <div className="p-4 rounded-2xl bg-[var(--bg-card)] border border-[var(--border-color)] space-y-1 md:col-span-2">
                <span className="text-[var(--text-secondary)] font-medium text-[11px] uppercase tracking-wider">
                  {t("network.mdns")}
                </span>
                <div className="text-xs font-mono text-[#7C5CFC] font-semibold">
                  {networkInfo?.mdns_service || "_aetherlink._tcp.local."}
                </div>
                <p className="text-[11px] text-[var(--text-secondary)]">
                  {t("network.mdns_desc")}
                </p>
              </div>
            </div>

            {/* Linux Firewall Helper (All Distros) */}
            <LinuxFirewallHelper />
          </div>
        </div>
      )}

      {/* TAB 3: AUTOMATION & SYNC */}
      {activeTab === "automation" && (
        <div className="space-y-6">
          {/* Storage Location */}
          <div className="rounded-3xl bg-[var(--bg-secondary)] border border-[var(--border-color)] p-6 space-y-5 shadow-xl">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-2xl bg-[#7C5CFC]/15 text-[#7C5CFC] border border-[#7C5CFC]/30">
                <FolderDown size={20} />
              </div>
              <div>
                <h2 className="text-base font-bold text-[var(--text-primary)]">
                  {t("storage.title")}
                </h2>
                <p className="text-xs text-[var(--text-secondary)]">
                  {t("storage.subtitle")}
                </p>
              </div>
            </div>

            <div className="p-4 rounded-2xl bg-[var(--bg-card)] border border-[var(--border-color)] flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div className="space-y-1">
                <span className="text-[11px] text-[var(--text-secondary)] font-medium uppercase tracking-wider">
                  {t("storage.download_folder")}
                </span>
                <div className="text-xs font-mono text-[var(--text-primary)] font-bold">
                  ~/Downloads/
                </div>
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={copyDownloadPath}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-[var(--bg-surface)] hover:bg-[#7C5CFC]/20 text-[var(--text-secondary)] hover:text-[#32C5FF] text-xs font-medium transition cursor-pointer"
                >
                  {copiedPath ? (
                    <Check size={13} className="text-[#35D49A]" />
                  ) : (
                    <Copy size={13} />
                  )}
                  <span>{copiedPath ? t("network.copied") : t("storage.copy_path")}</span>
                </button>
                <button
                  onClick={handleOpenDownloads}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-gradient-to-r from-[#7C5CFC] to-[#32C5FF] text-white text-xs font-bold transition hover:opacity-90 shadow-sm cursor-pointer"
                >
                  <FolderOpen size={13} />
                  <span>{t("storage.open_folder")}</span>
                </button>
              </div>
            </div>
          </div>

          {/* Sync Preferences & Toggles */}
          <div className="rounded-3xl bg-[var(--bg-secondary)] border border-[var(--border-color)] p-6 space-y-5 shadow-xl">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-2xl bg-[#35D49A]/15 text-[#35D49A] border border-[#35D49A]/30">
                <Layers size={20} />
              </div>
              <div>
                <h2 className="text-base font-bold text-[var(--text-primary)]">
                  {t("automation.title")}
                </h2>
                <p className="text-xs text-[var(--text-secondary)]">
                  {t("automation.subtitle")}
                </p>
              </div>
            </div>

            <div className="space-y-3">
              {/* Toggle 1 */}
              <div className="flex items-center justify-between p-4 rounded-2xl bg-[var(--bg-card)] border border-[var(--border-color)]">
                <div className="space-y-0.5">
                  <div className="text-xs font-bold text-[var(--text-primary)] flex items-center gap-2">
                    <Clipboard size={14} className="text-[#32C5FF]" />
                    {t("automation.auto_clip")}
                  </div>
                  <p className="text-[11px] text-[var(--text-secondary)]">
                    {t("automation.auto_clip_desc")}
                  </p>
                </div>
                <button
                  onClick={() => setAutoCopyClipboard(!autoCopyClipboard)}
                  className={`w-12 h-6 rounded-full transition-colors relative cursor-pointer ${
                    autoCopyClipboard ? "bg-[#7C5CFC]" : "bg-[var(--bg-surface)]"
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
              <div className="flex items-center justify-between p-4 rounded-2xl bg-[var(--bg-card)] border border-[var(--border-color)]">
                <div className="space-y-0.5">
                  <div className="text-xs font-bold text-[var(--text-primary)] flex items-center gap-2">
                    <HardDrive size={14} className="text-[#35D49A]" />
                    {t("automation.auto_files")}
                  </div>
                  <p className="text-[11px] text-[var(--text-secondary)]">
                    {t("automation.auto_files_desc")}
                  </p>
                </div>
                <button
                  onClick={() => setAutoAcceptFiles(!autoAcceptFiles)}
                  className={`w-12 h-6 rounded-full transition-colors relative cursor-pointer ${
                    autoAcceptFiles ? "bg-[#7C5CFC]" : "bg-[var(--bg-surface)]"
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
              <div className="flex items-center justify-between p-4 rounded-2xl bg-[var(--bg-card)] border border-[var(--border-color)]">
                <div className="space-y-0.5">
                  <div className="text-xs font-bold text-[var(--text-primary)] flex items-center gap-2">
                    <Bell size={14} className="text-[#FFB84D]" />
                    {t("automation.notifications")}
                  </div>
                  <p className="text-[11px] text-[var(--text-secondary)]">
                    {t("automation.notifications_desc")}
                  </p>
                </div>
                <button
                  onClick={() => setSystemNotifications(!systemNotifications)}
                  className={`w-12 h-6 rounded-full transition-colors relative cursor-pointer ${
                    systemNotifications ? "bg-[#7C5CFC]" : "bg-[var(--bg-surface)]"
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
        </div>
      )}

      {/* TAB 4: ABOUT & UPDATES */}
      {activeTab === "about" && (
        <div className="space-y-6">
          {/* Main About Card */}
          <div className="rounded-3xl bg-[var(--bg-secondary)] border border-[var(--border-color)] p-6 space-y-6 shadow-xl">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex items-center gap-4">
                <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-[#7C5CFC] via-[#32C5FF] to-cyan-300 flex items-center justify-center shadow-xl shadow-[#7C5CFC]/30 text-white">
                  <Sparkles size={28} />
                </div>
                <div>
                  <h2 className="text-xl font-extrabold tracking-tight text-[var(--text-primary)]">
                    VEXYRON Desktop
                  </h2>
                  <p className="text-xs text-[var(--text-secondary)]">
                    {t("nav.tagline")}
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2 self-start sm:self-auto">
                <span className="px-3 py-1 rounded-xl bg-[var(--bg-card)] border border-[var(--border-color)] font-mono text-xs font-bold text-[#32C5FF]">
                  v{appInfo?.version || currentVersion}
                </span>
                <span className="px-3 py-1 rounded-xl bg-[#35D49A]/15 border border-[#35D49A]/30 text-xs font-semibold text-[#35D49A]">
                  Release
                </span>
              </div>
            </div>

            {/* Architecture Details Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
              <div className="p-3.5 rounded-2xl bg-[var(--bg-card)] border border-[var(--border-color)] space-y-1">
                <span className="text-[10px] text-[var(--text-secondary)] uppercase font-semibold">
                  {t("about.platform")}
                </span>
                <div className="font-mono font-bold text-[var(--text-primary)] flex items-center gap-1.5">
                  <Laptop size={14} className="text-[#32C5FF]" />
                  <span>
                    {appInfo?.os || "linux"} ({appInfo?.arch || "x86_64"})
                  </span>
                </div>
              </div>

              <div className="p-3.5 rounded-2xl bg-[var(--bg-card)] border border-[var(--border-color)] space-y-1 sm:col-span-2">
                <span className="text-[10px] text-[var(--text-secondary)] uppercase font-semibold">
                  {t("about.engine")}
                </span>
                <div className="font-mono font-bold text-[var(--text-primary)] flex items-center gap-1.5">
                  <Cpu size={14} className="text-[#35D49A]" />
                  <span>{t("about.engine_desc")}</span>
                </div>
              </div>
            </div>

            {/* Online Update Checker Card */}
            <div className="p-5 rounded-2xl bg-[var(--bg-card)] border border-[var(--border-color)] flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-[var(--text-primary)]">
                    {t("about.check_updates")}
                  </span>
                  {statusMessage && (
                    <span
                      className={`text-[10px] px-2 py-0.5 rounded-md font-bold font-mono ${
                        isUpdateAvailable
                          ? "bg-[#32C5FF]/20 text-[#32C5FF]"
                          : "bg-[#35D49A]/20 text-[#35D49A]"
                      }`}
                    >
                      {statusMessage}
                    </span>
                  )}
                </div>
                <p className="text-[11px] text-[var(--text-secondary)]">
                  {isUpdateAvailable
                    ? `New version v${latestVersion} ready to install.`
                    : "Check if a newer version of VEXYRON has been released on GitHub."}
                </p>
              </div>

              <div className="flex items-center gap-2">
                {isUpdateAvailable && (
                  <button
                    onClick={openModal}
                    className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-gradient-to-r from-[#7C5CFC] to-[#32C5FF] text-white text-xs font-bold shadow-md shadow-[#7C5CFC]/20 hover:opacity-90 transition cursor-pointer"
                  >
                    <Download size={14} />
                    <span>Download v{latestVersion}</span>
                  </button>
                )}
                <button
                  onClick={() => checkUpdates(true)}
                  disabled={isChecking}
                  className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-[var(--bg-surface)] hover:bg-[var(--border-color)] text-[var(--text-primary)] text-xs font-semibold border border-[var(--border-color)] transition disabled:opacity-50 cursor-pointer"
                >
                  <RefreshCw
                    size={14}
                    className={isChecking ? "animate-spin text-[#32C5FF]" : ""}
                  />
                  <span>
                    {isChecking ? t("about.checking") : t("about.check_updates")}
                  </span>
                </button>
              </div>
            </div>

            {/* Quick Link Buttons */}
            <div className="flex flex-wrap items-center gap-3 pt-2">
              <button
                onClick={() => openUrl("https://github.com/Rev0nix/VEXYRON")}
                className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-[var(--bg-card)] hover:bg-[var(--bg-surface)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] text-xs font-medium border border-[var(--border-color)] transition cursor-pointer"
              >
                <ExternalLink size={13} />
                <span>{t("about.repo")}</span>
              </button>

              <button
                onClick={() =>
                  openUrl("https://github.com/Rev0nix/VEXYRON/releases")
                }
                className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-[var(--bg-card)] hover:bg-[var(--bg-surface)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] text-xs font-medium border border-[var(--border-color)] transition cursor-pointer"
              >
                <ArrowUpRight size={13} />
                <span>{t("about.releases")}</span>
              </button>

              <span className="text-[11px] text-[var(--text-secondary)] font-mono ml-auto">
                {t("about.license")} • Rev0nix
              </span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
