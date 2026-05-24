import { useState } from "react"
import { NavLink, Outlet, useNavigate } from "react-router-dom"
import { useAuth } from "../context/AuthContext"
import API from "../api/axios"

const navItems = [
    { to: "/",             label: "Dashboard",    icon: "📊" },
    { to: "/equipment",    label: "Equipment",    icon: "⚙️" },
    { to: "/parts",        label: "Parts",        icon: "🔩" },
    { to: "/suppliers",    label: "Suppliers",    icon: "🏭" },
    { to: "/maintenance",  label: "Maintenance",  icon: "🔧" },
    { to: "/breakdowns",   label: "Breakdowns",   icon: "⚠️" },
    { to: "/work-orders",  label: "Work Orders",  icon: "📋" },
]

export default function MainLayout() {
    const { user, logout, isAdmin, isManager } = useAuth()
    const navigate  = useNavigate()
    const [mobile, setMobile] = useState(false)

    const handleLogout = async () => {
        try { await API.post("/auth/logout") } catch {}
        logout()
        navigate("/login")
    }

    const adminItems = isAdmin() ? [
        { to: "/users", label: "Users", icon: "👥" }
    ] : []

    const allItems = [...navItems, ...adminItems]

    return (
        <div className="flex h-screen bg-gray-950 text-white overflow-hidden">

            {/* ── Sidebar ───────────────────────────────────────────── */}
            <aside className={`
        flex flex-col bg-gray-900 border-r border-gray-800
        w-56 flex-shrink-0 transition-all duration-200
        ${mobile ? "translate-x-0" : "-translate-x-full md:translate-x-0"}
        fixed md:static inset-y-0 left-0 z-50
      `}>

                {/* Logo */}
                <div className="px-5 py-6 border-b border-gray-800">
                    <h1 className="text-lg font-bold text-white">MaintainTrack</h1>
                    <p className="text-xs text-indigo-400 font-medium">Pro v2.0</p>
                </div>

                {/* Nav */}
                <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
                    {allItems.map(item => (
                        <NavLink
                            key={item.to}
                            to={item.to}
                            end={item.to === "/"}
                            onClick={() => setMobile(false)}
                            className={({ isActive }) => `
                flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm
                transition-colors font-medium
                ${isActive
                                ? "bg-indigo-600 text-white"
                                : "text-gray-400 hover:bg-gray-800 hover:text-white"}
              `}
                        >
                            <span>{item.icon}</span>
                            <span>{item.label}</span>
                        </NavLink>
                    ))}
                </nav>

                {/* User + Logout */}
                <div className="px-3 py-4 border-t border-gray-800 space-y-2">
                    <div className="px-3 py-2">
                        <p className="text-sm font-medium text-white">{user?.username}</p>
                        <p className="text-xs text-gray-500">{user?.role}</p>
                    </div>
                    <button
                        onClick={handleLogout}
                        className="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg
                       text-sm text-red-400 hover:bg-gray-800 transition-colors"
                    >
                        <span>⏻</span>
                        <span>Logout</span>
                    </button>
                </div>
            </aside>

            {/* ── Mobile overlay ────────────────────────────────────── */}
            {mobile && (
                <div
                    className="fixed inset-0 bg-black/50 z-40 md:hidden"
                    onClick={() => setMobile(false)}
                />
            )}

            {/* ── Main content ──────────────────────────────────────── */}
            <div className="flex-1 flex flex-col overflow-hidden">

                {/* Top bar */}
                <header className="bg-gray-900 border-b border-gray-800 px-6 py-4
                           flex items-center justify-between flex-shrink-0">
                    <button
                        className="md:hidden text-gray-400 hover:text-white"
                        onClick={() => setMobile(!mobile)}
                    >
                        ☰
                    </button>
                    <div className="hidden md:block text-sm text-gray-500">
                        MaintainTrack Pro — Cloud + Desktop Hybrid
                    </div>
                    <div className="text-xs text-gray-600">
                        {new Date().toLocaleDateString("en-IN", {
                            weekday: "long", year: "numeric",
                            month: "long", day: "numeric"
                        })}
                    </div>
                </header>

                {/* Page content */}
                <main className="flex-1 overflow-y-auto p-6">
                    <Outlet />
                </main>
            </div>
        </div>
    )
}