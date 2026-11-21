import React from "react";
import {
  Sparkles,
  Download,
  ExternalLink,
  X,
  CheckCircle2,
  ArrowRight,
  ShieldCheck,
  Package,
} from "lucide-react";
import { useUpdateStore } from "../services/updateService";
import { openUrl } from "../services/tauri";
import { useTranslation } from "../i18n";

export default function UpdateModal() {
  const {
    isModalOpen,
    closeModal,
    updateInfo,
    currentVersion,
    latestVersion,
  } = useUpdateStore();
  const { t } = useTranslation();

  if (!isModalOpen || !updateInfo) return null;

  const handleDismiss = () => {
    if (latestVersion) {
      sessionStorage.setItem("aetherlink_dismissed_update", latestVersion);
    }
    closeModal();
  };

  const handleDownload = (url?: string) => {
    const targetUrl = url || updateInfo.releaseUrl;
    openUrl(targetUrl);
    closeModal();
  };

  // Format changelog preview (strip very long markdown or limit to first 12 lines)
  const changelogPreview = updateInfo.changelog
    .split("\n")
    .slice(0, 15)
    .join("\n");

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-md animate-fade-in">
      <div className="relative w-full max-w-xl rounded-3xl bg-[var(--bg-secondary)] border border-[var(--border-color)] shadow-2xl overflow-hidden text-[var(--text-primary)]">
        {/* Glow Header */}
        <div className="relative p-6 bg-gradient-to-br from-[#7C5CFC]/20 via-[#32C5FF]/10 to-transparent border-b border-[var(--border-color)]">
          <button
            onClick={handleDismiss}
            className="absolute top-5 right-5 p-2 rounded-xl bg-[var(--bg-card)] hover:bg-[var(--bg-surface)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition cursor-pointer"
            title={t("update.dismiss")}
          >
            <X size={18} />
          </button>

          <div className="flex items-center gap-3 mb-2">
            <div className="p-2.5 rounded-2xl bg-gradient-to-tr from-[#7C5CFC] to-[#32C5FF] text-white shadow-lg shadow-[#7C5CFC]/30">
              <Sparkles size={22} />
            </div>
            <div>
              <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-[#35D49A]/15 text-[#35D49A] text-xs font-bold border border-[#35D49A]/30">
                <CheckCircle2 size={12} />
                {t("update.title")}
              </span>
              <h2 className="text-xl font-extrabold tracking-tight mt-1 text-[var(--text-primary)]">
                AetherLink v{updateInfo.version}
              </h2>
            </div>
          </div>

          <p className="text-xs text-[var(--text-secondary)] mt-1">
            {t("update.desc")}
          </p>

          {/* Version Comparison Bar */}
          <div className="mt-4 flex items-center gap-3 p-3 rounded-2xl bg-[var(--bg-card)]/80 border border-[var(--border-color)] text-xs">
            <div className="flex-1">
              <span className="text-[10px] text-[var(--text-secondary)] uppercase font-semibold">
                {t("update.current")}
              </span>
              <div className="font-mono font-bold text-[var(--text-secondary)]">
                v{currentVersion}
              </div>
            </div>
            <ArrowRight size={16} className="text-[#32C5FF]" />
            <div className="flex-1">
              <span className="text-[10px] text-[var(--text-secondary)] uppercase font-semibold">
                {t("update.latest")}
              </span>
              <div className="font-mono font-bold text-[#35D49A]">
                v{updateInfo.version}
              </div>
            </div>
          </div>
        </div>

        {/* Modal Body */}
        <div className="p-6 space-y-4 max-h-[45vh] overflow-y-auto">
          {/* Changelog Card */}
          <div className="space-y-2">
            <span className="text-xs font-bold uppercase tracking-wider text-[var(--text-secondary)]">
              {t("update.changelog")}
            </span>
            <div className="p-3.5 rounded-2xl bg-[var(--bg-card)] border border-[var(--border-color)] font-mono text-[11px] text-[var(--text-secondary)] whitespace-pre-line leading-relaxed max-h-40 overflow-y-auto">
              {changelogPreview}
            </div>
          </div>

          {/* Platform Direct Assets (if any) */}
          {updateInfo.assets && updateInfo.assets.length > 0 && (
            <div className="space-y-2">
              <span className="text-xs font-bold uppercase tracking-wider text-[var(--text-secondary)]">
                Direct Downloads:
              </span>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                {updateInfo.assets.map((asset) => (
                  <button
                    key={asset.name}
                    onClick={() => handleDownload(asset.downloadUrl)}
                    className="flex flex-col items-start p-2.5 rounded-xl bg-[var(--bg-card)] hover:bg-[var(--bg-surface)] border border-[var(--border-color)] hover:border-[#7C5CFC]/50 text-left transition cursor-pointer"
                  >
                    <div className="flex items-center gap-1.5 text-xs font-bold text-[var(--text-primary)] truncate w-full">
                      <Package size={13} className="text-[#32C5FF] shrink-0" />
                      <span className="truncate">{asset.name.replace(/^\d+-/, "")}</span>
                    </div>
                    <span className="text-[10px] text-[var(--text-secondary)] font-mono mt-1">
                      {(asset.size / (1024 * 1024)).toFixed(1)} MB
                    </span>
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="p-4 bg-[var(--bg-card)]/50 border-t border-[var(--border-color)] flex flex-col sm:flex-row items-center justify-between gap-3">
          <button
            onClick={handleDismiss}
            className="w-full sm:w-auto px-4 py-2 rounded-xl text-xs font-medium text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-[var(--bg-surface)] transition cursor-pointer"
          >
            {t("update.remind_later")}
          </button>

          <div className="flex items-center gap-2 w-full sm:w-auto">
            <button
              onClick={() => handleDownload(updateInfo.releaseUrl)}
              className="flex-1 sm:flex-none flex items-center justify-center gap-1.5 px-3.5 py-2 rounded-xl bg-[var(--bg-surface)] hover:bg-[var(--bg-card)] text-xs font-medium text-[var(--text-primary)] transition border border-[var(--border-color)] cursor-pointer"
            >
              <ExternalLink size={14} />
              <span>{t("update.view_release")}</span>
            </button>

            <button
              onClick={() => handleDownload()}
              className="flex-1 sm:flex-none flex items-center justify-center gap-2 px-5 py-2 rounded-xl bg-gradient-to-r from-[#7C5CFC] to-[#32C5FF] hover:opacity-90 text-white text-xs font-bold shadow-lg shadow-[#7C5CFC]/25 transition cursor-pointer"
            >
              <Download size={15} />
              <span>{t("update.download_now")}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
