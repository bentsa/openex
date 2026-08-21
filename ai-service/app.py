from flask import Flask, jsonify, request
from flask_cors import CORS
import numpy as np
import pandas as pd
from datetime import datetime, timedelta
from langchain_ollama import ChatOllama
import requests
import os

app = Flask(__name__)
CORS(app, origins=os.environ.get("CORS_ALLOWED_ORIGIN", "http://localhost:5173").split(","))

# --- Simulated market data generator ---

def generate_market_data(num_ticks: int = 200, start_price: float = 50000.0, drift: float = 0.0002, volatility: float = 0.01) -> pd.DataFrame:
    """
    Generates a simulated price series using a random walk with drift.
    """
    np.random.seed()

    returns = np.random.normal(loc=drift, scale=volatility, size=num_ticks)
    price_multipliers = np.cumprod(1 + returns)
    prices = start_price * price_multipliers

    now = datetime.utcnow()
    timestamps = [now - timedelta(seconds=(num_ticks - i)) for i in range(num_ticks)]

    df = pd.DataFrame({
        "timestamp": timestamps,
        "price": prices
    })

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


# --- AI trading assistant (Day 13: tool calling against the Kotlin wallets API) ---

KOTLIN_API_BASE = os.environ.get("KOTLIN_API_BASE", "http://localhost:8080")

FINANCIAL_PERSONA = """You are a knowledgeable but cautious financial assistant for OpenEx,
a simulated crypto trading platform. You help users understand trading concepts, market
terminology, and general financial literacy. You do not give specific buy/sell advice or
guarantee outcomes - you educate and explain. Keep answers concise and clear.

If [REAL WALLET DATA] is included below, use those exact numbers when answering
questions about the user's balance, holdings, or wallet. Do not make up numbers."""


@app.route("/api/chat", methods=["POST"])
def chat():
    data = request.get_json()

    if not data or "message" not in data:
        return jsonify({"error": "Request body must include a 'message' field"}), 400

    user_message = data["message"]
    auth_header = request.headers.get("Authorization")

    balance_keywords = ["balance", "wallet", "holdings", "funds", "how much"]
    needs_balance = any(kw in user_message.lower() for kw in balance_keywords)

    tool_context = ""
    if needs_balance:
        if not auth_header:
            tool_context = "\n\n[No auth token was provided, so real wallet data is unavailable.]"
        else:
            try:
                resp = requests.get(
                    f"{KOTLIN_API_BASE}/api/wallets",
                    headers={"Authorization": auth_header},
                    timeout=5,
                )
                resp.raise_for_status()
                wallets = resp.json()
                lines = [f"{w['currency']}: {w['balance']}" for w in wallets]
                tool_context = "\n\n[REAL WALLET DATA]\n" + "\n".join(lines)
            except requests.RequestException as e:
                tool_context = f"\n\n[Error fetching wallet data: {e}]"

    chat_llm = ChatOllama(model="mistral", temperature=0.2)
    full_prompt = FINANCIAL_PERSONA + tool_context + f"\n\nUser: {user_message}\nAssistant:"

    response = chat_llm.invoke(full_prompt)

    return jsonify({
        "response": response.content.strip()
    })


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5001))
    app.run(host="0.0.0.0", port=port, debug=False)