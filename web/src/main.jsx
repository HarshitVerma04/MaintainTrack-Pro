import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { Toaster } from 'react-hot-toast'
import './index.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <App />
        <Toaster
            position="bottom-right"
            toastOptions={{
                style: {
                    background: '#1f2937',
                    color: '#f9fafb',
                    border: '1px solid #374151',
                    borderRadius: '10px',
                    fontSize: '14px',
                },
                success: { iconTheme: { primary: '#22c55e', secondary: '#1f2937' } },
                error:   { iconTheme: { primary: '#ef4444', secondary: '#1f2937' } },
            }}
        />
    </StrictMode>
)