import { useState, useEffect } from "react"
import API from "../api/axios"

const EMPTY_FORM = {
    name: "", qtyOnHand: 0, minQty: 5,
    unit: "pcs", unitCost: 0, supplierId: ""
}

function StockBadge({ qty, min }) {
    if (qty <= 0)    return <span className="px-2 py-0.5 rounded-full text-xs bg-red-900 text-red-300">Out of Stock</span>
    if (qty <= min)  return <span className="px-2 py-0.5 rounded-full text-xs bg-yellow-900 text-yellow-300">Low Stock</span>
    return <span className="px-2 py-0.5 rounded-full text-xs bg-green-900 text-green-300">In Stock</span>
}

export default function PartsPage() {
    const [parts,     setParts]     = useState([])
    const [filtered,  setFiltered]  = useState([])
    const [suppliers, setSuppliers] = useState([])
    const [loading,   setLoading]   = useState(true)
    const [search,    setSearch]    = useState("")
    const [filter,    setFilter]    = useState("all")
    const [modal,     setModal]     = useState(false)
    const [editing,   setEditing]   = useState(null)
    const [form,      setForm]      = useState(EMPTY_FORM)
    const [saving,    setSaving]    = useState(false)
    const [error,     setError]     = useState("")
    const [deleteId,  setDeleteId]  = useState(null)

    useEffect(() => {
        fetchParts()
        fetchSuppliers()
    }, [])

    useEffect(() => {
        let result = parts
        if (filter === "low")  result = result.filter(p => p.qtyOnHand <= p.minQty && p.qtyOnHand > 0)
        if (filter === "out")  result = result.filter(p => p.qtyOnHand <= 0)
        const q = search.toLowerCase()
        if (q) result = result.filter(p =>
            p.name.toLowerCase().includes(q) ||
            (p.supplier?.name || "").toLowerCase().includes(q)
        )
        setFiltered(result)
    }, [search, filter, parts])

    const fetchParts = async () => {
        setLoading(true)
        try {
            const res = await API.get("/api/parts")
            setParts(Array.isArray(res.data) ? res.data : [])
        } catch { setError("Failed to load parts.") }
        finally  { setLoading(false) }
    }

    const fetchSuppliers = async () => {
        try {
            const res = await API.get("/api/suppliers")
            setSuppliers(Array.isArray(res.data) ? res.data : [])
        } catch {}
    }

    const openAdd = () => {
        setEditing(null)
        setForm(EMPTY_FORM)
        setError("")
        setModal(true)
    }

    const openEdit = (p) => {
        setEditing(p)
        setForm({
            name:       p.name,
            qtyOnHand:  p.qtyOnHand,
            minQty:     p.minQty,
            unit:       p.unit,
            unitCost:   p.unitCost,
            supplierId: p.supplier?.id || ""
        })
        setError("")
        setModal(true)
    }

    const closeModal = () => {
        setModal(false); setEditing(null)
        setForm(EMPTY_FORM); setError("")
    }

    const handleSave = async () => {
        if (!form.name.trim()) { setError("Name is required."); return }
        setSaving(true); setError("")
        try {
            const suppParam = form.supplierId ? `?supplierId=${form.supplierId}` : ""
            if (editing) {
                await API.put(`/api/parts/${editing.id}${suppParam}`, form)
            } else {
                await API.post(`/api/parts${suppParam}`, form)
            }
            await fetchParts()
            closeModal()
        } catch (err) {
            setError(err.response?.data?.message || "Save failed.")
        } finally { setSaving(false) }
    }

    const handleDelete = async () => {
        try {
            await API.delete(`/api/parts/${deleteId}`)
            setDeleteId(null)
            await fetchParts()
        } catch { setDeleteId(null) }
    }

    const lowCount = parts.filter(p => p.qtyOnHand <= p.minQty).length

    return (
        <div className="space-y-5">

            {/* Header */}
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-2xl font-bold text-white">Parts</h2>
                    <p className="text-sm text-gray-500 mt-0.5">
                        {parts.length} total
                        {lowCount > 0 && <span className="ml-2 text-yellow-400">· {lowCount} low stock</span>}
                    </p>
                </div>
                <button onClick={openAdd}
                        className="bg-indigo-600 hover:bg-indigo-700 text-white text-sm
                     font-medium px-4 py-2 rounded-lg transition-colors">
                    + Add Part
                </button>
            </div>

            {/* Filters */}
            <div className="flex gap-3 flex-wrap">
                <input
                    type="text" placeholder="Search parts or supplier..."
                    value={search} onChange={e => setSearch(e.target.value)}
                    className="flex-1 min-w-48 bg-gray-900 border border-gray-700 text-white
                     rounded-lg px-4 py-2 text-sm placeholder-gray-600
                     focus:outline-none focus:border-indigo-500"
                />
                {["all","low","out"].map(f => (
                    <button key={f} onClick={() => setFilter(f)}
                            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors
              ${filter === f
                                ? "bg-indigo-600 text-white"
                                : "bg-gray-900 border border-gray-700 text-gray-400 hover:text-white"}`}>
                        {f === "all" ? "All" : f === "low" ? "Low Stock" : "Out of Stock"}
                    </button>
                ))}
            </div>

            {/* Table */}
            {loading ? (
                <div className="text-gray-500 text-sm py-12 text-center">Loading...</div>
            ) : filtered.length === 0 ? (
                <div className="text-gray-500 text-sm py-12 text-center">No parts found.</div>
            ) : (
                <div className="bg-gray-900 rounded-xl border border-gray-800 overflow-hidden">
                    <table className="w-full text-sm">
                        <thead>
                        <tr className="border-b border-gray-800 text-gray-500 text-xs uppercase tracking-wide">
                            <th className="px-5 py-3 text-left">Name</th>
                            <th className="px-5 py-3 text-left">Supplier</th>
                            <th className="px-5 py-3 text-left">Stock</th>
                            <th className="px-5 py-3 text-left">Min Qty</th>
                            <th className="px-5 py-3 text-left">Unit</th>
                            <th className="px-5 py-3 text-left">Unit Cost</th>
                            <th className="px-5 py-3 text-left">Actions</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-800">
                        {filtered.map(p => (
                            <tr key={p.id}
                                className={`hover:bg-gray-800/50 transition-colors
                    ${p.qtyOnHand <= p.minQty ? "bg-yellow-950/20" : ""}`}>
                                <td className="px-5 py-3 font-medium text-white">{p.name}</td>
                                <td className="px-5 py-3 text-gray-400">
                                    {p.supplier?.name || "—"}
                                </td>
                                <td className="px-5 py-3">
                                    <div className="flex items-center gap-2">
                                        <span className="text-white font-medium">{p.qtyOnHand}</span>
                                        <StockBadge qty={p.qtyOnHand} min={p.minQty} />
                                    </div>
                                </td>
                                <td className="px-5 py-3 text-gray-400">{p.minQty}</td>
                                <td className="px-5 py-3 text-gray-400">{p.unit}</td>
                                <td className="px-5 py-3 text-gray-400">
                                    ₹{Number(p.unitCost).toLocaleString("en-IN")}
                                </td>
                                <td className="px-5 py-3">
                                    <div className="flex gap-2">
                                        <button onClick={() => openEdit(p)}
                                                className="text-indigo-400 hover:text-indigo-300 text-xs
                                   px-2 py-1 rounded hover:bg-gray-700 transition-colors">
                                            Edit
                                        </button>
                                        <button onClick={() => setDeleteId(p.id)}
                                                className="text-red-400 hover:text-red-300 text-xs
                                   px-2 py-1 rounded hover:bg-gray-700 transition-colors">
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
                            {editing ? "Edit Part" : "Add Part"}
                        </h3>

                        <div className="grid grid-cols-2 gap-4">
                            <div className="col-span-2">
                                <label className="block text-xs text-gray-400 mb-1">Name *</label>
                                <input value={form.name}
                                       onChange={e => setForm({...form, name: e.target.value})}
                                       placeholder="e.g. Hydraulic Oil Filter"
                                       className="w-full bg-gray-800 border border-gray-700 text-white
                             rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-indigo-500"/>
                            </div>

                            <div>
                                <label className="block text-xs text-gray-400 mb-1">Qty on Hand</label>
                                <input type="number" value={form.qtyOnHand}
                                       onChange={e => setForm({...form, qtyOnHand: parseInt(e.target.value)||0})}
                                       className="w-full bg-gray-800 border border-gray-700 text-white
                             rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-indigo-500"/>
                            </div>

                            <div>
                                <label className="block text-xs text-gray-400 mb-1">Min Qty</label>
                                <input type="number" value={form.minQty}
                                       onChange={e => setForm({...form, minQty: parseInt(e.target.value)||0})}
                                       className="w-full bg-gray-800 border border-gray-700 text-white
                             rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-indigo-500"/>
                            </div>

                            <div>
                                <label className="block text-xs text-gray-400 mb-1">Unit</label>
                                <input value={form.unit}
                                       onChange={e => setForm({...form, unit: e.target.value})}
                                       placeholder="pcs, kg, can..."
                                       className="w-full bg-gray-800 border border-gray-700 text-white
                             rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-indigo-500"/>
                            </div>

                            <div>
                                <label className="block text-xs text-gray-400 mb-1">Unit Cost (₹)</label>
                                <input type="number" value={form.unitCost}
                                       onChange={e => setForm({...form, unitCost: parseFloat(e.target.value)||0})}
                                       className="w-full bg-gray-800 border border-gray-700 text-white
                             rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-indigo-500"/>
                            </div>

                            <div className="col-span-2">
                                <label className="block text-xs text-gray-400 mb-1">Supplier</label>
                                <select value={form.supplierId}
                                        onChange={e => setForm({...form, supplierId: e.target.value})}
                                        className="w-full bg-gray-800 border border-gray-700 text-white
                             rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-indigo-500">
                                    <option value="">— No supplier —</option>
                                    {suppliers.map(s => (
                                        <option key={s.id} value={s.id}>{s.name}</option>
                                    ))}
                                </select>
                            </div>
                        </div>

                        {error && <p className="text-red-400 text-sm">{error}</p>}

                        <div className="flex gap-3 pt-2">
                            <button onClick={handleSave} disabled={saving}
                                    className="flex-1 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50
                           text-white font-medium py-2 rounded-lg text-sm transition-colors">
                                {saving ? "Saving..." : editing ? "Save Changes" : "Add Part"}
                            </button>
                            <button onClick={closeModal}
                                    className="flex-1 bg-gray-800 hover:bg-gray-700 text-gray-300
                           font-medium py-2 rounded-lg text-sm transition-colors">
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
                        <h3 className="text-lg font-semibold text-white mb-2">Delete Part?</h3>
                        <p className="text-gray-400 text-sm mb-5">This cannot be undone.</p>
                        <div className="flex gap-3">
                            <button onClick={handleDelete}
                                    className="flex-1 bg-red-600 hover:bg-red-700 text-white
                           font-medium py-2 rounded-lg text-sm transition-colors">
                                Delete
                            </button>
                            <button onClick={() => setDeleteId(null)}
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