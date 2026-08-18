import { useAuthStore } from "../store/authStore"

const AI_SERVICE_BASE_URL = "http://localhost:5001/api"

export interface ChatResponse {
  response: string
}

export async function sendChatMessage(message: string): Promise<ChatResponse> {
  const token = useAuthStore.getState().token

  const response = await fetch(`${AI_SERVICE_BASE_URL}/chat`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ message }),
  })

  if (!response.ok) {
    throw new Error(`Chat error ${response.status}: ${response.statusText}`)
  }

  return response.json()
}
