import { Component } from "react"

export default class ErrorBoundary extends Component {
    constructor(props) {
        super(props)
        this.state = { hasError: false, error: null }
    }

    static getDerivedStateFromError(error) {
        return { hasError: true, error }
    }

    render() {
        if (this.state.hasError) {
            return (
                <div className="flex flex-col items-center justify-center h-64 space-y-3">
                    <p className="text-4xl">⚠️</p>
                    <p className="text-white font-semibold">Something went wrong</p>
                    <p className="text-gray-500 text-sm">{this.state.error?.message}</p>
                    <button
                        onClick={() => this.setState({ hasError: false, error: null })}
                        className="text-sm text-indigo-400 hover:text-indigo-300 mt-2"
                    >
                        Try again
                    </button>
                </div>
            )
        }
        return this.props.children
    }
}