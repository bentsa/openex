import { useEffect, useState } from 'react'
import { getWallets, deposit, type WalletBalance } from '../api/wallet'
import { getOrders, type OrderResponse } from '../api/orders'
import { useAuthStore } from '../store/authStore'

export default function Dashboard() {
  const [wallets, setWallets] = useState<WalletBalance[]>([])
  const [orders, setOrders] = useState<OrderResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [depositCurrency, setDepositCurrency] = useState('USD')
  const [depositAmount, setDepositAmount] = useState('')
  const [depositMessage, setDepositMessage] = useState<string | null>(null)
  const [depositing, setDepositing] = useState(false)

  const email = useAuthStore((state) => state.email)
  const clearAuth = useAuthStore((state) => state.clearAuth)

  async function loadDashboardData() {
    setLoading(true)
    setError(null)
    try {
      const walletData = await getWallets()
      setWallets(walletData)

      const allOrders = await Promise.all(
        walletData.map((w) => getOrders(w.accountId).catch(() => []))
      )
      const merged = allOrders.flat().sort((a, b) => b.id.localeCompare(a.id))
      setOrders(merged)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load account data')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadDashboardData()
  }, [])

  async function handleDeposit(e: React.FormEvent) {
    e.preventDefault()
    setDepositMessage(null)

    const wallet = wallets.find((w) => w.currency === depositCurrency)
    if (!wallet) {
      setDepositMessage('No account found for that currency yet.')
      return
    }

    const amount = parseFloat(depositAmount)
    if (!amount || amount <= 0) {
      setDepositMessage('Enter a valid amount.')
      return
    }

    setDepositing(true)
    try {
      await deposit({ accountId: wallet.accountId, amount })
      setDepositAmount('')
      setDepositMessage(`Deposited ${amount} ${depositCurrency}.`)
      await loadDashboardData()
    } catch (err) {
      setDepositMessage(err instanceof Error ? err.message : 'Deposit failed')
    } finally {
      setDepositing(false)
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Dashboard</h1>
        <p className="page-subtitle">
          {email ? `Logged in as ${email}` : 'Your account overview'}
        </p>
      </div>

      {loading && <p style={{ color: 'var(--text-dim)' }}>Loading account data...</p>}
      {error && <p className="form-message error">{error}</p>}

      {!loading && !error && (
        <>
          <div className="stat-grid">
            {wallets.map((wallet, i) => (
              <div className={`stat-card${i === 0 ? ' primary' : ''}`} key={wallet.accountId}>
                <span className="stat-card-label">{wallet.currency} Balance</span>
                <span className="stat-card-value">{wallet.balance}</span>
              </div>
            ))}
          </div>

          <div className="card" style={{ marginTop: '1.5rem' }}>
            <h2 className="section-title">Deposit funds</h2>
            <form onSubmit={handleDeposit} className="deposit-form" style={{ display: 'flex', gap: '1rem', alignItems: 'flex-end', flexWrap: 'wrap' }}>
              <div>
                <label>Currency</label>
                <select value={depositCurrency} onChange={(e) => setDepositCurrency(e.target.value)}>
                  {wallets.map((w) => (
                    <option key={w.currency} value={w.currency}>{w.currency}</option>
                  ))}
                </select>
              </div>
              <div>
                <label>Amount</label>
                <input
                  type="number"
                  min="0"
                  step="any"
                  value={depositAmount}
                  onChange={(e) => setDepositAmount(e.target.value)}
                  placeholder="0.00"
                />
              </div>
              <button className="btn" type="submit" disabled={depositing}>
                {depositing ? 'Depositing...' : 'Deposit'}
              </button>
            </form>
            {depositMessage && <p className="form-message">{depositMessage}</p>}
          </div>

          <div className="card" style={{ marginTop: '1.5rem' }}>
            <h2 className="section-title">Trade history</h2>
            {orders.length === 0 && (
              <p style={{ color: 'var(--text-dim)' }}>No orders yet.</p>
            )}
            {orders.length > 0 && (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Side</th>
                    <th>Type</th>
                    <th>Quantity</th>
                    <th>Price</th>
                    <th>Filled</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {orders.map((order) => (
                    <tr key={order.id}>
                      <td className={order.side === 'BUY' ? 'text-green' : 'text-red'}>{order.side}</td>
                      <td>{order.orderType}</td>
                      <td>{order.quantity}</td>
                      <td>{order.price ?? 'Market'}</td>
                      <td>{order.filledQuantity}</td>
                      <td>{order.status}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}

      <button className="btn" onClick={clearAuth} style={{ marginTop: '1.5rem' }}>
        Log out
      </button>
    </div>
  )
}