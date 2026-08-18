import { BrowserRouter, Routes, Route, Link } from 'react-router-dom'
import Dashboard from './pages/Dashboard'
import Trading from './pages/Trading'
import Login from './pages/Login'
import ChatWidget from './components/ChatWidget'

function NavBar() {
  return (
    <nav style={{ display: 'flex', gap: '1rem', padding: '1rem', borderBottom: '1px solid #ccc' }}>
      <Link to="/">Dashboard</Link>
      <Link to="/trading">Trading</Link>
      <Link to="/login">Login</Link>
    </nav>
  )
}

function App() {
  return (
    <BrowserRouter>
      <NavBar />
      <div style={{ padding: '1rem' }}>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/trading" element={<Trading />} />
          <Route path="/login" element={<Login />} />
        </Routes>
      </div>
      <ChatWidget />
    </BrowserRouter>
  )
}

export default App