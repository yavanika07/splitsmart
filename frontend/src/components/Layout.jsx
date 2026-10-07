import { Link, Outlet } from 'react-router-dom'
import { useAuth } from '../auth.jsx'
import Logo from './Logo.jsx'

export default function Layout() {
  const { user, logout } = useAuth()
  return (
    <div className="app">
      <header className="topbar">
        <Link to="/" className="brand">
          <Logo /> SplitSmart
        </Link>
        <div className="topbar-right">
          <span className="muted hide-sm">Hi, {user?.name?.split(' ')[0]}</span>
          <button className="btn btn-ghost btn-sm" onClick={logout}>Log out</button>
        </div>
      </header>
      <main className="container">
        <Outlet />
      </main>
    </div>
  )
}
