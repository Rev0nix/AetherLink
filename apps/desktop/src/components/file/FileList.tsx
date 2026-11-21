import FileItem from "./FileItem";
import type { PhoneFile } from "../../types/file";
import { Folder } from "lucide-react";

interface Props {
  files: PhoneFile[];
  onOpen: (file: PhoneFile) => void;
  onDownload: (file: PhoneFile) => Promise<void>;
}

export default function FileList({ files, onOpen, onDownload }: Props) {
  if (files.length === 0) {
    return (
      <div className="p-12 text-center rounded-2xl bg-[#111522]/80 border border-[#20283A] space-y-2">
        <Folder size={32} className="mx-auto text-[#929BB0]/40" />
        <p className="text-xs text-[#929BB0]">This folder is empty</p>
      </div>
    );
  }

  // Sort directories first, then files alphabetically
  const sorted = [...files].sort((a, b) => {
    if (a.is_directory === b.is_directory) {
      return a.name.localeCompare(b.name);
    }
    return a.is_directory ? -1 : 1;
  });

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-2.5">
      {sorted.map((file) => (
        <FileItem
          key={file.path}
          file={file}
          onOpen={onOpen}
          onDownload={onDownload}
        />
      ))}
    </div>
  );
}
