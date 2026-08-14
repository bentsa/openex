import { apiFetch } from './client'

export type OrderSide = 'BUY' | 'SELL'
export type OrderType = 'LIMIT' | 'MARKET'
export type OrderStatus = 'OPEN' | 'PARTIALLY_FILLED' | 'FILLED' | 'CANCELLED'

export interface CreateOrderRequest {
  accountId: string
  side: OrderSide
  orderType: OrderType
  price?: number
  quantity: number
}

export interface OrderResponse {
  id: string
  accountId: string
  side: OrderSide
  orderType: OrderType
  price: number | null
  quantity: number
  filledQuantity: number
  status: OrderStatus
}

export function createOrder(request: CreateOrderRequest): Promise<OrderResponse> {
  const idempotencyKey = crypto.randomUUID()

  return apiFetch('/orders', {
    method: 'POST',
    headers: {
      'Idempotency-Key': idempotencyKey,
    },
    body: JSON.stringify(request),
  })
}