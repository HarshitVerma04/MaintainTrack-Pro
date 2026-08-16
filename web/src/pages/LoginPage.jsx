import { useState } from "react"
import { useNavigate } from "react-router-dom"
import { useAuth } from "../context/AuthContext"
import API from "../api/axios"

export default function LoginPage() {
    const [username, setUsername] = useState("")
    const [password, setPassword] = useState("")
    const [error,    setError]    = useState("")
    const [loading,  setLoading]  = useState(false)
    const { login }  = useAuth()
    const navigate   = useNavigate()

    const handleSubmit = async (e) => {
        e.preventDefault()
        if (!username || !password) {
            setError("Please enter username and password.")
            return
        }
        setLoading(true)
        setError("")
        try {
            const res = await API.post("/auth/login", { username, password })
            login(res.data.token, {
                username: res.data.username,
                role:     res.data.role,
                email:    res.data.email,
            })
            navigate("/")
        } catch (err) {
            setError(err.response?.data?.error || "Login failed. Check your credentials.")
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="min-h-screen bg-gray-950 flex items-center justify-center px-4">
            <div className="w-full max-w-md">

                {/* Header */}
                <div className="text-center mb-8">
                    <h1 className="text-3xl font-bold text-white">MaintainTrack Pro</h1>
                    <p className="text-gray-400 mt-2 text-sm">Sign in to your account</p>
                </div>

                {/* Card */}
                <div className="bg-gray-900 rounded-2xl p-8 shadow-xl border border-gray-800">
                    <form onSubmit={handleSubmit} className="space-y-5">

                        <div>
                            <label className="block text-sm text-gray-400 mb-1">Username</label>
                            <input
                                type="text"
                                value={username}
                                onChange={e => setUsername(e.target.value)}
                                placeholder="Enter your username"
                                className="w-full bg-gray-800 text-white rounded-lg px-4 py-3
                           border border-gray-700 focus:outline-none
                           focus:border-indigo-500 placeholder-gray-600 text-sm"
                            />
                        </div>

                        <div>
                            <label className="block text-sm text-gray-400 mb-1">Password</label>
                            <input
                                type="password"
                                value={password}
                                onChange={e => setPassword(e.target.value)}
                                placeholder="Enter your password"
                                className="w-full bg-gray-800 text-white rounded-lg px-4 py-3
                           border border-gray-700 focus:outline-none
                           focus:border-indigo-500 placeholder-gray-600 text-sm"
                            />
                        </div>

                        {error && (
                            <p className="text-red-400 text-sm">{error}</p>
                        )}

                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50
                         text-white font-semibold py-3 rounded-lg transition-colors"
                        >
                            {loading ? "Signing in..." : "Sign In"}
                        </button>

                    </form>
                </div>

                <p className="text-center text-gray-600 text-xs mt-6">
                    v2.0 — Cloud + Desktop Hybrid
                </p>
            </div>
        </div>
    )
}