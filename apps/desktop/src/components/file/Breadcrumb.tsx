interface Props {
  path: string;
}

export default function Breadcrumb({
  path,
}: Props) {
  return (
    <div className="rounded bg-zinc-900 p-3 text-zinc-00">
      📂 {path}
    </div>
  );
}