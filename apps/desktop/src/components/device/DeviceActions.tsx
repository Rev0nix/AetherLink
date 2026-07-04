interface Props {
  onBrowseFiles: () => void;
}

export default function DeviceActions({ onBrowseFiles }: Props) {
  return (
    <div className="grid gap-4">
      <button onClick={onBrowseFiles}>
        📂 Browse Files
      </button>

      <button disabled>📤 Send File</button>

      <button disabled>📥 Receive File</button>

      <button disabled>📋 Clipboard Sync</button>

      <button disabled>🔔 Notifications</button>

      <button disabled>🖥 Screen Mirror</button>

      <button disabled>⚙ Terminal</button>
    </div>
  );
}

