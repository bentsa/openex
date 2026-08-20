const AI_SERVICE_BASE_URL = import.meta.env.VITE_AI_SERVICE_URL ?? "http://localhost:5001/api"

export interface MarketTick {
  timestamp: string
  price: number | null
  ma_10: number | null
  ma_50: number | null
}

export interface MarketDataResponse {
  symbol: string
  ticks: MarketTick[]
}

export async function getMarketData(): Promise<MarketDataResponse> {
  const response = await fetch(`${AI_SERVICE_BASE_URL}/market-data`)

  if (!response.ok) {
    throw new Error(`Market data error ${response.status}: ${response.statusText}`)
  }

  return response.json()
}
