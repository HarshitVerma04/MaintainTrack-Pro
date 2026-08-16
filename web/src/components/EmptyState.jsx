export default function EmptyState({ icon, title, message, action, onAction }) {
    return (
        <div className="flex flex-col items-center justify-center py-16 space-y-3">
            <p className="text-5xl">{icon || "📭"}</p>
            <p className="text-white font-semibold">{title}</p>
            {message && <p className="text-gray-500 text-sm text-center max-w-xs">{message}</p>}
            {action && onAction && (
                <button
                    onClick={onAction}
                    className="mt-2 bg-indigo-600 hover:bg-indigo-700 text-white
                     text-sm font-medium px-4 py-2 rounded-lg transition-colors"
                >
                    {action}
                </button>
            )}
        </div>
    )
}