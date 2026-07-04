import {
  LayoutDashboard,
  Smartphone,
  Clipboard,
  Bell,
  Settings,
  Cpu,
} from "lucide-react";
import { NavLink } from "react-router-dom";

const menu = [
  {
    name: "Dashboard",
    icon: LayoutDashboard,
    path: "/",
  },
  {
    name: "Devices",
    icon: Smartphone,
    path: "/devices",
  },
  {
    name: "Clipboard",
    icon: Clipboard,
    path: "/clipboard",
  },
  {
    name: "Notifications",
    icon: Bell,
    path: "/notifications",
  },
  {
    name: "Settings",
    icon: Settings,
    path: "/settings",
  },
];

export default function Sidebar() {
  return (
    <aside className="w-72 bg-zinc-900 border-r border-zinc-800 flex flex-col">
      <div className="p-8 border-b border-zinc-800">
        <h1 className="text-3xl font-bold tracking-tight">
          AetherLink
        </h1>

        <p className="text-sm text-zinc-400 mt-2">
          AI Android Companion
        </p>
      </div>

      <nav className="flex-1 p-4 space-y-2">
        {menu.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-xl px-4 py-3 transition-all ${
                isActive
                  ? "bg-blue-600 text-white"
                  : "hover:bg-zinc-800 text-zinc-300"
              }`
            }
          >
            <item.icon size={20} />
            {item.name}
          </NavLink>
        ))}
      </nav>

      <div className="p-5 border-t border-zinc-800">
        <div className="flex items-center gap-3">
          <Cpu className="text-green-500" />

          <div>
            <p className="font-semibold">
              Core Engine
            </p>

            <p className="text-xs text-zinc-400">
              Rust Backend Running
            </p>
          </div>
        </div>
      </div>
    </aside>
  );
}