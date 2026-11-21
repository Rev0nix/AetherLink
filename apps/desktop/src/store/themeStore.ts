import { create } from "zustand";

export type ThemeMode = "dark" | "light" | "oled" | "cyberpunk";

export interface ThemeOption {
  id: ThemeMode;
  nameKey: string;
  descKey: string;
  primaryColor: string;
  accentColor: string;
  badge: string;
}

export const THEME_OPTIONS: ThemeOption[] = [
  {
    id: "dark",
    nameKey: "appearance.dark_title",
    descKey: "appearance.dark_desc",
    primaryColor: "#090B14",
    accentColor: "#7C5CFC",
    badge: "Default",
  },
  {
    id: "light",
    nameKey: "appearance.light_title",
    descKey: "appearance.light_desc",
    primaryColor: "#F4F6FB",
    accentColor: "#0284C7",
    badge: "Light",
  },
  {
    id: "oled",
    nameKey: "appearance.oled_title",
    descKey: "appearance.oled_desc",
    primaryColor: "#000000",
    accentColor: "#38D9A9",
    badge: "AMOLED",
  },
  {
    id: "cyberpunk",
    nameKey: "appearance.cyberpunk_title",
    descKey: "appearance.cyberpunk_desc",
    primaryColor: "#080816",
    accentColor: "#FF2A85",
    badge: "Neon",
  },
];

interface ThemeState {
  theme: ThemeMode;
  setTheme: (theme: ThemeMode) => void;
}

export const useThemeStore = create<ThemeState>((set) => {
  const initialTheme = (localStorage.getItem("aetherlink_theme") as ThemeMode) || "dark";
  
  // Apply to document element on initialization
  if (typeof document !== "undefined") {
    document.documentElement.setAttribute("data-theme", initialTheme);
    if (initialTheme === "light") {
      document.documentElement.classList.add("light");
      document.documentElement.classList.remove("dark");
    } else {
      document.documentElement.classList.add("dark");
      document.documentElement.classList.remove("light");
    }
  }

  return {
    theme: initialTheme,
    setTheme: (theme: ThemeMode) => {
      localStorage.setItem("aetherlink_theme", theme);
      if (typeof document !== "undefined") {
        document.documentElement.setAttribute("data-theme", theme);
        if (theme === "light") {
          document.documentElement.classList.add("light");
          document.documentElement.classList.remove("dark");
        } else {
          document.documentElement.classList.add("dark");
          document.documentElement.classList.remove("light");
        }
      }
      set({ theme });
    },
  };
});
