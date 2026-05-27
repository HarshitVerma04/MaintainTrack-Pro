import { useState, useEffect } from "react"
import API from "../api/axios"
import {
    DndContext, DragOverlay, closestCenter,
    PointerSensor, useSensor, useSensors
} from "@dnd-kit/core"
import { useDroppable } from "@dnd-kit/core"
import { useDraggable } from "@dnd-kit/core"

const STATUSES   = ["Open", "In Progress", "Resolved"]
const PRIORITIES = ["Low", "Medium", "High"]

const PRIORITY_COLORS = {
    High:   "bg-red-900 text-red-300",
    Medium: "bg-yellow-900 text-yellow-300",
    Low:    "bg-green-900 text-green-300",
}

const COLUMN_COLORS = {
    "Open":        "border-gray-700",
    "In Progress": "border-yellow-700",
    "Resolved":    "border-green-700",
}

const EMPTY_FORM = {
    title: "", description: "", priority: "Medium",
    assignedTo: "", equipmentId: ""
}

function Card({ wo, onEdit }) {
    const { attributes, listeners, setNodeRef, transform, isDragging } =
        useDraggable({ id: wo.id.toString() })

    const style = transform ? {
        transform: `translate(${transform.x}px, ${transform.y}px)`,
        zIndex: 999, opacity: 0.9
    } : {}

    return (
        <div
            ref={setNodeRef}
            style={style}
            {...listeners}
            {...attributes}
            className={`bg-gray-800 rounded-lg p-3 border border-gray-700
                  cursor-grab active:cursor-grabbing space-y-2
                  ${isDragging ? "opacity-50" : "hover:border-gray-600"}
                  transition-colors`}
        >
            <div className="flex items-start justify-between gap-2">
                <p className="text-sm font-medium text-white leading-snug">{wo.title}</p>
                <span className={`shrink-0 px-1.5 py-0.5 rounded text-xs font-medium
                          ${PRIORITY_COLORS[wo.priority] || ""}`}>
          {wo.priority}
        </span>
            </div>

            {wo.description && (
                <p className="text-xs text-gray-500 line-clamp-2">{wo.description}</p>
            )}

            <div className="flex items-center justify-between">
        <span className="text-xs text-gray-600">
          {wo.equipment?.name || "—"}
        </span>
                {wo.assignedTo && (
                    <span className="text-xs text-indigo-400">{wo.assignedTo}</span>
                )}
            </div>

            <button
                onClick={e => { e.stopPropagation(); onEdit(wo) }}
                onPointerDown={e => e.stopPropagation()}
                className="text-xs text-gray-600 hover:text-gray-400 transition-colors"
            >
                Edit
            </button>
        </div>
    )
}

function Column({ status, items, onEdit }) {
    const { setNodeRef, isOver } = useDroppable({ id: status })

    return (
        <div
            ref={setNodeRef}
            className={`flex flex-col bg-gray-900 rounded-xl border-t-2
                  ${COLUMN_COLORS[status]}
                  ${isOver ? "bg-gray-800/80" : ""}
                  transition-colors min-h-96`}
        >
            <div className="px-4 py-3 border-b border-gray-800 flex items-center justify-between">
                <h3 className="text-sm font-semibold text-white">{status}</h3>
                <span className="text-xs text-gray-500 bg-gray-800 px-2 py-0.5 rounded-full">
          {items.length}
        </span>
            </div>
            <div className="flex-1 p-3 space-y-2 overflow-y-auto">
                {items.length === 0 && (
                    <p className="text-xs text-gray-700 text-center py-8">Drop here</p>
                )}
                {items.map(wo => (
                    <Card key={wo.id} wo={wo} onEdit={onEdit} />
                ))}
            </div>
        </div>
    )
}

export default function WorkOrdersPage() {
    const [workOrders, setWorkOrders] = useState([])
    const [equipment,  setEquipment]  = useState([])
    const [loading,    setLoading]    = useState(true)
    const [modal,      setModal]      = useState(false)
    const [editing,    setEditing]    = useState(null)
    const [form,       setForm]       = useState(EMPTY_FORM)
    const [saving,     setSaving]     = useState(false)
    const [error,      setError]      = useState("")
    const [activeId,   setActiveId]   = useState(null)

    const sensors = useSensors(useSensor(PointerSensor, {
        activationConstraint: { distance: 8 }
    }))

    useEffect(() => { fetchWorkOrders(); fetchEquipment() }, [])

    const fetchWorkOrders = async () => {
        setLoading(true)
        try {
            const res = await API.get("/api/work-orders")
            setWorkOrders(Array.isArray(res.data) ? res.data : [])
        } catch { setError("Failed to load work orders.") }
        finally  { setLoading(false) }
    }

    const fetchEquipment = async () => {
        try {
            const res = await API.get("/api/equipment")
            setEquipment(Array.isArray(res.data) ? res.data : [])
        } catch {}
    }

    const openAdd = () => {
        setEditing(null); setForm(EMPTY_FORM)
        setError(""); setModal(true)
    }

    const openEdit = (wo) => {
        setEditing(wo)
        setForm({
            title:       wo.title,
            description: wo.description || "",
            priority:    wo.priority,
            assignedTo:  wo.assignedTo || "",
            equipmentId: wo.equipment?.id || ""
        })
        setError(""); setModal(true)
    }

    const closeModal = () => {
        setModal(false); setEditing(null)
        setForm(EMPTY_FORM); setError("")
    }

    const handleSave = async () => {
        if (!form.title.trim())    { setError("Title is required."); return }
        if (!form.equipmentId)     { setError("Please select equipment."); return }
        setSaving(true); setError("")
        try {
            if (editing) {
                await API.put(`/api/work-orders/${editing.id}`, {
                    ...form, equipmentId: parseInt(form.equipmentId),
                    status: editing.status
                })
            } else {
                await API.post(`/api/work-orders?equipmentId=${form.equipmentId}`, {
                    title:       form.title,
                    description: form.description,
                    priority:    form.priority,
                    assignedTo:  form.assignedTo,
                })
            }
            await fetchWorkOrders()
            closeModal()
        } catch (err) {
            setError(err.response?.data?.error || "Save failed.")
        } finally { setSaving(false) }
    }

    const handleDragStart = ({ active }) => setActiveId(active.id)

    const handleDragEnd = async ({ active, over }) => {
        setActiveId(null)
        if (!over) return
        const newStatus = over.id
        if (!STATUSES.includes(newStatus)) return

        const wo = workOrders.find(w => w.id.toString() === active.id)
        if (!wo || wo.status === newStatus) return

        // Optimistic update
        setWorkOrders(prev => prev.map(w =>
            w.id.toString() === active.id ? { ...w, status: newStatus } : w
        ))

        try {
            await API.put(`/api/work-orders/${wo.id}/status`, { status: newStatus })
        } catch {
            // Revert on failure
            setWorkOrders(prev => prev.map(w =>
                w.id.toString() === active.id ? { ...w, status: wo.status } : w
            ))
        }
    }

    const columns = STATUSES.reduce((acc, s) => {
        acc[s] = workOrders.filter(wo => wo.status === s)
        return acc
    }, {})

    const activeWo = activeId
        ? workOrders.find(w => w.id.toString() === activeId)
        : null

    return (
        <div className="space-y-5 h-full flex flex-col">

            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-2xl font-bold text-white">Work Orders</h2>
                    <p className="text-sm text-gray-500 mt-0.5">
                        {workOrders.length} total ·{" "}
                        <span className="text-yellow-400">
              {columns["Open"]?.length || 0} open
            </span>
                    </p>
                </div>
                <button onClick={openAdd}
                        className="bg-indigo-600 hover:bg-indigo-700 text-white text-sm
                     font-medium px-4 py-2 rounded-lg transition-colors">
                    + New Work Order
                </button>
            </div>

            {loading ? (
                <div className="text-gray-500 text-sm py-12 text-center">Loading...</div>
            ) : (
                <DndContext
                    sensors={sensors}
                    collisionDetection={closestCenter}
                    onDragStart={handleDragStart}
                    onDragEnd={handleDragEnd}
                >
                    <div className="grid grid-cols-3 gap-4 flex-1">
                        {STATUSES.map(status => (
                            <Column
                                key={status}
                                status={status}
                                items={columns[status] || []}
                                onEdit={openEdit}
                            />
                        ))}
                    </div>

                    <DragOverlay>
                        {activeWo && (
                            <div className="bg-gray-800 rounded-lg p-3 border border-indigo-500
                              shadow-2xl w-64 space-y-2">
                                <p className="text-sm font-medium text-white">{activeWo.title}</p>
                                <p className="text-xs text-gray-500">{activeWo.equipment?.name}</p>
                            </div>
                        )}
                    </DragOverlay>
                </DndContext>
            )}

            {/* Add/Edit Modal */}
            {modal && (
                <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
                    <div className="bg-gray-900 rounded-2xl border border-gray-700
                          w-full max-w-md p-6 space-y-4">
                        <h3 className="text-lg font-semibold text-white">
                            {editing ? "Edit Work Order" : "New Work Order"}
                        </h3>

                        <div className="space-y-3">
                            <div>
                                <label className="block text-xs text-gray-400 mb-1">Title *</label>
                                <input value={form.title}
                                       onChange={e => setForm({...form, title: e.target.value})}
                                       placeholder="e.g. Replace conveyor belt"
                                       className="w-full bg-gray-800 border border-gray-700 text-white
                             rounded-lg px-3 py-2 text-sm focus:outline-none
                             focus:border-indigo-500"/>
                            </div>

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

                            <div className="grid grid-cols-2 gap-3">
                                <div>
                                    <label className="block text-xs text-gray-400 mb-1">Priority</label>
                                    <select value={form.priority}
                                            onChange={e => setForm({...form, priority: e.target.value})}
                                            className="w-full bg-gray-800 border border-gray-700 text-white
                               rounded-lg px-3 py-2 text-sm focus:outline-none
                               focus:border-indigo-500">
                                        {PRIORITIES.map(p => <option key={p}>{p}</option>)}
                                    </select>
                                </div>

                                <div>
                                    <label className="block text-xs text-gray-400 mb-1">Assign To</label>
                                    <input value={form.assignedTo}
                                           onChange={e => setForm({...form, assignedTo: e.target.value})}
                                           placeholder="e.g. Ravi"
                                           className="w-full bg-gray-800 border border-gray-700 text-white
                               rounded-lg px-3 py-2 text-sm focus:outline-none
                               focus:border-indigo-500"/>
                                </div>
                            </div>

                            <div>
                                <label className="block text-xs text-gray-400 mb-1">Description</label>
                                <textarea value={form.description}
                                          onChange={e => setForm({...form, description: e.target.value})}
                                          rows={3} placeholder="Details about the work order..."
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
                                {saving ? "Saving..." : editing ? "Save Changes" : "Create"}
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
        </div>
    )
}