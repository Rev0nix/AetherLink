interface Props {
    onRefresh: () => void;
    onUp: () => void;
}

export default function FileToolbar({
    onRefresh,
    onUp,
}: Props) {
    return (
        <div className="flex gap-3">
            <button
                className="rounded bg-blue-800 px-4 py-2"
                onClick={onRefresh}
            >
                Refresh
            </button>

            <button
                onClick={onUp}
                className="rounded bg-zinc-600 px-4 py-2"
            >
                ⬅ Up
            </button>
        </div>
    );
}