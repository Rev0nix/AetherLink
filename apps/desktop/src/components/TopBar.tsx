export default function TopBar() {
  return (
    <header className="h-20 border-b border-zinc-800 flex items-center justify-between px-8">
      <div>
        <h2 className="text-2xl font-bold">
          Dashboard
        </h2>

        <p className="text-zinc-400 text-sm">
          Manage all your Android devices
        </p>
      </div>

      <input
        placeholder="Search..."
        className="w-72 rounded-xl bg-zinc-900 border border-zinc-700 px-4 py-2 outline-none"
      />
    </header>
  );
}