"""
Tests for the OpenEx AI service (ai-service/app.py).

These tests mock both the Kotlin wallets API and the Ollama LLM call so they
run fast and do not require the full stack (Docker/Kotlin/Ollama) to be up.
Run with: python -m pytest test_app.py -v
"""
import json
from unittest.mock import patch, MagicMock

import pytest

from app import app


@pytest.fixture
def client():
    app.config["TESTING"] = True
    with app.test_client() as client:
        yield client


def test_health_check(client):
    """Sanity check that the service is up."""
    response = client.get("/api/health")
    assert response.status_code == 200
    assert response.get_json() == {"status": "ok"}


def test_chat_requires_message_field(client):
    """POSTing without a 'message' field should return 400, not crash."""
    response = client.post(
        "/api/chat",
        data=json.dumps({}),
        content_type="application/json",
    )
    assert response.status_code == 400
    assert "error" in response.get_json()


@patch("app.ChatOllama")
@patch("app.requests.get")
def test_chat_fetches_and_quotes_real_balance(mock_get, mock_chat_ollama, client):
    """
    Core Day 13 requirement: when the user asks about their balance, the
    endpoint should call the Kotlin wallets API and pass the REAL balance
    data into the LLM prompt instead of letting the LLM guess.
    """
    mock_kotlin_response = MagicMock()
    mock_kotlin_response.json.return_value = [
        {"accountId": "a07a7caa-...", "currency": "USD", "balance": 0},
        {"accountId": "6f35ab95-...", "currency": "BTC", "balance": 3.0},
    ]
    mock_kotlin_response.raise_for_status.return_value = None
    mock_get.return_value = mock_kotlin_response

    mock_llm_instance = MagicMock()
    mock_llm_instance.invoke.return_value = MagicMock(
        content="Your current balance is 0 USD and 3.0 BTC."
    )
    mock_chat_ollama.return_value = mock_llm_instance

    response = client.post(
        "/api/chat",
        data=json.dumps({"message": "What's my current wallet balance?"}),
        content_type="application/json",
        headers={"Authorization": "Bearer fake-test-token"},
    )

    assert response.status_code == 200
    body = response.get_json()
    assert "3.0" in body["response"] or "3" in body["response"]

    mock_get.assert_called_once()
    call_args = mock_get.call_args
    assert "/api/wallets" in call_args[0][0]
    assert call_args[1]["headers"]["Authorization"] == "Bearer fake-test-token"

    prompt_sent_to_llm = mock_llm_instance.invoke.call_args[0][0]
    assert "3.0" in prompt_sent_to_llm
    assert "REAL WALLET DATA" in prompt_sent_to_llm


@patch("app.ChatOllama")
def test_chat_skips_wallet_fetch_for_unrelated_questions(mock_chat_ollama, client):
    """Non-balance questions should not trigger a call to the Kotlin API at all."""
    mock_llm_instance = MagicMock()
    mock_llm_instance.invoke.return_value = MagicMock(
        content="A limit order lets you set your own price."
    )
    mock_chat_ollama.return_value = mock_llm_instance

    with patch("app.requests.get") as mock_get:
        response = client.post(
            "/api/chat",
            data=json.dumps({"message": "What is a limit order?"}),
            content_type="application/json",
            headers={"Authorization": "Bearer fake-test-token"},
        )
        assert response.status_code == 200
        mock_get.assert_not_called()


def test_chat_handles_missing_auth_header_gracefully(client):
    """If no Authorization header is sent for a balance question, do not crash."""
    with patch("app.ChatOllama") as mock_chat_ollama:
        mock_llm_instance = MagicMock()
        mock_llm_instance.invoke.return_value = MagicMock(
            content="I don't have access to your balance right now."
        )
        mock_chat_ollama.return_value = mock_llm_instance

        response = client.post(
            "/api/chat",
            data=json.dumps({"message": "What's my wallet balance?"}),
            content_type="application/json",
        )
        assert response.status_code == 200
