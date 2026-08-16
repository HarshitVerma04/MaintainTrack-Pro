import { useState, useEffect } from "react"
import API from "../api/axios"
import {
    BarChart, Bar, LineChart, Line, PieChart, Pie, Cell,
    XAxis, YAxis, Tooltip, ResponsiveContainer, Legend
} from "recharts"

const COLORS = ["#6366f1", "#22c55e", "#f59e0b", "#ef4444", "#8b5cf6"]

function MetricCard({ label, value, sub, color }) {
    return (
        <div className="bg-gray-900 rounded-xl border border-gray-800 p-5">
            <p className="text-sm text-gray-500">{label}</p>
            <p className={`text-3xl font-bold mt-1 ${color || "text-white"}`}>{value}</p>
            {sub && <p className="text-xs text-gray-600 mt-1">{sub}</p>}
        </div>
    )
}

function ActivityItem({ item }) {
    const icons = { maintenance: "🔧", breakdown: "⚠️" }
    return (
        <div className="flex items-start gap-3 py-2.5 border-b border-gray-800 last:border-0">
            <span className="text-base mt-0.5">{icons[item.type] || "📋"}</span>
            <div className="flex-1 min-w-0">
                <p className="text-sm text-white truncate">{item.description}</p>
                <p className="text-xs text-gray-600 mt-0.5">{item.date}</p>
            </div>
            {item.by && (
                <span className="text-xs text-indigo-400 shrink-0">{item.by}</span>
            )}
        </div>
    )
}

export default function DashboardPage() {
    const [kpis,     setKpis]     = useState(null)
    const [loading,  setLoading]  = useState(true)
    const [error,    setError]    = useState("")

    // For charts — we'll derive from the KPI data + extra calls
    const [maintenanceByMonth, setMaintenanceByMonth] = useState([])
    const [breakdownByMonth,   setBreakdownByMonth]   = useState([])
    const [equipmentStatus,    setEquipmentStatus]    = useState([])

    useEffect(() => {
        fetchDashboard()
        fetchChartData()
    }, [])

    const fetchDashboard = async () => {
        setLoading(true)
        try {
            const res = await API.get("/api/dashboard/kpis")
            setKpis(res.data)
        } catch { setError("Failed to load dashboard.") }
        finally  { setLoading(false) }
    }

    const fetchChartData = async () => {
        try {
            // Equipment status distribution
            const eqRes = await API.get("/api/equipment")
            const equipment = Array.isArray(eqRes.data) ? eqRes.data : []

            const statusCounts = equipment.reduce((acc, e) => {
                acc[e.status] = (acc[e.status] || 0) + 1
                return acc
            }, {})
            setEquipmentStatus(
                Object.entries(statusCounts).map(([name, value]) => ({ name, value }))
            )

            // Maintenance by month (last 6 months)
            const mRes = await API.get("/api/maintenance")
            const logs = Array.isArray(mRes.data) ? mRes.data : []
            setMaintenanceByMonth(groupByMonth(logs, "doneOn", 6))

            // Breakdowns by month (last 6 months)
            const bRes = await API.get("/api/breakdowns")
            const breakdowns = Array.isArray(bRes.data) ? bRes.data : []
            setBreakdownByMonth(groupByMonth(breakdowns, "occurredOn", 6))

        } catch (e) {
            console.error("Chart data error:", e)
        }
    }

    const groupByMonth = (items, dateField, monthCount) => {
        const now    = new Date()
        const months = []

        for (let i = monthCount - 1; i >= 0; i--) {
            const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
            months.push({
                month: d.toLocaleString("default", { month: "short", year: "2-digit" }),
                year:  d.getFullYear(),
                m:     d.getMonth(),
                count: 0
            })
        }

        items.forEach(item => {
            const d = item[dateField]
            if (!d) return
            const date  = new Date(d)
            const entry = months.find(
                m => m.year === date.getFullYear() && m.m === date.getMonth()
            )
            if (entry) entry.count++
        })

        return months.map(({ month, count }) => ({ month, count }))
    }

    if (loading) return (
        <div className="flex items-center justify-center h-64">
            <p className="text-gray-500">Loading dashboard...</p>
        </div>
    )

    if (error) return (
        <div className="flex items-center justify-center h-64">
            <p className="text-red-400">{error}</p>
        </div>
    )

    return (
        <div className="space-y-6">

            <div>
                <h2 className="text-2xl font-bold text-white">Dashboard</h2>
                <p className="text-sm text-gray-500 mt-0.5">Live overview of your facility</p>
            </div>

            {/* ── Metric Cards ───────────────────────────────────────── */}
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                <MetricCard
                    label="Total Equipment"
                    value={kpis?.totalEquipment ?? "—"}
                    sub="across all locations"
                />
                <MetricCard
                    label="Overdue Maintenance"
                    value={kpis?.overdueCount ?? "—"}
                    color={kpis?.overdueCount > 0 ? "text-red-400" : "text-green-400"}
                    sub="require immediate attention"
                />
                <MetricCard
                    label="Low Stock Parts"
                    value={kpis?.lowStockCount ?? "—"}
                    color={kpis?.lowStockCount > 0 ? "text-yellow-400" : "text-green-400"}
                    sub="below minimum quantity"
                />
                <MetricCard
                    label="Open Work Orders"
                    value={(kpis?.openWorkOrders ?? 0) + (kpis?.inProgressWorkOrders ?? 0)}
                    color="text-indigo-400"
                    sub={`${kpis?.inProgressWorkOrders ?? 0} in progress`}
                />
            </div>

            {/* ── Charts Row ─────────────────────────────────────────── */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">

                {/* Maintenance per month */}
                <div className="lg:col-span-2 bg-gray-900 rounded-xl border border-gray-800 p-5">
                    <h3 className="text-sm font-semibold text-white mb-4">
                        Maintenance Activity — Last 6 Months
                    </h3>
                    <ResponsiveContainer width="100%" height={200}>
                        <BarChart data={maintenanceByMonth}
                                  margin={{ top: 0, right: 0, bottom: 0, left: -20 }}>
                            <XAxis dataKey="month" tick={{ fill: "#6b7280", fontSize: 11 }}
                                   axisLine={false} tickLine={false}/>
                            <YAxis tick={{ fill: "#6b7280", fontSize: 11 }}
                                   axisLine={false} tickLine={false} allowDecimals={false}/>
                            <Tooltip
                                contentStyle={{ background: "#1f2937", border: "1px solid #374151",
                                    borderRadius: "8px", color: "#fff" }}
                                cursor={{ fill: "#374151" }}
                            />
                            <Bar dataKey="count" fill="#6366f1" radius={[4,4,0,0]} name="Logs"/>
                        </BarChart>
                    </ResponsiveContainer>
                </div>

                {/* Equipment status donut */}
                <div className="bg-gray-900 rounded-xl border border-gray-800 p-5">
                    <h3 className="text-sm font-semibold text-white mb-4">
                        Equipment Status
                    </h3>
                    <ResponsiveContainer width="100%" height={200}>
                        <PieChart>
                            <Pie
                                data={equipmentStatus}
                                cx="50%" cy="50%"
                                innerRadius={55} outerRadius={80}
                                paddingAngle={3}
                                dataKey="value"
                            >
                                {equipmentStatus.map((_, i) => (
                                    <Cell key={i} fill={COLORS[i % COLORS.length]} />
                                ))}
                            </Pie>
                            <Tooltip
                                contentStyle={{ background: "#1f2937", border: "1px solid #374151",
                                    borderRadius: "8px", color: "#fff" }}
                            />
                            <Legend
                                iconType="circle" iconSize={8}
                                formatter={v => <span style={{ color: "#9ca3af", fontSize: 11 }}>{v}</span>}
                            />
                        </PieChart>
                    </ResponsiveContainer>
                </div>

            </div>

            {/* ── Breakdown Trend + Activity ──────────────────────────── */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">

                {/* Breakdown trend */}
                <div className="bg-gray-900 rounded-xl border border-gray-800 p-5">
                    <h3 className="text-sm font-semibold text-white mb-4">
                        Breakdown Frequency — Last 6 Months
                    </h3>
                    <ResponsiveContainer width="100%" height={180}>
                        <LineChart data={breakdownByMonth}
                                   margin={{ top: 0, right: 0, bottom: 0, left: -20 }}>
                            <XAxis dataKey="month" tick={{ fill: "#6b7280", fontSize: 11 }}
                                   axisLine={false} tickLine={false}/>
                            <YAxis tick={{ fill: "#6b7280", fontSize: 11 }}
                                   axisLine={false} tickLine={false} allowDecimals={false}/>
                            <Tooltip
                                contentStyle={{ background: "#1f2937", border: "1px solid #374151",
                                    borderRadius: "8px", color: "#fff" }}
                            />
                            <Line type="monotone" dataKey="count" stroke="#ef4444"
                                  strokeWidth={2} dot={{ fill: "#ef4444", r: 3 }}
                                  name="Breakdowns"/>
                        </LineChart>
                    </ResponsiveContainer>
                </div>

                {/* Recent activity */}
                <div className="bg-gray-900 rounded-xl border border-gray-800 p-5">
                    <h3 className="text-sm font-semibold text-white mb-3">Recent Activity</h3>
                    <div className="overflow-y-auto max-h-52">
                        {kpis?.recentActivity?.length > 0 ? (
                            kpis.recentActivity.map((item, i) => (
                                <ActivityItem key={i} item={item} />
                            ))
                        ) : (
                            <p className="text-gray-600 text-sm py-4 text-center">No activity yet.</p>
                        )}
                    </div>
                </div>

            </div>
        </div>
    )
}