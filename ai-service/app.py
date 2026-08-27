from flask import Flask, jsonify, request
from flask_cors import CORS
import numpy as np
import pandas as pd
from datetime import datetime, timedelta
from langchain_ollama import ChatOllama
from langchain_core.tools import tool
from langchain_core.messages import HumanMessage
from langgraph.prebuilt import create_react_agent
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


# --- AI trading assistant (Day 13: agentic tool calling against the Kotlin wallets API) ---
OLLAMA_MODEL = os.environ.get("OLLAMA_MODEL", "mistral")

# Bug fix: the OLLAMA_HOST env var set in docker-compose.yml (e.g.
# "http://ollama:11434") was previously never passed into ChatOllama, which
# defaults to localhost:11434. Inside Docker that would silently fail to
# reach the ollama container. base_url must be set explicitly.
OLLAMA_BASE_URL = os.environ.get("OLLAMA_HOST", "http://localhost:11434")

chat_llm = ChatOllama(model=OLLAMA_MODEL, base_url=OLLAMA_BASE_URL, temperature=0.2)
KOTLIN_API_BASE = os.environ.get("KOTLIN_API_BASE", "http://localhost:8080")

FINANCIAL_PERSONA = """You are a knowledgeable but cautious financial assistant for OpenEx,
a simulated crypto trading platform. You help users understand trading concepts, market
terminology, and general financial literacy. You do not give specific buy/sell advice or
guarantee outcomes - you educate and explain. Keep answers concise and clear.

You have a tool available called get_wallet_balance. Use it whenever the user asks about
their balance, funds, holdings, or how much of something they own. Never guess or make up
balance numbers - always call the tool to get the real figures first."""


def make_wallet_balance_tool(auth_header: str | None):
    """
    Builds the get_wallet_balance LangChain tool for a single request, closing
    over that request's Authorization header so the tool call is scoped to
    the authenticated user making the chat request (Day 13 requirement:
    "Register this function as a tool in LangChain so the LLM can securely
    use it to answer user questions about their balance").
    """

    @tool
    def get_wallet_balance() -> str:
        """Look up the authenticated user's current OpenEx wallet balances
        (USD and BTC). Call this whenever the user asks about their balance,
        funds, holdings, or how much they have."""
        if not auth_header:
            return "No auth token was provided, so wallet data is unavailable."

        try:
            resp = requests.get(
                f"{KOTLIN_API_BASE}/api/wallets",
                headers={"Authorization": auth_header},
                timeout=5,
            )
            resp.raise_for_status()
            wallets = resp.json()
            return "; ".join(f"{w['currency']}: {w['balance']}" for w in wallets)
        except requests.RequestException as e:
            return f"Error fetching wallet data: {e}"

    return get_wallet_balance


@app.route("/api/chat", methods=["POST"])
def chat():
    data = request.get_json()

    if not data or "message" not in data:
        return jsonify({"error": "Request body must include a 'message' field"}), 400

    user_message = data["message"]
    auth_header = request.headers.get("Authorization")

    wallet_tool = make_wallet_balance_tool(auth_header)
    agent = create_react_agent(model=chat_llm, tools=[wallet_tool], prompt=FINANCIAL_PERSONA)

    result = agent.invoke({"messages": [HumanMessage(content=user_message)]})
    final_message = result["messages"][-1]

    return jsonify({
        "response": final_message.content.strip()
    })


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5001))
    app.run(host="0.0.0.0", port=port, debug=False)