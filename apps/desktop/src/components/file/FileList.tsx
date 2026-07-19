import FileItem from "./FileItem";
import type { PhoneFile } from "../../types/file";

interface Props {
  files: PhoneFile[];
  onOpen: (file: PhoneFile) => void;
}

export default function FileList({
  files,
  onOpen,
}: Props) {
  return (
    <div
      className="space-y-2 gray-500 p-4"
      onClick={() => alert("FileList clicked")}
    >
      {files.map((file) => (
        <FileItem
          key={file.name}
          file={file}
          onOpen={onOpen}
        />
      ))}
    </div>
  );
}