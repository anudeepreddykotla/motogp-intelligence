import { useQuery } from '@tanstack/react-query'
import { NavLink, Route, Routes } from 'react-router-dom'

import { api } from './api'
import './styles.css'

function StatusCard({ label, value, detail }: { label: string; value: string; detail: string }) {
  return (
    <article className="status-card">
      <span className="eyebrow">{label}</span>
      <strong>{value}</strong>
      <span className="muted">{detail}</span>
    </article>
  )
}

function Dashboard() {
  const backend = useQuery({ queryKey: ['system-status'], queryFn: api.systemStatus })
  const python = useQuery({ queryKey: ['python-health'], queryFn: api.pythonHealth })

  return (
    <main className="page">
      <section className="hero">
        <p className="eyebrow">PHASE 0 FOUNDATION</p>
        <h1>MotoGP Intelligence</h1>
        <p className="lede">A clear starting point for trusted race intelligence.</p>
      </section>
      <section className="status-grid" aria-label="Service status">
        {backend.isPending && <StatusCard label="Java core" value="Loading" detail="Checking backend health…" />}
        {backend.isError && <StatusCard label="Java core" value="Unavailable" detail="Backend health could not be reached." />}
        {backend.data && <StatusCard label="Java core" value={backend.data.status} detail="Public API foundation" />}
        {python.isPending && <StatusCard label="Python AI" value="Loading" detail="Checking internal contract…" />}
        {python.isError && <StatusCard label="Python AI" value="Unavailable" detail="AI service is not available yet." />}
        {python.data && <StatusCard label="Python AI" value={python.data.status} detail={`Contract ${python.data.contractVersion}`} />}
      </section>
      <section className="notice">
        <h2>No race data loaded</h2>
        <p>Historical, live, news, prediction, and AI features are intentionally unavailable during Phase 0.</p>
      </section>
    </main>
  )
}

function Unavailable({ title }: { title: string }) {
  return (
    <main className="page">
      <p className="eyebrow">NOT AVAILABLE IN PHASE 0</p>
      <h1>{title}</h1>
      <p className="lede">This capability will be enabled only when its approved backend contract and real data source are implemented.</p>
    </main>
  )
}

export function App() {
  return (
    <div className="app-shell">
      <header className="topbar">
        <NavLink className="brand" to="/">MotoGP Intelligence</NavLink>
        <nav aria-label="Primary navigation">
          <NavLink to="/">Overview</NavLink>
          <NavLink to="/races">Races</NavLink>
          <NavLink to="/news">News</NavLink>
          <NavLink to="/ai">AI Engineer</NavLink>
        </nav>
      </header>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/races" element={<Unavailable title="Historical races" />} />
        <Route path="/news" element={<Unavailable title="MotoGP news" />} />
        <Route path="/ai" element={<Unavailable title="AI Race Engineer" />} />
        <Route path="*" element={<Unavailable title="Page not found" />} />
      </Routes>
    </div>
  )
}
