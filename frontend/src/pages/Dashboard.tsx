import { useEffect, useState } from 'react'
import { getWallets, type WalletBalance } from '../api/wallet'
import { useAuthStore } from '../store/authStore'

export default function Dashboard() {
  const [wallets, setWallets] = useState<WalletBalance[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const email = useAuthStore((state) => state.email)
  const clearAuth = useAuthStore((state) => state.clearAuth)

  useEffect(() => {
    getWallets()
      .then(setWallets)
      .catch((err) => {
        setError(err instanceof Error ? err.message : 'Failed to load accounts')
      })
      .finally(() => setLoading(false))
  }, [])

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Dashboard</h1>
        <p className="page-subtitle">
          {email ? `Logged in as ${email}` : 'Your account overview'}
        </p>
      </div>

      {loading && <p style={{ color: 'var(--text-dim)' }}>Loading balances...</p>}
      {error && <p className="form-message error">{error}</p>}

      {!loading && !error && (
        <div className="stat-grid">
          {wallets.map((wallet, i) => (
            <div className={`stat-card${i === 0 ? ' primary' : ''}`} key={wallet.accountId}>
              <span className="stat-card-label">{wallet.currency} Balance</span>
              <span className="stat-card-value">{wallet.balance}</span>
            </div>
          ))}
        </div>
      )}

      <button className="btn" onClick={clearAuth}>
        Log out
      </button>
    </div>
  )
}
