import { useState, useEffect } from "react"
import API from "../api/axios"
import { useAuth } from "../context/AuthContext"
import toast from "react-hot-toast"
import EmptyState from "../components/EmptyState"
import SkeletonRow from "../components/SkeletonRow"

const ROLES = ["ADMIN", "MANAGER", "TECHNICIAN"]

const ROLE_COLORS = {
    ADMIN:      "bg-red-900 text-red-300",
    MANAGER:    "bg-indigo-900 text-indigo-300",
    TECHNICIAN: "bg-green-900 text-green-300",
}

export default function UsersPage() {
    const { user }      = useAuth()
    const [users,       setUsers]       = useState([])
    const [loading,     setLoading]     = useState(true)
    const [error,       setError]       = useState("")
    const [search,      setSearch]      = useState("")
    const [filtered,    setFiltered]    = useState([])
    const [modal,       setModal]       = useState(false)
    const [form,        setForm]        = useState({ username: "", email: "", password: "", role: "TECHNICIAN" })
    const [saving,      setSaving]      = useState(false)
    const [formError,   setFormError]   = useState("")
    const [deleteId,    setDeleteId]    = useState(null)
    const [changingRole, setChangingRole] = useState(null)

    useEffect(() => { fetchUsers() }, [])

    useEffect(() => {
        const q = search.toLowerCase()
        setFiltered(users.filter(u =>
            u.username.toLowerCase().includes(q) ||
            (u.email || "").toLowerCase().includes(q) ||
            u.role.toLowerCase().includes(q)
        ))
    }, [search, users])

    const fetchUsers = async () => {
        setLoading(true)
        try {
            const res = await API.get("/api/users")
            setUsers(Array.isArray(res.data) ? res.data : [])
        } catch { setError("Failed to load users.") }
        finally  { setLoading(false) }
    }

    const handleRoleChange = async (userId, newRole) => {
        if (userId === user?.id) return // can't change own role
        setChangingRole(userId)
        try {
            await API.put(`/api/users/${userId}/role`, { role: newRole })
            await fetchUsers()
            toast.success("Role updated.")
        } catch {
            toast.error("Failed to update role.")
        }
        finally { setChangingRole(null) }
    }

    const handleDelete = async () => {
        if (!deleteId) return
        try {
            await API.delete(`/api/users/${deleteId}`)
            setDeleteId(null)
            await fetchUsers()
            toast.success("User removed.")
        } catch {
            setDeleteId(null)
            toast.error("Something went wrong.")
        }
    }

    const handleInvite = async () => {
        if (!form.username.trim()) { setFormError("Username is required."); return }
        if (!form.email.trim())    { setFormError("Email is required."); return }
        if (!form.password.trim()) { setFormError("Password is required."); return }
        setSaving(true); setFormError("")
        try {
            await API.post("/auth/register", form)
            await fetchUsers()
            setModal(false)
            setForm({ username: "", email: "", password: "", role: "TECHNICIAN" })
            toast.success("User created.")
        } catch (err) {
            setFormError(err.response?.data?.error || "Failed to create user.")
            toast.error("Something went wrong.")
        } finally { setSaving(false) }
    }

    const formatDate = (dateStr) => {
        if (!dateStr) return "—"
        return new Date(dateStr).toLocaleDateString("en-IN", {
            day: "numeric", month: "short", year: "numeric"
        })
    }

    return (
        <div className="space-y-5">

            {/* Header */}
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-2xl font-bold text-white">Users</h2>
                    <p className="text-sm text-gray-500 mt-0.5">{users.length} total</p>
                </div>
                <button onClick={() => { setModal(true); setFormError("") }}
                        className="bg-indigo-600 hover:bg-indigo-700 text-white text-sm
                     font-medium px-4 py-2 rounded-lg transition-colors">
                    + Invite User
                </button>
            </div>

            {/* Search */}
            <input type="text" placeholder="Search by username, email or role..."
                   value={search} onChange={e => setSearch(e.target.value)}
                   className="w-full bg-gray-900 border border-gray-700 text-white
                   rounded-lg px-4 py-2.5 text-sm placeholder-gray-600
                   focus:outline-none focus:border-indigo-500"/>

            {error && <p className="text-red-400 text-sm">{error}</p>}

            {/* Table */}
            {loading ? (
                <div className="bg-gray-900 rounded-xl border border-gray-800 overflow-hidden">
                    <table className="w-full">
                        <tbody>
                        {/* Note: Users table has 5 columns */}
                        {Array.from({ length: 5 }).map((_, i) => <SkeletonRow key={i} cols={5} />)}
                        </tbody>
                    </table>
                </div>
            ) : filtered.length === 0 ? (
                <EmptyState
                    icon="👥"
                    title="No users found"
                    message="Invite your team members to the platform."
                    action="Invite User"
                    onAction={() => { setModal(true); setFormError("") }}
                />
            ) : (
                <div className="bg-gray-900 rounded-xl border border-gray-800 overflow-hidden">
                    <table className="w-full text-sm">
                        <thead>
                        <tr className="border-b border-gray-800 text-gray-500
                             text-xs uppercase tracking-wide">
                            <th className="px-5 py-3 text-left">Username</th>
                            <th className="px-5 py-3 text-left">Email</th>
                            <th className="px-5 py-3 text-left">Role</th>
                            <th className="px-5 py-3 text-left">Joined</th>
                            <th className="px-5 py-3 text-left">Actions</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-800">
                        {filtered.map(u => (
                            <tr key={u.id}
                                className={`hover:bg-gray-800/50 transition-colors
                    ${u.username === user?.username ? "bg-indigo-950/20" : ""}`}>

                                <td className="px-5 py-3">
                                    <div className="flex items-center gap-2">
                                        <div className="w-7 h-7 rounded-full bg-indigo-700 flex items-center
                                      justify-center text-xs font-bold text-white shrink-0">
                                            {u.username[0].toUpperCase()}
                                        </div>
                                        <span className="font-medium text-white">{u.username}</span>
                                        {u.username === user?.username && (
                                            <span className="text-xs text-gray-600">(you)</span>
                                        )}
                                    </div>
                                </td>

                                <td className="px-5 py-3 text-gray-400">{u.email}</td>

                                <td className="px-5 py-3">
                                    {u.username === user?.username ? (
                                        <span className={`px-2 py-0.5 rounded-full text-xs font-medium
                                        ${ROLE_COLORS[u.role]}`}>
                        {u.role}
                      </span>
                                    ) : (
                                        <select
                                            value={u.role}
                                            disabled={changingRole === u.id}
                                            onChange={e => handleRoleChange(u.id, e.target.value)}
                                            className={`bg-gray-800 border border-gray-700 text-xs
                                    rounded-lg px-2 py-1 focus:outline-none
                                    focus:border-indigo-500 transition-colors
                                    ${ROLE_COLORS[u.role]}
                                    ${changingRole === u.id ? "opacity-50" : ""}`}
                                        >
                                            {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
                                        </select>
                                    )}
                                </td>

                                <td className="px-5 py-3 text-gray-500 text-xs">
                                    {formatDate(u.createdAt)}
                                </td>

                                <td className="px-5 py-3">
                                    {u.username !== user?.username && (
                                        <button onClick={() => setDeleteId(u.id)}
                                                className="text-red-400 hover:text-red-300 text-xs
                                   px-2 py-1 rounded hover:bg-gray-700 transition-colors">
                                            Remove
                                        </button>
                                    )}
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}

            {/* Invite Modal */}
            {modal && (
                <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
                    <div className="bg-gray-900 rounded-2xl border border-gray-700
                          w-full max-w-md p-6 space-y-4">
                        <h3 className="text-lg font-semibold text-white">Invite User</h3>

                        <div className="space-y-3">
                            {[
                                { key: "username", label: "Username *", type: "text",     ph: "e.g. ravi" },
                                { key: "email",    label: "Email *",    type: "email",    ph: "e.g. ravi@company.com" },
                                { key: "password", label: "Password *", type: "password", ph: "Temporary password" },
                            ].map(f => (
                                <div key={f.key}>
                                    <label className="block text-xs text-gray-400 mb-1">{f.label}</label>
                                    <input
                                        type={f.type}
                                        value={form[f.key]}
                                        onChange={e => setForm({...form, [f.key]: e.target.value})}
                                        placeholder={f.ph}
                                        className="w-full bg-gray-800 border border-gray-700 text-white
                               rounded-lg px-3 py-2 text-sm focus:outline-none
                               focus:border-indigo-500"
                                    />
                                </div>
                            ))}

                            <div>
                                <label className="block text-xs text-gray-400 mb-1">Role</label>
                                <select value={form.role}
                                        onChange={e => setForm({...form, role: e.target.value})}
                                        className="w-full bg-gray-800 border border-gray-700 text-white
                             rounded-lg px-3 py-2 text-sm focus:outline-none
                             focus:border-indigo-500">
                                    {ROLES.map(r => <option key={r}>{r}</option>)}
                                </select>
                            </div>
                        </div>

                        {formError && <p className="text-red-400 text-sm">{formError}</p>}

                        <div className="flex gap-3 pt-2">
                            <button onClick={handleInvite} disabled={saving}
                                    className="flex-1 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50
                           text-white font-medium py-2 rounded-lg text-sm transition-colors">
                                {saving ? "Creating..." : "Create User"}
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

            {/* Delete Confirmation */}
            {deleteId && (
                <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
                    <div className="bg-gray-900 rounded-2xl border border-gray-700
                          p-6 max-w-sm w-full">
                        <h3 className="text-lg font-semibold text-white mb-2">Remove User?</h3>
                        <p className="text-gray-400 text-sm mb-5">
                            This user will lose all access immediately.
                        </p>
                        <div className="flex gap-3">
                            <button onClick={handleDelete}
                                    className="flex-1 bg-red-600 hover:bg-red-700 text-white
                           font-medium py-2 rounded-lg text-sm transition-colors">
                                Remove
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