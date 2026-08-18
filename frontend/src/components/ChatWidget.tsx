import { useState, useRef, useEffect } from "react"
import { sendChatMessage } from "../api/chat"

interface Message {
  role: "user" | "assistant"
  text: string
}

export default function ChatWidget() {
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState<Message[]>([
    { role: "assistant", text: "Hi! Ask me about trading concepts or your wallet balance." },
  ])
  const [input, setInput] = useState("")
  const [sending, setSending] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [messages, open])

  async function handleSend(e: React.FormEvent) {
    e.preventDefault()
    const text = input.trim()
    if (!text || sending) return

    setMessages((prev) => [...prev, { role: "user", text }])
    setInput("")
    setSending(true)

    try {
      const res = await sendChatMessage(text)
      setMessages((prev) => [...prev, { role: "assistant", text: res.response }])
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Something went wrong."
      setMessages((prev) => [...prev, { role: "assistant", text: `Error: ${msg}` }])
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="chat-widget">
      {open && (
        <div className="chat-panel">
          <div className="chat-panel-header">
            <span>AI Trading Assistant</span>
            <button type="button" onClick={() => setOpen(false)} className="chat-close-btn">
              &times;
            </button>
          </div>

          <div className="chat-messages">
            {messages.map((m, i) => (
              <div key={i} className={`chat-message ${m.role}`}>
                {m.text}
              </div>
            ))}
            {sending && <div className="chat-message assistant chat-typing">Thinking...</div>}
            <div ref={messagesEndRef} />
          </div>

          <form onSubmit={handleSend} className="chat-input-row">
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Ask about your balance or trading..."
              disabled={sending}
            />
            <button type="submit" disabled={sending || !input.trim()}>
              Send
            </button>
          </form>
        </div>
      )}

      <button type="button" className="chat-toggle-btn" onClick={() => setOpen((o) => !o)}>
        {open ? "Close" : "Chat"}
      </button>
    </div>
  )
}
