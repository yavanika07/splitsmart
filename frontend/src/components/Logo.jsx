export default function Logo({ size = 28 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true">
      <rect width="32" height="32" rx="8" fill="var(--accent)" />
      <path d="M9 21l14-10M9 11h6M17 21h6" stroke="white" strokeWidth="3" strokeLinecap="round" />
    </svg>
  )
}
