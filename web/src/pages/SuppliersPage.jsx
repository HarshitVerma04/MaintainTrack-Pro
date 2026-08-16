import { useState, useEffect } from "react"
import API from "../api/axios"
import toast from "react-hot-toast"
import { useAuth } from "../context/AuthContext"

const EMPTY_FORM = { name: "", contactName: "", phone: "", email: "" }

export default function SuppliersPage() {
    const { canDelete } = useAuth()

    const [suppliers, setSuppliers] = useState([])
    const [filtered,  setFiltered]  = useState([])
    const [loading,   setLoading]   = useState(true)
    const [search,    setSearch]    = useState("")
    const [modal,     setModal]     = useState(false)
    const [editing,   setEditing]   = useState(null)
    const [form,      setForm]      = useState(EMPTY_FORM)
    const [saving,    setSaving]    = useState(false)
    const [error,     setError]     = useState("")
    const [deleteId,  setDeleteId]  = useState(null)

    useEffect(() => { fetchSuppliers() }, [])

    useEffect(() => {
        const q = search.toLowerCase()
        setFiltered(suppliers.filter(s =>
            s.name.toLowerCase().includes(q) ||
            (s.contactName || "").toLowerCase().includes(q) ||
            (s.email || "").toLowerCase().includes(q)
        ))
    }, [search, suppliers])

    const fetchSuppliers = async () => {
        setLoading(true)
        try {
            const res = await API.get("/api/suppliers")
            setSuppliers(Array.isArray(res.data) ? res.data : [])
        } catch {
            toast.error("Failed to load suppliers.")
        } finally { setLoading(false) }
    }

    const openAdd = () => {
        setEditing(null); setForm(EMPTY_FORM); setError(""); setModal(true)
    }

    const openEdit = (s) => {
        setEditing(s)
        setForm({ name: s.name, contactName: s.contactName || "",
            phone: s.phone || "", email: s.email || "" })
        setError(""); setModal(true)
    }

    const closeModal = () => {
        setModal(false); setEditing(null); setForm(EMPTY_FORM); setError("")
    }

    const handleSave = async () => {
        if (!form.name.trim()) { setError("Name is required."); return }
        setSaving(true); setError("")
        try {
            if (editing) {
                await API.put(`/api/suppliers/${editing.id}`, form)
            } else {
                await API.post("/api/suppliers", form)
            }
            await fetchSuppliers()
            closeModal()
            toast.success(editing ? "Supplier updated." : "Supplier added.")
        } catch (err) {
            setError(err.response?.data?.message || "Save failed.")
        } finally { setSaving(false) }
    }

    const handleDelete = async () => {
        try {
            await API.delete(`/api/suppliers/${deleteId}`)
            setDeleteId(null)
            await fetchSuppliers()
            toast.success("Supplier deleted.")
        } catch (err) {
            setDeleteId(null)
            if (err.response?.status === 403) {
                toast.error("You don't have permission to delete suppliers.")
            } else {
                toast.error("Delete failed.")
            }
        }
    }

    return (
        <div className="space-y-5">

            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-2xl font-bold text-white">Suppliers</h2>
                    <p className="text-sm text-gray-500 mt-0.5">{suppliers.length} total</p>
                </div>
                <button onClick={openAdd}
                        className="bg-indigo-600 hover:bg-indigo-700 text-white text-sm
                     font-medium px-4 py-2 rounded-lg transition-colors">
                    + Add Supplier
                </button>
            </div>

            <input type="text" placeholder="Search suppliers..."
                   value={search} onChange={e => setSearch(e.target.value)}
                   className="w-full bg-gray-900 border border-gray-700 text-white
                   rounded-lg px-4 py-2.5 text-sm placeholder-gray-600
                   focus:outline-none focus:border-indigo-500"/>

            {loading ? (
                <div className="text-gray-500 text-sm py-12 text-center">Loading...</div>
            ) : filtered.length === 0 ? (
                <div className="text-gray-500 text-sm py-12 text-center">No suppliers found.</div>
            ) : (
                <div className="bg-gray-900 rounded-xl border border-gray-800 overflow-hidden">
                    <table className="w-full text-sm">
                        <thead>
                        <tr className="border-b border-gray-800 text-gray-500 text-xs uppercase tracking-wide">
                            <th className="px-5 py-3 text-left">Name</th>
                            <th className="px-5 py-3 text-left">Contact</th>
                            <th className="px-5 py-3 text-left">Phone</th>
                            <th className="px-5 py-3 text-left">Email</th>
                            <th className="px-5 py-3 text-left">Actions</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-800">
                        {filtered.map(s => (
                            <tr key={s.id} className="hover:bg-gray-800/50 transition-colors">
                                <td className="px-5 py-3 font-medium text-white">{s.name}</td>
                                <td className="px-5 py-3 text-gray-400">{s.contactName || "—"}</td>
                                <td className="px-5 py-3 text-gray-400">{s.phone || "—"}</td>
                                <td className="px-5 py-3 text-gray-400">{s.email || "—"}</td>
                                <td className="px-5 py-3">
                                    <div className="flex gap-2">
                                        <button onClick={() => openEdit(s)}
                                                className="text-indigo-400 hover:text-indigo-300 text-xs
                                   px-2 py-1 rounded hover:bg-gray-700 transition-colors">
                                            Edit
                                        </button>
                                        {/* Delete — ADMIN and MANAGER only */}
                                        {canDelete() && (
                                            <button onClick={() => setDeleteId(s.id)}
                                                    className="text-red-400 hover:text-red-300 text-xs
                                     px-2 py-1 rounded hover:bg-gray-700 transition-colors">
                                                Delete
                                            </button>
                                        )}
                                    </div>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}

            {/* Add / Edit Modal */}
            {modal && (
                <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
                    <div className="bg-gray-900 rounded-2xl border border-gray-700
                          w-full max-w-md p-6 space-y-4">
                        <h3 className="text-lg font-semibold text-white">
                            {editing ? "Edit Supplier" : "Add Supplier"}
                        </h3>
                        <div className="space-y-3">
                            {[
                                { key: "name",        label: "Name *",       ph: "e.g. FastParts Ltd" },
                                { key: "contactName", label: "Contact Name", ph: "e.g. Rahul Sharma" },
                                { key: "phone",       label: "Phone",        ph: "e.g. +91-9876543210" },
                                { key: "email",       label: "Email",        ph: "e.g. rahul@fastparts.in" },
                            ].map(f => (
                                <div key={f.key}>
                                    <label className="block text-xs text-gray-400 mb-1">{f.label}</label>
                                    <input value={form[f.key]}
                                           onChange={e => setForm({...form, [f.key]: e.target.value})}
                                           placeholder={f.ph}
                                           className="w-full bg-gray-800 border border-gray-700 text-white
                               rounded-lg px-3 py-2 text-sm focus:outline-none
                               focus:border-indigo-500"/>
                                </div>
                            ))}
                        </div>

                        {error && <p className="text-red-400 text-sm">{error}</p>}

                        <div className="flex gap-3 pt-2">
                            <button onClick={handleSave} disabled={saving}
                                    className="flex-1 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50
                           text-white font-medium py-2 rounded-lg text-sm transition-colors">
                                {saving ? "Saving..." : editing ? "Save Changes" : "Add Supplier"}
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
                        <h3 className="text-lg font-semibold text-white mb-2">Delete Supplier?</h3>
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