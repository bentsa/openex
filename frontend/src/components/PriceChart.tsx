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
        <h3>BTC-USD Price</h3>
        <div className="orderbook-empty">Loading market data...</div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="price-chart">
        <h3>BTC-USD Price</h3>
        <p style={{ color: "red" }}>{error}</p>
      </div>
    )
  }

  const labels = ticks.map((t) =>
    new Date(t.timestamp).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" })
  )

  const data = {
    labels,
    datasets: [
      {
        label: "Price",
        data: ticks.map((t) => t.price),
        borderColor: "#2563eb",
        backgroundColor: "transparent",
        pointRadius: 0,
        borderWidth: 2,
        tension: 0.1,
      },
      {
        label: "MA 10",
        data: ticks.map((t) => t.ma_10),
        borderColor: "#16a34a",
        backgroundColor: "transparent",
        pointRadius: 0,
        borderWidth: 1,
        borderDash: [4, 4],
        tension: 0.1,
      },
      {
        label: "MA 50",
        data: ticks.map((t) => t.ma_50),
        borderColor: "#dc2626",
        backgroundColor: "transparent",
        pointRadius: 0,
        borderWidth: 1,
        borderDash: [4, 4],
        tension: 0.1,
      },
    ],
  }

  const options = {
    responsive: true,
    animation: false as const,
    plugins: {
      legend: { position: "top" as const },
    },
    scales: {
      x: { ticks: { maxTicksLimit: 8 } },
      y: { ticks: { callback: (value: string | number) => `$${value}` } },
    },
  }

  return (
    <div className="price-chart">
      <h3>BTC-USD Price</h3>
      <Line data={data} options={options} />
    </div>
  )
}
