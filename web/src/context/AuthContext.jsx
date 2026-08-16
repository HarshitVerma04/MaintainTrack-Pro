import { createContext, useContext, useState } from "react"

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
    const [user, setUser] = useState(() => {
        const stored = localStorage.getItem("user")
        return stored ? JSON.parse(stored) : null
    })

    const [token, setToken] = useState(() => localStorage.getItem("token"))

    const login = (tokenStr, userData) => {
        localStorage.setItem("token", tokenStr)
        localStorage.setItem("user", JSON.stringify(userData))
        setToken(tokenStr)
        setUser(userData)
    }

    const logout = () => {
        localStorage.removeItem("token")
        localStorage.removeItem("user")
        setToken(null)
        setUser(null)
    }

    const isLoggedIn    = () => !!token
    const isAdmin       = () => user?.role === "ADMIN"
    const isManager     = () => user?.role === "MANAGER" || user?.role === "ADMIN"
    const isTechnician  = () => user?.role === "TECHNICIAN"

    // Returns true if the user can delete records (ADMIN or MANAGER only)
    const canDelete     = () => user?.role === "ADMIN" || user?.role === "MANAGER"

    return (
        <AuthContext.Provider value={{
            user, token, login, logout,
            isLoggedIn, isAdmin, isManager, isTechnician, canDelete
        }}>
            {children}
        </AuthContext.Provider>
    )
}

export const useAuth = () => useContext(AuthContext)