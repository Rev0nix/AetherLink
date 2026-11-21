import { ArrowUp, RefreshCw, Upload, Search } from "lucide-react";

interface Props {
  onRefresh: () => void;
  onUp: () => void;
  isUpDisabled: boolean;
  onUpload: () => void;
  isUploading: boolean;
  search: string;
  onSearchChange: (val: string) => void;
}

export default function FileToolbar({
  onRefresh,
  onUp,
  isUpDisabled,
  onUpload,
  isUploading,
  search,
  onSearchChange,
}: Props) {
  return (
    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-4 rounded-2xl bg-[#111522]/90 border border-[#20283A]">
      <div className="flex items-center gap-2">
        <button
          onClick={onUp}
          disabled={isUpDisabled}
          className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-[#171D2B] hover:bg-[#20283A] text-zinc-300 disabled:opacity-40 text-xs font-semibold border border-[#20283A] transition cursor-pointer"
        >
          <ArrowUp size={15} />
          <span>Parent Folder</span>
        </button>

        <button
          onClick={onRefresh}
          className="p-2 rounded-xl bg-[#171D2B] hover:bg-[#20283A] text-zinc-300 hover:text-white border border-[#20283A] transition cursor-pointer"
          title="Refresh Directory"
        >
          <RefreshCw size={15} />
        </button>

        <button
          onClick={onUpload}
          disabled={isUploading}
          className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-gradient-to-r from-[#7C5CFC] to-[#32C5FF] hover:opacity-90 disabled:opacity-50 text-white text-xs font-bold transition shadow-md shadow-[#7C5CFC]/20 cursor-pointer"
        >
          <Upload size={14} />
          <span>{isUploading ? "Uploading..." : "Upload File"}</span>
        </button>
      </div>

      <div className="relative">
        <Search
          size={14}
          className="absolute left-3 top-1/2 -translate-y-1/2 text-[#929BB0]"
        />
        <input
          type="text"
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
          placeholder="Filter files..."
          className="pl-9 pr-3 py-1.5 rounded-xl bg-[#090B14] border border-[#20283A] text-xs text-white placeholder-[#929BB0]/60 outline-none focus:border-[#7C5CFC] w-full sm:w-56 transition"
        />
      </div>
    </div>
  );
}
