import { useState, useEffect } from "react"
import API from "../api/axios"
import toast from "react-hot-toast"
import EmptyState from "../components/EmptyState"
import SkeletonRow from "../components/SkeletonRow"

const STATUS_COLORS = {
    "Operational":       "bg-green-900 text-green-300",
    "Under Maintenance": "bg-yellow-900 text-yellow-300",
    "Out of Service":    "bg-red-900 text-red-300",
}

const STATUSES = ["Operational", "Under Maintenance", "Out of Service"]

function StatusBadge({ status }) {
    const today = new Date().toISOString().split("T")[0]
    return (
        <span className={`px-2 py-0.5 rounded-full text-xs font-medium
                      ${STATUS_COLORS[status] || "bg-gray-800 text-gray-400"}`}>
      {status}
    </span>
    )
}

function DueBadge({ date }) {
    if (!date) return <span className="text-gray-600">—</span>
    const today = new Date()
    const due   = new Date(date)
    const diff  = Math.ceil((due - today) / (1000 * 60 * 60 * 24))

    if (diff < 0)  return <span className="text-red-400 text-sm font-medium">{date} ⚠ Overdue</span>
    if (diff <= 7) return <span className="text-yellow-400 text-sm">{date} · {diff}d</span>
    return <span className="text-gray-400 text-sm">{date}</span>
}

const EMPTY_FORM = {
    name: "", location: "", status: "Operational",
    nextMaintenanceDate: "", intervalDays: 30
}

export default function EquipmentPage() {
    const [equipment, setEquipment] = useState([])
    const [filtered,  setFiltered]  = useState([])
    const [loading,   setLoading]   = useState(true)
    const [search,    setSearch]    = useState("")
    const [modal,     setModal]     = useState(false)
    const [editing,   setEditing]   = useState(null)
    const [form,      setForm]      = useState(EMPTY_FORM)
    const [saving,    setSaving]    = useState(false)
    const [error,     setError]     = useState("")
    const [deleteId,  setDeleteId]  = useState(null)

    useEffect(() => { fetchEquipment() }, [])

    useEffect(() => {
        const q = search.toLowerCase()
        setFiltered(equipment.filter(e =>
            e.name.toLowerCase().includes(q) ||
            (e.location || "").toLowerCase().includes(q) ||
            (e.status || "").toLowerCase().includes(q)
        ))
    }, [search, equipment])

    const fetchEquipment = async () => {
        setLoading(true)
        try {
            const res = await API.get("/api/equipment")
            const data = Array.isArray(res.data) ? res.data : []
            setEquipment(data)
            setFiltered(data)
        } catch {
            setError("Failed to load equipment.")
        } finally {
            setLoading(false)
        }
    }

    const openAdd = () => {
        setEditing(null)
        setForm(EMPTY_FORM)
        setError("")
        setModal(true)
    }

    const openEdit = (e) => {
        setEditing(e)
        setForm({
            name:                e.name || "",
            location:            e.location || "",
            status:              e.status || "Operational",
            nextMaintenanceDate: e.nextMaintenanceDate || "",
            intervalDays:        e.intervalDays || 30,
        })
        setError("")
        setModal(true)
    }

    const closeModal = () => {
        setModal(false)
        setEditing(null)
        setForm(EMPTY_FORM)
        setError("")
    }

    const handleSave = async () => {
        if (!form.name.trim()) { setError("Name is required."); return }
        if (!form.intervalDays || form.intervalDays <= 0) {
            setError("Interval days must be greater than 0."); return
        }
        setSaving(true)
        setError("")
        try {
            if (editing) {
                await API.put(`/api/equipment/${editing.id}`, form)
            } else {
                await API.post("/api/equipment", form)
            }
            await fetchEquipment()
            closeModal()
            toast.success(editing ? "Equipment updated." : "Equipment added.")
        } catch (err) {
            setError(err.response?.data?.message || "Save failed.")
        } finally {
            setSaving(false)
        }
    }

    const handleDelete = async () => {
        if (!deleteId) return
        try {
            await API.delete(`/api/equipment/${deleteId}`)
            setDeleteId(null)
            await fetchEquipment()
            toast.success("Equipment deleted.")
        } catch (err) {
            setError(err.response?.data?.message || "Delete failed.")
            setDeleteId(null)
            toast.error("Something went wrong.")
        }
    }

    const overdueCount = equipment.filter(e => {
        if (!e.nextMaintenanceDate) return false
        return new Date(e.nextMaintenanceDate) < new Date()
    }).length

    return (
        <div className="space-y-5">

            {/* Header */}
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-2xl font-bold text-white">Equipment</h2>
                    <p className="text-sm text-gray-500 mt-0.5">
                        {equipment.length} total
                        {overdueCount > 0 &&
                            <span className="ml-2 text-red-400">· {overdueCount} overdue</span>}
                    </p>
                </div>
                <button
                    onClick={openAdd}
                    className="bg-indigo-600 hover:bg-indigo-700 text-white text-sm
                     font-medium px-4 py-2 rounded-lg transition-colors"
                >
                    + Add Equipment
                </button>
            </div>

            {/* Search */}
            <input
                type="text"
                placeholder="Search by name, location or status..."
                value={search}
                onChange={e => setSearch(e.target.value)}
                className="w-full bg-gray-900 border border-gray-700 text-white
                   rounded-lg px-4 py-2.5 text-sm placeholder-gray-600
                   focus:outline-none focus:border-indigo-500"
            />

            {/* Table */}
            {loading ? (
                <div className="bg-gray-900 rounded-xl border border-gray-800 overflow-hidden">
                    <table className="w-full">
                        <tbody>
                        {Array.from({ length: 5 }).map((_, i) => <SkeletonRow key={i} cols={6} />)}
                        </tbody>
                    </table>
                </div>
            ) : filtered.length === 0 ? (
                <EmptyState
                    icon="⚙️"
                    title="No equipment found"
                    message="Add your first piece of equipment to get started."
                    action="Add Equipment"
                    onAction={openAdd}
                />
            ) : (
                <div className="bg-gray-900 rounded-xl border border-gray-800 overflow-hidden">
                    <table className="w-full text-sm">
                        <thead>
                        <tr className="border-b border-gray-800 text-gray-500 text-xs uppercase tracking-wide">
                            <th className="px-5 py-3 text-left">Name</th>
                            <th className="px-5 py-3 text-left">Location</th>
                            <th className="px-5 py-3 text-left">Status</th>
                            <th className="px-5 py-3 text-left">Next Due</th>
                            <th className="px-5 py-3 text-left">Interval</th>
                            <th className="px-5 py-3 text-left">Actions</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-800">
                        {filtered.map(e => (
                            <tr key={e.id} className="hover:bg-gray-800/50 transition-colors">
                                <td className="px-5 py-3 font-medium text-white">{e.name}</td>
                                <td className="px-5 py-3 text-gray-400">{e.location || "—"}</td>
                                <td className="px-5 py-3"><StatusBadge status={e.status} /></td>
                                <td className="px-5 py-3"><DueBadge date={e.nextMaintenanceDate} /></td>
                                <td className="px-5 py-3 text-gray-400">{e.intervalDays}d</td>
                                <td className="px-5 py-3">
                                    <div className="flex gap-2">
                                        <button
                                            onClick={() => openEdit(e)}
                                            className="text-indigo-400 hover:text-indigo-300 text-xs
                                   px-2 py-1 rounded hover:bg-gray-700 transition-colors"
                                        >
                                            Edit
                                        </button>
                                        <button
                                            onClick={() => setDeleteId(e.id)}
                                            className="text-red-400 hover:text-red-300 text-xs
                                   px-2 py-1 rounded hover:bg-gray-700 transition-colors"
                                        >
                                            Delete
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}

            {/* Add/Edit Modal */}
            {modal && (
                <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
                    <div className="bg-gray-900 rounded-2xl border border-gray-700
                          w-full max-w-lg p-6 space-y-4">
                        <h3 className="text-lg font-semibold text-white">
                            {editing ? "Edit Equipment" : "Add Equipment"}
                        </h3>

                        <div className="grid grid-cols-2 gap-4">
                            <div className="col-span-2">
                                <label className="block text-xs text-gray-400 mb-1">Name *</label>
                                <input
                                    value={form.name}
                                    onChange={e => setForm({...form, name: e.target.value})}
                                    placeholder="e.g. CNC Machine A1"
                                    className="w-full bg-gray-800 border border-gray-700 text-white
                             rounded-lg px-3 py-2 text-sm focus:outline-none
                             focus:border-indigo-500"
                                />
                            </div>

                            <div>
                                <label className="block text-xs text-gray-400 mb-1">Location</label>
                                <input
                                    value={form.location}
                                    onChange={e => setForm({...form, location: e.target.value})}
                                    placeholder="e.g. Shop Floor 1"
                                    className="w-full bg-gray-800 border border-gray-700 text-white
                             rounded-lg px-3 py-2 text-sm focus:outline-none
                             focus:border-indigo-500"
                                />
                            </div>

                            <div>
                                <label className="block text-xs text-gray-400 mb-1">Status</label>
                                <select
                                    value={form.status}
                                    onChange={e => setForm({...form, status: e.target.value})}
                                    className="w-full bg-gray-800 border border-gray-700 text-white
                             rounded-lg px-3 py-2 text-sm focus:outline-none
                             focus:border-indigo-500"
                                >
                                    {STATUSES.map(s => <option key={s}>{s}</option>)}
                                </select>
                            </div>

                            <div>
                                <label className="block text-xs text-gray-400 mb-1">Next Due Date</label>
                                <input
                                    type="date"
                                    value={form.nextMaintenanceDate}
                                    onChange={e => setForm({...form, nextMaintenanceDate: e.target.value})}
                                    className="w-full bg-gray-800 border border-gray-700 text-white
                             rounded-lg px-3 py-2 text-sm focus:outline-none
                             focus:border-indigo-500"
                                />
                            </div>

                            <div>
                                <label className="block text-xs text-gray-400 mb-1">Interval (days) *</label>
                                <input
                                    type="number"
                                    value={form.intervalDays}
                                    onChange={e => setForm({...form, intervalDays: parseInt(e.target.value)})}
                                    className="w-full bg-gray-800 border border-gray-700 text-white
                             rounded-lg px-3 py-2 text-sm focus:outline-none
                             focus:border-indigo-500"
                                />
                            </div>
                        </div>

                        {error && <p className="text-red-400 text-sm">{error}</p>}

                        <div className="flex gap-3 pt-2">
                            <button
                                onClick={handleSave}
                                disabled={saving}
                                className="flex-1 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50
                           text-white font-medium py-2 rounded-lg text-sm transition-colors"
                            >
                                {saving ? "Saving..." : editing ? "Save Changes" : "Add Equipment"}
                            </button>
                            <button
                                onClick={closeModal}
                                className="flex-1 bg-gray-800 hover:bg-gray-700 text-gray-300
                           font-medium py-2 rounded-lg text-sm transition-colors"
                            >
                                Cancel
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Delete Confirmation */}
            {deleteId && (
                <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
                    <div className="bg-gray-900 rounded-2xl border border-gray-700 p-6 max-w-sm w-full">
                        <h3 className="text-lg font-semibold text-white mb-2">Delete Equipment?</h3>
                        <p className="text-gray-400 text-sm mb-5">
                            This will permanently delete this equipment and all associated records.
                            This cannot be undone.
                        </p>
                        <div className="flex gap-3">
                            <button
                                onClick={handleDelete}
                                className="flex-1 bg-red-600 hover:bg-red-700 text-white
                           font-medium py-2 rounded-lg text-sm transition-colors"
                            >
                                Delete
                            </button>
                            <button
                                onClick={() => setDeleteId(null)}
                                className="flex-1 bg-gray-800 hover:bg-gray-700 text-gray-300
                           font-medium py-2 rounded-lg text-sm transition-colors"
                            >
                                Cancel
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}