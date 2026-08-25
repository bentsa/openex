import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { loginUser, registerUser } from '../api/auth'
import { useAuthStore } from '../store/authStore'

const WATCHLIST = [
  { symbol: 'BTC/USD', price: '63,265.40', change: '+3.42%', positive: true },
  { symbol: 'ETH/USD', price: '3,184.12', change: '+1.85%', positive: true },
  { symbol: 'SOL/USD', price: '142.07', change: '-0.64%', positive: false },
]

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
                placeholder="********"
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
          <span className="auth-visual-eyebrow">OpenEx Terminal</span>
          <h2 className="auth-visual-heading">
            Trade with the precision of a real exchange.
          </h2>
          <p className="auth-visual-subheading">
            A live matching engine, real-time order books over WebSockets, and an
            AI trading assistant that reads your actual portfolio - built to feel
            like the desk, not a demo.
          </p>

          <ul className="auth-feature-list">
            <li>Real-time price feeds and order book depth</li>
            <li>Limit and market orders against a live matching engine</li>
            <li>AI assistant with direct wallet and trade-history access</li>
          </ul>

          <div className="auth-watchlist">
            <div className="auth-watchlist-header">
              <span>Market</span>
              <span>Last</span>
              <span>24h</span>
            </div>
            {WATCHLIST.map((row) => (
              <div className="auth-watchlist-row" key={row.symbol}>
                <span className="auth-watchlist-symbol">{row.symbol}</span>
                <span className="auth-watchlist-price">${row.price}</span>
                <span className={`auth-watchlist-change ${row.positive ? 'positive' : 'negative'}`}>
                  {row.change}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
