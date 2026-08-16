export default function SkeletonRow({ cols = 5 }) {
    return (
        <tr className="animate-pulse">
            {Array.from({ length: cols }).map((_, i) => (
                <td key={i} className="px-5 py-3">
                    <div className="h-3 bg-gray-800 rounded w-3/4"/>
                </td>
            ))}
        </tr>
    )
}