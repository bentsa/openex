import { useEffect, useState, useRef } from "react"
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
} from "chart.js"
import { Line } from "react-chartjs-2"
import { getMarketData, type MarketTick } from "../api/marketData"

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend)

const POLL_INTERVAL_MS = 5000

export default function PriceChart() {
  const [symbol, setSymbol] = useState("BTC-USD")
  const [ticks, setTicks] = useState<MarketTick[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const intervalRef = useRef<number | null>(null)

  useEffect(() => {
    let cancelled = false
    async function fetchData() {
      try {
        const data = await getMarketData()
        if (!cancelled) {
          setTicks(data.ticks)
          setSymbol(data.symbol)
          setError(null)
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "Failed to load market data")
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    fetchData()
    intervalRef.current = window.setInterval(fetchData, POLL_INTERVAL_MS)
    return () => {
      cancelled = true
      if (intervalRef.current) window.clearInterval(intervalRef.current)
    }
  }, [])

  if (loading) {
    return (
      <div className="price-chart">
        <h3>{symbol} Price</h3>
        <div className="orderbook-empty">Loading market data...</div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="price-chart">
        <h3>{symbol} Price</h3>
        <p className="form-message error">{error}</p>
      </div>
    )
  }

  const pricedTicks = ticks.filter((t) => t.price !== null)
  const latestPrice = pricedTicks.length > 0 ? pricedTicks[pricedTicks.length - 1].price! : null
  const firstPrice = pricedTicks.length > 0 ? pricedTicks[0].price! : null
  const change = latestPrice !== null && firstPrice !== null ? latestPrice - firstPrice : null
  const changePct = change !== null && firstPrice ? (change / firstPrice) * 100 : null
  const isUp = change !== null && change >= 0

  const labels = ticks.map((t) =>
    new Date(t.timestamp).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" })
  )

  const data = {
    labels,
    datasets: [
      {
        label: "Price",
        data: ticks.map((t) => t.price),
        borderColor: "#ff5fa2",
        backgroundColor: "rgba(255, 95, 162, 0.08)",
        fill: true,
        pointRadius: 0,
        borderWidth: 2,
        tension: 0.25,
      },
      {
        label: "MA 10",
        data: ticks.map((t) => t.ma_10),
        borderColor: "#17c787",
        backgroundColor: "transparent",
        pointRadius: 0,
        borderWidth: 1,
        borderDash: [4, 4],
        tension: 0.25,
      },
      {
        label: "MA 50",
        data: ticks.map((t) => t.ma_50),
        borderColor: "#8b5cf6",
        backgroundColor: "transparent",
        pointRadius: 0,
        borderWidth: 1,
        borderDash: [4, 4],
        tension: 0.25,
      },
    ],
  }

  const options = {
    responsive: true,
    animation: false as const,
    plugins: {
      legend: {
        position: "top" as const,
        labels: { color: "#8891a8", boxWidth: 12, font: { size: 11 } },
      },
    },
    scales: {
      x: {
        ticks: { maxTicksLimit: 8, color: "#565d75", font: { size: 10 } },
        grid: { color: "#242840" },
      },
      y: {
        ticks: {
          color: "#565d75",
          font: { size: 10 },
          callback: (value: string | number) => `$${value}`,
        },
        grid: { color: "#242840" },
      },
    },
  }

  return (
    <div className="price-chart">
      <div className="price-ticker-header">
        <h3>{symbol}</h3>
        {latestPrice !== null && (
          <div className="price-ticker">
            <span className="price-ticker-value">
              ${latestPrice.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </span>
            {change !== null && changePct !== null && (
              <span className={`price-ticker-change ${isUp ? "positive" : "negative"}`}>
                {isUp ? "\u25B2" : "\u25BC"} {Math.abs(change).toFixed(2)} ({Math.abs(changePct).toFixed(2)}%)
              </span>
            )}
          </div>
        )}
      </div>
      <Line data={data} options={options} />
    </div>
  )
}