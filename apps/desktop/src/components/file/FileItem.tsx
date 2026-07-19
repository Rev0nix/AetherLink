import type { PhoneFile } from "../../types/file";

interface Props {
  file: PhoneFile;
  onOpen: (file: PhoneFile) => void;
}

export default function FileItem({ file, onOpen }: Props) {
  return (
    <button
      onClick={() => onOpen(file)}
      style={{
        display: "block",
        width: "100%",
        padding: "12px",
        marginBottom: "8px",
        textAlign: "left",
        
        background: "#222",
        color: "white",
      }}
    >
      {file.is_directory ? "📁" : "📄"} {file.name}
    </button>
  );
}