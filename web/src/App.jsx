import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom"
import { AuthProvider } from "./context/AuthContext"
import ProtectedRoute from "./components/ProtectedRoute"
import MainLayout from "./components/MainLayout"
import LoginPage from "./pages/LoginPage"
import DashboardPage from "./pages/DashboardPage"
import EquipmentPage from "./pages/EquipmentPage"
import PartsPage     from "./pages/PartsPage"
import SuppliersPage from "./pages/SuppliersPage"

function App() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <Routes>

                    <Route path="/login" element={<LoginPage />} />

                    <Route
                        path="/"
                        element={
                            <ProtectedRoute>
                                <MainLayout />
                            </ProtectedRoute>
                        }
                    >
                        <Route index element={<DashboardPage />} />
                        <Route path="equipment"   element={<EquipmentPage />} />
                        <Route path="parts"     element={<PartsPage />} />
                        <Route path="suppliers" element={<SuppliersPage />} />
                        <Route path="maintenance" element={<div className="text-gray-400">Maintenance — Day 26</div>} />
                        <Route path="breakdowns"  element={<div className="text-gray-400">Breakdowns — Day 26</div>} />
                        <Route path="work-orders" element={<div className="text-gray-400">Work Orders — Day 27</div>} />
                        <Route path="users"       element={<div className="text-gray-400">Users — Day 29</div>} />
                        <Route path="*"           element={<Navigate to="/" replace />} />
                    </Route>

                </Routes>
            </BrowserRouter>
        </AuthProvider>
    )
}

export default App