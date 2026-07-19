interface Props {
  onDownload: () => void;
  onDelete: () => void;
  onRename: () => void;
}

export default function FileContextMenu({
  onDownload,
  onDelete,
  onRename,
}: Props) {
  return (
    <div className="flex gap-2 mt-2">
      <button
        onClick={onDownload}
        className="rounded bg-blue-600 px-3 py-1"
      >
        ⬇ Download
      </button>

      <button
        onClick={onDelete}
        className="rounded bg-red-600 px-3 py-1"
      >
        🗑 Delete
      </button>

      <button
        onClick={onRename}
        className="rounded bg-yellow-600 px-3 py-1"
      >
        ✏ Rename
      </button>
    </div>
  );
}