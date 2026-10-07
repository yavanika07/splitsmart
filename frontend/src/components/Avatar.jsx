import { initials } from '../format.js'

// Same name always gets the same colour
const COLORS = ['#0f766e', '#c2410c', '#7c3aed', '#be185d', '#1d4ed8', '#4d7c0f', '#a16207', '#0e7490']

export default function Avatar({ name, size = 32 }) {
  const hash = [...(name || '')].reduce((h, c) => (h * 31 + c.charCodeAt(0)) >>> 0, 7)
  return (
    <span
      className="avatar"
      style={{ width: size, height: size, fontSize: size * 0.4, background: COLORS[hash % COLORS.length] }}
      title={name}
    >
      {initials(name)}
    </span>
  )
}
