import { useNavigate } from "react-router-dom";
import {
  FolderSync,
  Clipboard,
  Bell,
  HardDrive,
  Activity,
  ArrowRight,
} from "lucide-react";

interface Props {
  onBrowseFiles: () => void;
}

export default function DeviceActions({ onBrowseFiles }: Props) {
  const navigate = useNavigate();

  return (
    <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
      <button
        onClick={onBrowseFiles}
        className="flex items-center justify-between p-4 rounded-2xl bg-[#171D2B]/90 hover:bg-[#171D2B] border border-[#20283A] hover:border-[#7C5CFC]/40 transition-all text-left cursor-pointer group shadow-sm"
      >
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-[#7C5CFC]/15 text-[#7C5CFC]">
            <FolderSync size={20} />
          </div>
          <div>
            <div className="text-xs font-bold text-white group-hover:text-[#32C5FF] transition-colors">
              Browse Files
            </div>
            <div className="text-[11px] text-[#929BB0]">
              Explore internal storage
            </div>
          </div>
        </div>
        <ArrowRight size={14} className="text-[#929BB0] group-hover:text-white group-hover:translate-x-1 transition-all" />
      </button>

      <button
        onClick={() => navigate("/clipboard")}
        className="flex items-center justify-between p-4 rounded-2xl bg-[#171D2B]/90 hover:bg-[#171D2B] border border-[#20283A] hover:border-[#32C5FF]/40 transition-all text-left cursor-pointer group shadow-sm"
      >
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-[#32C5FF]/15 text-[#32C5FF]">
            <Clipboard size={20} />
          </div>
          <div>
            <div className="text-xs font-bold text-white group-hover:text-[#32C5FF] transition-colors">
              Clipboard Sync
            </div>
            <div className="text-[11px] text-[#929BB0]">
              Push & pull shared text
            </div>
          </div>
        </div>
        <ArrowRight size={14} className="text-[#929BB0] group-hover:text-white group-hover:translate-x-1 transition-all" />
      </button>

      <button
        onClick={() => navigate("/notifications")}
        className="flex items-center justify-between p-4 rounded-2xl bg-[#171D2B]/90 hover:bg-[#171D2B] border border-[#20283A] hover:border-[#FFB84D]/40 transition-all text-left cursor-pointer group shadow-sm"
      >
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-[#FFB84D]/15 text-[#FFB84D]">
            <Bell size={20} />
          </div>
          <div>
            <div className="text-xs font-bold text-white group-hover:text-[#32C5FF] transition-colors">
              Notifications
            </div>
            <div className="text-[11px] text-[#929BB0]">
              View mirrored alerts
            </div>
          </div>
        </div>
        <ArrowRight size={14} className="text-[#929BB0] group-hover:text-white group-hover:translate-x-1 transition-all" />
      </button>
    </div>
  );
}
