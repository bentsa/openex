import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { loginUser, registerUser } from '../api/auth'
import { useAuthStore } from '../store/authStore'

export default function Login() {
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const setAuth = useAuthStore((state) => state.setAuth)
  const navigate = useNavigate()

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const result = mode === 'login'
        ? await loginUser(email, password)
        : await registerUser(email, password)
      setAuth(result.token, result.email)
      navigate('/')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-shell">
      <div className="auth-panel-form">
        <div className="auth-card">
          <div className="auth-logo">
            <div className="sidebar-logo-mark">Ox</div>
            <span className="sidebar-logo-text">OpenEx</span>
          </div>

          <h1 className="auth-heading">
            {mode === 'login' ? 'Log in to your account' : 'Create your account'}
          </h1>
          <p className="auth-subheading">
            {mode === 'login'
              ? 'Enter your email and password to continue.'
              : 'Enter your details to get started.'}
          </p>

          <div className="btn-toggle-group">
            <button
              type="button"
              className={`btn-toggle${mode === 'login' ? ' active' : ''}`}
              onClick={() => setMode('login')}
            >
              Log in
            </button>
            <button
              type="button"
              className={`btn-toggle${mode === 'register' ? ' active' : ''}`}
              onClick={() => setMode('register')}
            >
              Register
            </button>
          </div>

          <form onSubmit={handleSubmit} className="auth-form">
            <label className="field">
              Email
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                placeholder="you@example.com"
              />
            </label>
            <label className="field">
              Password
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                placeholder="••••••••"
              />
            </label>

            {error && <p className="form-message error">{error}</p>}

            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Please wait...' : mode === 'login' ? 'Log in' : 'Create account'}
            </button>
          </form>
        </div>
      </div>

      <div className="auth-panel-visual">
        <div className="auth-visual-content">
          <h2 className="auth-visual-heading">
            The easiest way to trade your portfolio.
          </h2>
          <p className="auth-visual-subheading">
            Live prices, real-time order books, and an AI assistant — all in one terminal.
          </p>

          <div className="auth-preview-card">
            <div className="auth-preview-header">
              <span>BTC/USD</span>
              <span className="auth-preview-price">$63,265</span>
            </div>
            <svg className="auth-preview-sparkline" viewBox="0 0 240 60" preserveAspectRatio="none">
              <polyline
                points="0,40 20,35 40,42 60,28 80,32 100,20 120,25 140,15 160,22 180,10 200,18 220,8 240,14"
                fill="none"
                stroke="url(#sparkGradient)"
                strokeWidth="3"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
              <defs>
                <linearGradient id="sparkGradient" x1="0" y1="0" x2="1" y2="0">
                  <stop offset="0%" stopColor="#ff5fa2" />
                  <stop offset="100%" stopColor="#8b5cf6" />
                </linearGradient>
              </defs>
            </svg>
            <div className="auth-preview-stats">
              <div className="auth-preview-stat">
                <span className="auth-preview-stat-label">24h Change</span>
                <span className="auth-preview-stat-value positive">+3.42%</span>
              </div>
              <div className="auth-preview-stat">
                <span className="auth-preview-stat-label">Volume</span>
                <span className="auth-preview-stat-value">$1.2B</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
