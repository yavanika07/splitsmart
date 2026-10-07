import Logo from '../components/Logo.jsx'

// Shared two-panel layout for the login and register pages
export default function AuthShell({ title, subtitle, children }) {
  return (
    <div className="auth">
      <aside className="auth-side">
        <div className="brand brand-light"><Logo /> SplitSmart</div>
        <div>
          <p className="auth-quote">Trips, flats, dinners.<br />Settle up in the fewest payments.</p>
          <div className="auth-demo">
            <div className="demo-row"><span>Kiran</span><span className="demo-arrow">→</span><span>Asha</span><b>₹400.00</b></div>
            <div className="demo-row"><span>Ravi</span><span className="demo-arrow">→</span><span>Asha</span><b>₹100.00</b></div>
            <p className="demo-note">5 expenses, 3 friends, just 2 payments.</p>
          </div>
        </div>
        <p className="auth-foot">Built with Spring Boot, React and PostgreSQL</p>
      </aside>
      <section className="auth-main">
        <div className="auth-card">
          <h1>{title}</h1>
          <p className="muted">{subtitle}</p>
          {children}
        </div>
      </section>
    </div>
  )
}
