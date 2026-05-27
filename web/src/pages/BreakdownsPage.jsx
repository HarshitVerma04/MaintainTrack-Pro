import { useState, useEffect } from "react"
import API from "../api/axios"

const EMPTY_FORM = { equipmentId: "", occurredOn: "", description: "" }

export default function BreakdownsPage() {
    const [logs,      setLogs]      = useState([])
    const [filtered,  setFiltered]  = useState([])
    const [equipment, setEquipment] = useState([])
    const [loading,   setLoading]   = useState(true)
    const [search,    setSearch]    = useState("")
    const [modal,     setModal]     = useState(false)
    const [form,      setForm]      = useState(EMPTY_FORM)
    const [saving,    setSaving]    = useState(false)
    const [error,     setError]     = useState("")
    const [resolving, setResolving] = useState(null)

    useEffect(() => { fetchLogs(); fetchEquipment() }, [])

    useEffect(() => {
        const q = search.toLowerCase()
        setFiltered(logs.filter(l =>
            (l.equipment?.name || "").toLowerCase().includes(q) ||
            (l.description || "").toLowerCase().includes(q) ||
            (l.resolvedBy || "").toLowerCase().includes(q)
        ))
    }, [search, logs])

    const fetchLogs = async () => {
        setLoading(true)
        try {
            const res = await API.get("/api/breakdowns")
            setLogs(Array.isArray(res.data) ? res.data : [])
        } catch { setError("Failed to load breakdown logs.") }
        finally  { setLoading(false) }
    }

    const fetchEquipment = async () => {
        try {
            const res = await API.get("/api/equipment")
            setEquipment(Array.isArray(res.data) ? res.data : [])
        } catch {}
    }

    const openModal = () => {
        setForm({ ...EMPTY_FORM, occurredOn: new Date().toISOString().split("T")[0] })
        setError(""); setModal(true)
    }

    const handleSave = async () => {
        if (!form.equipmentId)  { setError("Please select equipment."); return }
        if (!form.occurredOn)   { setError("Date is required."); return }
        if (!form.description)  { setError("Description is required."); return }
        setSaving(true); setError("")
        try {
            await API.post("/api/breakdowns", {
                equipmentId: parseInt(form.equipmentId),
                occurredOn:  form.occurredOn,
                description: form.description,
            })
            await fetchLogs()
            setModal(false)
            setForm(EMPTY_FORM)
        } catch (err) {
            setError(err.response?.data?.error || "Failed to save.")
        } finally { setSaving(false) }
    }

    const handleResolve = async (id) => {
        setResolving(id)
        try {
            await API.put(`/api/breakdowns/${id}/resolve`, { resolvedBy: "web-user" })
            await fetchLogs()
        } catch {}
        finally { setResolving(null) }
    }

    const unresolvedCount = logs.filter(l => !l.resolvedBy).length

    return (
        <div className="space-y-5">

            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-2xl font-bold text-white">Breakdown Logs</h2>
                    <p className="text-sm text-gray-500 mt-0.5">
                        {logs.length} total
                        {unresolvedCount > 0 &&
                            <span className="ml-2 text-red-400">· {unresolvedCount} unresolved</span>}
                    </p>
                </div>
                <button onClick={openModal}
                        className="bg-red-600 hover:bg-red-700 text-white text-sm
                     font-medium px-4 py-2 rounded-lg transition-colors">
                    + Log Breakdown
                </button>
            </div>

            <input type="text" placeholder="Search by equipment, description or resolved by..."
                   value={search} onChange={e => setSearch(e.target.value)}
                   className="w-full bg-gray-900 border border-gray-700 text-white
                   rounded-lg px-4 py-2.5 text-sm placeholder-gray-600
                   focus:outline-none focus:border-indigo-500"/>

            {loading ? (
                <div className="text-gray-500 text-sm py-12 text-center">Loading...</div>
            ) : filtered.length === 0 ? (
                <div className="text-gray-500 text-sm py-12 text-center">No breakdowns found.</div>
            ) : (
                <div className="bg-gray-900 rounded-xl border border-gray-800 overflow-hidden">
                    <table className="w-full text-sm">
                        <thead>
                        <tr className="border-b border-gray-800 text-gray-500 text-xs uppercase tracking-wide">
                            <th className="px-5 py-3 text-left">Equipment</th>
                            <th className="px-5 py-3 text-left">Date</th>
                            <th className="px-5 py-3 text-left">Description</th>
                            <th className="px-5 py-3 text-left">Status</th>
                            <th className="px-5 py-3 text-left">Action</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-800">
                        {filtered.map(l => (
                            <tr key={l.id}
                                className={`hover:bg-gray-800/50 transition-colors
                    ${!l.resolvedBy ? "bg-red-950/20" : ""}`}>
                                <td className="px-5 py-3 font-medium text-white">
                                    {l.equipment?.name || "—"}
                                </td>
                                <td className="px-5 py-3 text-gray-400">{l.occurredOn}</td>
                                <td className="px-5 py-3 text-gray-400 max-w-xs truncate">
                                    {l.description || "—"}
                                </td>
                                <td className="px-5 py-3">
                                    {l.resolvedBy ? (
                                        <span className="px-2 py-0.5 rounded-full text-xs
                                       bg-green-900 text-green-300">
                        Resolved · {l.resolvedBy}
                      </span>
                                    ) : (
                                        <span className="px-2 py-0.5 rounded-full text-xs
                                       bg-red-900 text-red-300">
                        Unresolved
                      </span>
                                    )}
                                </td>
                                <td className="px-5 py-3">
                                    {!l.resolvedBy && (
                                        <button
                                            onClick={() => handleResolve(l.id)}
                                            disabled={resolving === l.id}
                                            className="text-green-400 hover:text-green-300 text-xs
                                   px-2 py-1 rounded hover:bg-gray-700
                                   transition-colors disabled:opacity-50">
                                            {resolving === l.id ? "..." : "Resolve"}
                                        </button>
                                    )}
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}

            {/* Log Modal */}
            {modal && (
                <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
                    <div className="bg-gray-900 rounded-2xl border border-gray-700
                          w-full max-w-md p-6 space-y-4">
                        <h3 className="text-lg font-semibold text-white">Log Breakdown</h3>

                        <div className="space-y-3">
                            <div>
                                <label className="block text-xs text-gray-400 mb-1">Equipment *</label>
                                <select value={form.equipmentId}
                                        onChange={e => setForm({...form, equipmentId: e.target.value})}
                                        className="w-full bg-gray-800 border border-gray-700 text-white
                             rounded-lg px-3 py-2 text-sm focus:outline-none
                             focus:border-indigo-500">
                                    <option value="">— Select equipment —</option>
                                    {equipment.map(e => (
                                        <option key={e.id} value={e.id}>{e.name}</option>
                                    ))}
                                </select>
                            </div>

                            <div>
                                <label className="block text-xs text-gray-400 mb-1">Occurred On *</label>
                                <input type="date" value={form.occurredOn}
                                       onChange={e => setForm({...form, occurredOn: e.target.value})}
                                       className="w-full bg-gray-800 border border-gray-700 text-white
                             rounded-lg px-3 py-2 text-sm focus:outline-none
                             focus:border-indigo-500"/>
                            </div>

                            <div>
                                <label className="block text-xs text-gray-400 mb-1">Description *</label>
                                <textarea value={form.description}
                                          onChange={e => setForm({...form, description: e.target.value})}
                                          rows={3} placeholder="What happened?"
                                          className="w-full bg-gray-800 border border-gray-700 text-white
                             rounded-lg px-3 py-2 text-sm focus:outline-none
                             focus:border-indigo-500 resize-none"/>
                            </div>
                        </div>

                        {error && <p className="text-red-400 text-sm">{error}</p>}

                        <div className="flex gap-3 pt-2">
                            <button onClick={handleSave} disabled={saving}
                                    className="flex-1 bg-red-600 hover:bg-red-700 disabled:opacity-50
                           text-white font-medium py-2 rounded-lg text-sm transition-colors">
                                {saving ? "Saving..." : "Log Breakdown"}
                            </button>
                            <button onClick={() => setModal(false)}
                                    className="flex-1 bg-gray-800 hover:bg-gray-700 text-gray-300
                           font-medium py-2 rounded-lg text-sm transition-colors">
                                Cancel
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}