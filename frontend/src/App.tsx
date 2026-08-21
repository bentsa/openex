import { BrowserRouter, Routes, Route, NavLink, Navigate } from 'react-router-dom'
import './App.css'
import Dashboard from './pages/Dashboard'
import Trading from './pages/Trading'
import Login from './pages/Login'
import ChatWidget from './components/ChatWidget'
import { useAuthStore } from './store/authStore'

function Sidebar() {
  const email = useAuthStore((state) => state.email)
  const clearAuth = useAuthStore((state) => state.clearAuth)

  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <div className="sidebar-logo-mark">Ox</div>
        <span className="sidebar-logo-text">OpenEx</span>
      </div>

      <nav className="sidebar-nav">
        <NavLink to="/" end className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`}>
          <span className="sidebar-link-icon">◈</span>
          Dashboard
        </NavLink>
        <NavLink to="/trading" className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`}>
          <span className="sidebar-link-icon">⇄</span>
          Trading
        </NavLink>
      </nav>

      {email && (
        <div className="sidebar-footer">
          <div className="sidebar-user">{email}</div>
          <button type="button" className="sidebar-logout" onClick={clearAuth}>
            Log out
          </button>
        </div>
      )}
    </aside>
  )
}

// Wraps pages that require the user to be logged in.
// If there's no email in the auth store, bounce to /login instead of rendering anything —
// including the sidebar, so it never appears before you've logged in.
function RequireAuth({ children }: { children: React.ReactNode }) {
  const email = useAuthStore((state) => state.email)
  if (!email) {
    return <Navigate to="/login" replace />
  }
  return <>{children}</>
}

// Wraps the login page. If the user is already logged in, there's no reason
// to show them the login form again — send them straight to the dashboard.
function RedirectIfAuthed({ children }: { children: React.ReactNode }) {
  const email = useAuthStore((state) => state.email)
  if (email) {
    return <Navigate to="/" replace />
  }
  return <>{children}</>
}

function AuthedLayout() {
  return (
    <RequireAuth>
      <div className="app-shell">
        <Sidebar />
        <main className="main-area">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/trading" element={<Trading />} />
          </Routes>
        </main>
      </div>
    </RequireAuth>
  )
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/login"
          element={
            <RedirectIfAuthed>
              <Login />
            </RedirectIfAuthed>
          }
        />
        <Route path="/*" element={<AuthedLayout />} />
      </Routes>
      <ChatWidgetGate />
    </BrowserRouter>
  )
}

// Only show the floating chat widget once the user is logged in —
// it doesn't make sense to offer AI trading help on the login screen.
function ChatWidgetGate() {
  const email = useAuthStore((state) => state.email)
  if (!email) return null
  return <ChatWidget />
}

export default App
