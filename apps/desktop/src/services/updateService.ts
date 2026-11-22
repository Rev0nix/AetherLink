import { create } from "zustand";
import { getAppInfo } from "./tauri";

export interface ReleaseAsset {
  name: string;
  size: number;
  downloadUrl: string;
}

export interface UpdateInfo {
  version: string;
  title: string;
  changelog: string;
  releaseUrl: string;
  publishedAt: string;
  assets: ReleaseAsset[];
}

export interface UpdateState {
  currentVersion: string;
  latestVersion: string | null;
  isUpdateAvailable: boolean;
  isChecking: boolean;
  updateInfo: UpdateInfo | null;
  isModalOpen: boolean;
  statusMessage: string | null;
  checkUpdates: (manual?: boolean) => Promise<boolean>;
  openModal: () => void;
  closeModal: () => void;
}

function parseSemver(v: string): number[] {
  return v
    .replace(/^v/i, "")
    .split(/[-+]/)[0]
    .split(".")
    .map((num) => parseInt(num, 10) || 0);
}

function isNewerVersion(latest: string, current: string): boolean {
  const l = parseSemver(latest);
  const c = parseSemver(current);

  for (let i = 0; i < Math.max(l.length, c.length); i++) {
    const lPart = l[i] ?? 0;
    const cPart = c[i] ?? 0;
    if (lPart > cPart) return true;
    if (lPart < cPart) return false;
  }
  return false;
}

export const useUpdateStore = create<UpdateState>((set, get) => ({
  currentVersion: "2.3.3",
  latestVersion: null,
  isUpdateAvailable: false,
  isChecking: false,
  updateInfo: null,
  isModalOpen: false,
  statusMessage: null,

  openModal: () => set({ isModalOpen: true }),
  closeModal: () => set({ isModalOpen: false }),

  checkUpdates: async (manual = false) => {
    set({ isChecking: true, statusMessage: null });
    try {
      const appInfo = await getAppInfo();
      const currentVersion = appInfo?.version || "2.2.2";
      set({ currentVersion });

      const resp = await fetch(
        "https://api.github.com/repos/Rev0nix/AetherLink/releases/latest",
        {
          headers: {
            Accept: "application/vnd.github.v3+json",
          },
        }
      );

      if (!resp.ok) {
        throw new Error(`GitHub API returned status ${resp.status}`);
      }

      const data = await resp.json();
      const latestTag = data.tag_name || "";
      const latestVersion = latestTag.replace(/^v/i, "");

      const updateAvailable = isNewerVersion(latestVersion, currentVersion);

      const assets: ReleaseAsset[] = (data.assets || []).map((a: any) => ({
        name: a.name,
        size: a.size,
        downloadUrl: a.browser_download_url,
      }));

      const updateInfo: UpdateInfo = {
        version: latestVersion,
        title: data.name || `VEXYRON v${latestVersion}`,
        changelog: data.body || "No changelog provided for this release.",
        releaseUrl: data.html_url || "https://github.com/Rev0nix/AetherLink/releases",
        publishedAt: data.published_at || new Date().toISOString(),
        assets,
      };

      set({
        latestVersion,
        isUpdateAvailable: updateAvailable,
        updateInfo,
        isChecking: false,
        statusMessage: updateAvailable
          ? `v${latestVersion} available`
          : "Up to date",
      });

      // If an update is available and it wasn't dismissed in this session
      const dismissed = sessionStorage.getItem("aetherlink_dismissed_update");
      if (updateAvailable && (manual || dismissed !== latestVersion)) {
        set({ isModalOpen: true });
      }

      return updateAvailable;
    } catch (err) {
      console.warn("[AetherLink Update Checker]", err);
      set({
        isChecking: false,
        statusMessage: "Check failed",
      });
      return false;
    }
  },
}));
