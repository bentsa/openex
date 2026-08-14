import { useEffect, useState } from 'react'
import { createOrder, type OrderSide, type OrderType } from '../api/orders'
import { getWallets, type WalletBalance } from '../api/wallet'

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
    <div style={{ maxWidth: '320px' }}>
      <h1>Trading</h1>

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
        <label>
          Account
          <select
            value={accountId}
            onChange={(e) => setAccountId(e.target.value)}
            required
            style={{ display: 'block', width: '100%' }}
          >
            {wallets.map((w) => (
              <option key={w.accountId} value={w.accountId}>
                {w.currency} (balance: {w.balance})
              </option>
            ))}
          </select>
        </label>

        <div>
          <button type="button" onClick={() => setSide('BUY')} disabled={side === 'BUY'}>
            Buy
          </button>
          <button type="button" onClick={() => setSide('SELL')} disabled={side === 'SELL'} style={{ marginLeft: '0.5rem' }}>
            Sell
          </button>
        </div>

        <div>
          <button type="button" onClick={() => setOrderType('LIMIT')} disabled={orderType === 'LIMIT'}>
            Limit
          </button>
          <button type="button" onClick={() => setOrderType('MARKET')} disabled={orderType === 'MARKET'} style={{ marginLeft: '0.5rem' }}>
            Market
          </button>
        </div>

        <label>
          Quantity
          <input
            type="number"
            step="any"
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
            required
            style={{ display: 'block', width: '100%' }}
          />
        </label>

        {orderType === 'LIMIT' && (
          <label>
            Price
            <input
              type="number"
              step="any"
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              required
              style={{ display: 'block', width: '100%' }}
            />
          </label>
        )}

        {message && <p style={{ color: 'green' }}>{message}</p>}
        {error && <p style={{ color: 'red' }}>{error}</p>}

        <button type="submit" disabled={submitting || !accountId}>
          {submitting ? 'Placing order...' : 'Place Order'}
        </button>
      </form>
    </div>
  )
}