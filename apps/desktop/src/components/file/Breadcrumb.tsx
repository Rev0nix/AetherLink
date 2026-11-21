import { Folder, ChevronRight, Home } from "lucide-react";

interface Props {
  path: string;
  onNavigate: (path: string) => void;
}

export default function Breadcrumb({ path, onNavigate }: Props) {
  const segments = path.split("/").filter(Boolean);

  return (
    <div className="flex items-center gap-1.5 p-3 rounded-xl bg-[#171D2B]/80 border border-[#20283A] text-xs overflow-x-auto">
      <button
        onClick={() => onNavigate("/sdcard")}
        className="flex items-center gap-1.5 text-[#32C5FF] hover:underline font-semibold cursor-pointer shrink-0"
      >
        <Home size={14} />
        <span>Internal Storage</span>
      </button>

      {segments.map((seg, idx) => {
        const fullPath = "/" + segments.slice(0, idx + 1).join("/");
        const isLast = idx === segments.length - 1;

        // Skip rendering redundant root if path starts with sdcard
        if (seg === "sdcard" && idx === 0) return null;

        return (
          <div key={fullPath} className="flex items-center gap-1.5 shrink-0">
            <ChevronRight size={13} className="text-[#929BB0]/60" />
            <button
              onClick={() => onNavigate(fullPath)}
              disabled={isLast}
              className={`font-mono transition cursor-pointer ${
                isLast
                  ? "text-white font-bold cursor-default"
                  : "text-[#929BB0] hover:text-[#32C5FF]"
              }`}
            >
              {seg}
            </button>
          </div>
        );
      })}
    </div>
  );
}
