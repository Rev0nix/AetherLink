import {
  Folder,
  FileText,
  FileImage,
  FileVideo,
  FileAudio,
  FileArchive,
  FileCode,
  Download,
  Check,
} from "lucide-react";
import type { PhoneFile } from "../../types/file";
import { useState } from "react";

interface Props {
  file: PhoneFile;
  onOpen: (file: PhoneFile) => void;
  onDownload: (file: PhoneFile) => Promise<void>;
}

function formatBytes(bytes: number, decimals = 1) {
  if (bytes === 0) return "0 B";
  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ["B", "KB", "MB", "GB", "TB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + " " + sizes[i];
}

function getFileIcon(name: string, isDirectory: boolean) {
  if (isDirectory) return <Folder className="text-[#32C5FF]" size={18} />;

  const ext = name.split(".").pop()?.toLowerCase();
  switch (ext) {
    case "jpg":
    case "jpeg":
    case "png":
    case "gif":
    case "webp":
    case "svg":
      return <FileImage className="text-[#7C5CFC]" size={18} />;
    case "mp4":
    case "mkv":
    case "avi":
    case "mov":
      return <FileVideo className="text-[#FF5D73]" size={18} />;
    case "mp3":
    case "wav":
    case "ogg":
    case "flac":
      return <FileAudio className="text-[#FFB84D]" size={18} />;
    case "zip":
    case "rar":
    case "7z":
    case "tar":
    case "gz":
      return <FileArchive className="text-amber-400" size={18} />;
    case "js":
    case "ts":
    case "json":
    case "kt":
    case "py":
    case "rs":
      return <FileCode className="text-[#35D49A]" size={18} />;
    default:
      return <FileText className="text-[#929BB0]" size={18} />;
  }
}

export default function FileItem({ file, onOpen, onDownload }: Props) {
  const [downloading, setDownloading] = useState(false);
  const [downloaded, setDownloaded] = useState(false);

  const handleDownload = async (e: React.MouseEvent) => {
    e.stopPropagation();
    setDownloading(true);
    try {
      await onDownload(file);
      setDownloaded(true);
      setTimeout(() => setDownloaded(false), 2000);
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div
      onClick={() => onOpen(file)}
      className="flex items-center justify-between p-3.5 rounded-2xl bg-[#171D2B]/70 hover:bg-[#171D2B] border border-[#20283A] hover:border-[#7C5CFC]/40 transition-all cursor-pointer group shadow-sm"
    >
      <div className="flex items-center gap-3.5 min-w-0 flex-1">
        <div className="p-2 rounded-xl bg-[#090B14] border border-[#20283A] shrink-0">
          {getFileIcon(file.name, file.is_directory)}
        </div>

        <div className="min-w-0 flex-1">
          <div className="text-xs font-semibold text-white truncate group-hover:text-[#32C5FF] transition-colors">
            {file.name}
          </div>
          <div className="text-[11px] text-[#929BB0] font-mono mt-0.5">
            {file.is_directory ? "Folder" : formatBytes(file.size)}
          </div>
        </div>
      </div>

      {!file.is_directory && (
        <button
          onClick={handleDownload}
          disabled={downloading}
          className="p-2 rounded-xl bg-[#20283A] hover:bg-[#7C5CFC]/20 text-[#929BB0] hover:text-[#32C5FF] transition cursor-pointer shrink-0 ml-3"
          title="Download to Desktop"
        >
          {downloaded ? (
            <Check size={15} className="text-[#35D49A]" />
          ) : (
            <Download size={15} className={downloading ? "animate-bounce" : ""} />
          )}
        </button>
      )}
    </div>
  );
}
