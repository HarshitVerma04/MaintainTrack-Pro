import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom"
import { AuthProvider } from "./context/AuthContext"
import ProtectedRoute from "./components/ProtectedRoute"
import MainLayout from "./components/MainLayout"
import LoginPage from "./pages/LoginPage"
import DashboardPage from "./pages/DashboardPage"
import EquipmentPage from "./pages/EquipmentPage"
import PartsPage     from "./pages/PartsPage"
import SuppliersPage from "./pages/SuppliersPage"
import MaintenancePage from "./pages/MaintenancePage"
import BreakdownsPage  from "./pages/BreakdownsPage"
import WorkOrdersPage from "./pages/WorkOrdersPage"
import UsersPage from "./pages/UsersPage"

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
                        <Route path="maintenance" element={<MaintenancePage />} />
                        <Route path="breakdowns"  element={<BreakdownsPage />} />
                        <Route path="work-orders" element={<WorkOrdersPage />} />
                        <Route path="users"       element={<UsersPage />} />
                        <Route path="*"           element={<Navigate to="/" replace />} />
                    </Route>

                </Routes>
            </BrowserRouter>
        </AuthProvider>
    )
}

export default App