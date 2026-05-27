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
import ErrorBoundary from "./components/ErrorBoundary"

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
                        <Route index element={
                            <ErrorBoundary><DashboardPage /></ErrorBoundary>
                        } />
                        <Route path="equipment" element={
                            <ErrorBoundary><EquipmentPage /></ErrorBoundary>
                        } />
                        <Route path="parts" element={
                            <ErrorBoundary><PartsPage /></ErrorBoundary>
                        } />
                        <Route path="suppliers" element={
                            <ErrorBoundary><SuppliersPage /></ErrorBoundary>
                        } />
                        <Route path="maintenance" element={
                            <ErrorBoundary><MaintenancePage /></ErrorBoundary>
                        } />
                        <Route path="breakdowns" element={
                            <ErrorBoundary><BreakdownsPage /></ErrorBoundary>
                        } />
                        <Route path="work-orders" element={
                            <ErrorBoundary><WorkOrdersPage /></ErrorBoundary>
                        } />
                        <Route path="users" element={
                            <ErrorBoundary><UsersPage /></ErrorBoundary>
                        } />
                        <Route path="*" element={<Navigate to="/" replace />} />
                    </Route>

                </Routes>
            </BrowserRouter>
        </AuthProvider>
    )
}

export default App