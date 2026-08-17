from flask import Flask, jsonify
import numpy as np
import pandas as pd
from datetime import datetime, timedelta

app = Flask(__name__)

# --- Simulated market data generator ---

def generate_market_data(num_ticks: int = 200, start_price: float = 50000.0, drift: float = 0.0002, volatility: float = 0.01) -> pd.DataFrame:
    """
    Generates a simulated price series using a random walk with drift.
    Each tick represents one time step; prices move by a small random
    percentage each step, nudged slightly upward or downward by `drift`.
    """
    np.random.seed()  # non-deterministic each call, so /market-data looks "live"

    returns = np.random.normal(loc=drift, scale=volatility, size=num_ticks)
    price_multipliers = np.cumprod(1 + returns)
    prices = start_price * price_multipliers

    now = datetime.utcnow()
    timestamps = [now - timedelta(seconds=(num_ticks - i)) for i in range(num_ticks)]

    df = pd.DataFrame({
        "timestamp": timestamps,
        "price": prices
    })

    # Moving averages — common technical indicators for a trading UI
    df["ma_10"] = df["price"].rolling(window=10).mean()
    df["ma_50"] = df["price"].rolling(window=50).mean()

    return df


@app.route("/api/market-data", methods=["GET"])
def market_data():
    df = generate_market_data()

    result = {
        "symbol": "BTC-USD",
        "ticks": [
            {
                "timestamp": row["timestamp"].isoformat() + "Z",
                "price": round(row["price"], 2) if not pd.isna(row["price"]) else None,
                "ma_10": round(row["ma_10"], 2) if not pd.isna(row["ma_10"]) else None,
                "ma_50": round(row["ma_50"], 2) if not pd.isna(row["ma_50"]) else None,
            }
            for _, row in df.iterrows()
        ]
    }

    return jsonify(result)


@app.route("/api/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001, debug=True)