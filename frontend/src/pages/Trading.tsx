import { useEffect, useState } from 'react'
import { createOrder, type OrderSide, type OrderType } from '../api/orders'
import { getWallets, type WalletBalance } from '../api/wallet'
import OrderBook from '../components/OrderBook'
import PriceChart from '../components/PriceChart'

export default function Trading() {
  const [wallets, setWallets] = useState<WalletBalance[]>([])
  const [accountId, setAccountId] = useState('')
  const [side, setSide] = useState<OrderSide>('BUY')
  const [orderType, setOrderType] = useState<OrderType>('LIMIT')
  const [quantity, setQuantity] = useState('')
  const [price, setPrice] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getWallets()
      .then((data) => {
        setWallets(data)
        if (data.length > 0) {
          setAccountId(data[0].accountId)
        }
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : 'Failed to load accounts')
      })
  }, [])

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setMessage(null)
    setSubmitting(true)

    try {
      const order = await createOrder({
        accountId,
        side,
        orderType,
        quantity: Number(quantity),
        price: orderType === 'LIMIT' ? Number(price) : undefined,
      })

      setMessage(`Order placed: ${order.side} ${order.quantity} (${order.status})`)
      setQuantity('')
      setPrice('')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to place order')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Trading</h1>
        <p className="page-subtitle">Place limit and market orders on BTC-USD</p>
      </div>

      <div style={{ display: 'flex', gap: '1.5rem', alignItems: 'flex-start', flexWrap: 'wrap' }}>
        <div className="card order-form-card" style={{ width: '280px', flexShrink: 0 }}>
          <div className="card-title">New Order</div>

          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '0.7rem' }}>
            <label className="field">
              Account
              <select value={accountId} onChange={(e) => setAccountId(e.target.value)} required>
                {wallets.map((w) => (
                  <option key={w.accountId} value={w.accountId}>
                    {w.currency} (balance: {w.balance})
                  </option>
                ))}
              </select>
            </label>

            <div className="btn-toggle-group">
              <button
                type="button"
                className={`btn-toggle buy${side === 'BUY' ? ' active' : ''}`}
                onClick={() => setSide('BUY')}
              >
                Buy
              </button>
              <button
                type="button"
                className={`btn-toggle sell${side === 'SELL' ? ' active' : ''}`}
                onClick={() => setSide('SELL')}
              >
                Sell
              </button>
            </div>

            <div className="btn-toggle-group">
              <button
                type="button"
                className={`btn-toggle${orderType === 'LIMIT' ? ' active' : ''}`}
                onClick={() => setOrderType('LIMIT')}
              >
                Limit
              </button>
              <button
                type="button"
                className={`btn-toggle${orderType === 'MARKET' ? ' active' : ''}`}
                onClick={() => setOrderType('MARKET')}
              >
                Market
              </button>
            </div>

            <label className="field">
              Quantity
              <input
                type="number"
                step="any"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                required
                placeholder="0.00"
              />
            </label>

            {orderType === 'LIMIT' && (
              <label className="field">
                Price
                <input
                  type="number"
                  step="any"
                  value={price}
                  onChange={(e) => setPrice(e.target.value)}
                  required
                  placeholder="0.00"
                />
              </label>
            )}

            {message && <p className="form-message success">{message}</p>}
            {error && <p className="form-message error">{error}</p>}

            <button type="submit" className="btn btn-primary" disabled={submitting || !accountId}>
              {submitting ? 'Placing order...' : `Place ${side === 'BUY' ? 'Buy' : 'Sell'} Order`}
            </button>
          </form>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', flex: 1, minWidth: '320px' }}>
          <PriceChart />
          <div className="card">
            <OrderBook />
          </div>
        </div>
      </div>
    </div>
  )
}
