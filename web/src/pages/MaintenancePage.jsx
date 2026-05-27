import { useState, useEffect } from "react"
import API from "../api/axios"

const EMPTY_FORM = { equipmentId: "", doneOn: "", notes: "" }

export default function MaintenancePage() {
    const [logs,      setLogs]      = useState([])
    const [filtered,  setFiltered]  = useState([])
    const [equipment, setEquipment] = useState([])
    const [loading,   setLoading]   = useState(true)
    const [search,    setSearch]    = useState("")
    const [modal,     setModal]     = useState(false)
    const [form,      setForm]      = useState(EMPTY_FORM)
    const [saving,    setSaving]    = useState(false)
    const [error,     setError]     = useState("")

    useEffect(() => { fetchLogs(); fetchEquipment() }, [])

    useEffect(() => {
        const q = search.toLowerCase()
        setFiltered(logs.filter(l =>
            (l.equipment?.name || "").toLowerCase().includes(q) ||
            (l.notes || "").toLowerCase().includes(q) ||
            (l.doneBy || "").toLowerCase().includes(q)
        ))
    }, [search, logs])

    const fetchLogs = async () => {
        setLoading(true)
        try {
            const res = await API.get("/api/maintenance")
            setLogs(Array.isArray(res.data) ? res.data : [])
        } catch { setError("Failed to load maintenance logs.") }
        finally  { setLoading(false) }
    }

    const fetchEquipment = async () => {
        try {
            const res = await API.get("/api/equipment")
            setEquipment(Array.isArray(res.data) ? res.data : [])
        } catch {}
    }

    const openModal = () => {
        setForm({ ...EMPTY_FORM, doneOn: new Date().toISOString().split("T")[0] })
        setError(""); setModal(true)
    }

    const handleSave = async () => {
        if (!form.equipmentId) { setError("Please select equipment."); return }
        if (!form.doneOn)      { setError("Date is required."); return }
        setSaving(true); setError("")
        try {
            await API.post("/api/maintenance/log", {
                equipmentId: parseInt(form.equipmentId),
                doneOn:      form.doneOn,
                notes:       form.notes,
            })
            await fetchLogs()
            setModal(false)
            setForm(EMPTY_FORM)
        } catch (err) {
            setError(err.response?.data?.error || "Failed to save.")
        } finally { setSaving(false) }
    }

    return (
        <div className="space-y-5">

            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-2xl font-bold text-white">Maintenance Logs</h2>
                    <p className="text-sm text-gray-500 mt-0.5">{logs.length} total entries</p>
                </div>
                <button onClick={openModal}
                        className="bg-indigo-600 hover:bg-indigo-700 text-white text-sm
                     font-medium px-4 py-2 rounded-lg transition-colors">
                    + Log Maintenance
                </button>
            </div>

            <input type="text" placeholder="Search by equipment, notes or technician..."
                   value={search} onChange={e => setSearch(e.target.value)}
                   className="w-full bg-gray-900 border border-gray-700 text-white
                   rounded-lg px-4 py-2.5 text-sm placeholder-gray-600
                   focus:outline-none focus:border-indigo-500"/>

            {loading ? (
                <div className="text-gray-500 text-sm py-12 text-center">Loading...</div>
            ) : filtered.length === 0 ? (
                <div className="text-gray-500 text-sm py-12 text-center">No logs found.</div>
            ) : (
                <div className="bg-gray-900 rounded-xl border border-gray-800 overflow-hidden">
                    <table className="w-full text-sm">
                        <thead>
                        <tr className="border-b border-gray-800 text-gray-500 text-xs uppercase tracking-wide">
                            <th className="px-5 py-3 text-left">Equipment</th>
                            <th className="px-5 py-3 text-left">Date</th>
                            <th className="px-5 py-3 text-left">Notes</th>
                            <th className="px-5 py-3 text-left">Done By</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-800">
                        {filtered.map(l => (
                            <tr key={l.id} className="hover:bg-gray-800/50 transition-colors">
                                <td className="px-5 py-3 font-medium text-white">
                                    {l.equipment?.name || "—"}
                                </td>
                                <td className="px-5 py-3 text-gray-400">{l.doneOn}</td>
                                <td className="px-5 py-3 text-gray-400 max-w-xs truncate">
                                    {l.notes || "—"}
                                </td>
                                <td className="px-5 py-3">
                    <span className="text-indigo-300 text-xs font-medium">
                      {l.doneBy || "—"}
                    </span>
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
                        <h3 className="text-lg font-semibold text-white">Log Maintenance</h3>

                        <div className="space-y-3">
                            <div>
                                <label className="block text-xs text-gray-400 mb-1">Equipment *</label>
                                <select value={form.equipmentId}
                                        onChange={e => setForm({...form, equipmentId: e.target.value})}
                                        className="w-full bg-gray-800 border border-gray-700 text-white
                             rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-indigo-500">
                                    <option value="">— Select equipment —</option>
                                    {equipment.map(e => (
                                        <option key={e.id} value={e.id}>{e.name}</option>
                                    ))}
                                </select>
                            </div>

                            <div>
                                <label className="block text-xs text-gray-400 mb-1">Date *</label>
                                <input type="date" value={form.doneOn}
                                       onChange={e => setForm({...form, doneOn: e.target.value})}
                                       className="w-full bg-gray-800 border border-gray-700 text-white
                             rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-indigo-500"/>
                            </div>

                            <div>
                                <label className="block text-xs text-gray-400 mb-1">Notes</label>
                                <textarea value={form.notes}
                                          onChange={e => setForm({...form, notes: e.target.value})}
                                          rows={3} placeholder="What was done?"
                                          className="w-full bg-gray-800 border border-gray-700 text-white
                             rounded-lg px-3 py-2 text-sm focus:outline-none
                             focus:border-indigo-500 resize-none"/>
                            </div>
                        </div>

                        {error && <p className="text-red-400 text-sm">{error}</p>}

                        <div className="flex gap-3 pt-2">
                            <button onClick={handleSave} disabled={saving}
                                    className="flex-1 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50
                           text-white font-medium py-2 rounded-lg text-sm transition-colors">
                                {saving ? "Saving..." : "Log Maintenance"}
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