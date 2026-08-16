import axios from 'axios'

const API = axios.create({
    baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
})

// Attach JWT to every request automatically
API.interceptors.request.use(config => {
    const token = localStorage.getItem('token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
})

// 401 = token expired/invalid → logout
// 403 = forbidden → show message, DO NOT logout
API.interceptors.response.use(
    response => response,
    error => {
        if (error.response?.status === 401) {
            localStorage.removeItem('token')
            localStorage.removeItem('user')
            window.location.href = '/login'
        }
        // 403 is handled per-page — do nothing globally
        return Promise.reject(error)
    }
)

export default API