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
    let cancelled = false

    async function fetchWallets() {
      try {
        const data = await getWallets()
        if (!cancelled) {
          setWallets(data)
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to load wallets')
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    fetchWallets()

    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div>
      <h1>Dashboard</h1>
      {email && <p>Logged in as {email}</p>}

      {loading && <p>Loading balances...</p>}
      {error && <p style={{ color: 'red' }}>{error}</p>}

      {!loading && !error && (
        <ul style={{ listStyle: 'none', padding: 0 }}>
          {wallets.map((wallet) => (
            <li key={wallet.accountId} style={{ marginBottom: '0.5rem' }}>
              <strong>{wallet.currency}:</strong> {wallet.balance}
            </li>
          ))}
        </ul>
      )}

      <button onClick={clearAuth} style={{ marginTop: '1rem' }}>
        Log out
      </button>
    </div>
  )
}