import { useMemo, useState } from 'react'
import Modal from './Modal.jsx'
import { money, todayIso, toPaisa } from '../format.js'

const TYPES = [
  { value: 'EQUAL', label: 'Equally' },
  { value: 'EXACT', label: 'Exact amounts' },
  { value: 'PERCENTAGE', label: 'Percentages' },
]

// Splits `total` units into n whole-number parts, giving leftovers to the first people
const evenParts = (total, n) =>
  Array.from({ length: n }, (_, i) => Math.floor(total / n) + (i < total % n ? 1 : 0))

/** Add or edit an expense. `expense` is passed only when editing. */
export default function ExpenseForm({ members, expense, onSave, onClose }) {
  const [description, setDescription] = useState(expense?.description ?? '')
  const [amount, setAmount] = useState(expense ? String(expense.amount) : '')
  const [date, setDate] = useState(expense?.expenseDate ?? todayIso())
  const [paidById, setPaidById] = useState(expense?.paidBy.id ?? members[0]?.id)
  const [splitType, setSplitType] = useState(expense?.splitType ?? 'EQUAL')
  const [selected, setSelected] = useState(
    () => new Set(expense ? expense.splits.map((s) => s.memberId) : members.map((m) => m.id))
  )
  const [exact, setExact] = useState(() =>
    expense?.splitType === 'EXACT'
      ? Object.fromEntries(expense.splits.map((s) => [s.memberId, String(s.share)]))
      : {}
  )
  const [percent, setPercent] = useState(() =>
    expense?.splitType === 'PERCENTAGE'
      ? Object.fromEntries(expense.splits.map((s) => [s.memberId, String(Number(s.percent))]))
      : {}
  )
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const participants = members.filter((m) => selected.has(m.id))
  const totalPaisa = toPaisa(amount)

  const toggle = (memberId) => {
    const next = new Set(selected)
    next.has(memberId) ? next.delete(memberId) : next.add(memberId)
    setSelected(next)
  }

  // Live "how much is left to assign" hint
  const status = useMemo(() => {
    if (participants.length === 0) return { ok: false, text: 'Pick at least one person' }
    if (splitType === 'EQUAL') {
      return { ok: true, text: totalPaisa > 0 ? `${money(totalPaisa / 100 / participants.length)} each (approx.)` : '' }
    }
    if (splitType === 'EXACT') {
      const diff = totalPaisa - participants.reduce((s, m) => s + toPaisa(exact[m.id]), 0)
      if (diff === 0) return { ok: true, text: 'Adds up to the total' }
      return { ok: false, text: diff > 0 ? `${money(diff / 100)} left to assign` : `${money(-diff / 100)} too much` }
    }
    const diff = 10000 - participants.reduce((s, m) => s + Math.round(Number(percent[m.id] || 0) * 100), 0)
    if (diff === 0) return { ok: true, text: 'Adds up to 100%' }
    return { ok: false, text: diff > 0 ? `${diff / 100}% left` : `${-diff / 100}% too much` }
  }, [participants, splitType, totalPaisa, exact, percent])

  const fillEvenly = () => {
    const ids = participants.map((m) => m.id)
    if (splitType === 'EXACT') {
      const parts = evenParts(totalPaisa, ids.length)
      setExact(Object.fromEntries(ids.map((id, i) => [id, (parts[i] / 100).toFixed(2)])))
    } else {
      const parts = evenParts(10000, ids.length)
      setPercent(Object.fromEntries(ids.map((id, i) => [id, String(parts[i] / 100)])))
    }
  }

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    if (totalPaisa <= 0) return setError('Enter an amount greater than 0')
    if (!status.ok) return setError(status.text)

    const body = {
      description: description.trim(),
      amount: Number(amount),
      paidById: Number(paidById),
      splitType,
      expenseDate: date || null,
      participants: participants.map((m) => ({
        memberId: m.id,
        amount: splitType === 'EXACT' ? Number(exact[m.id] || 0) : null,
        percent: splitType === 'PERCENTAGE' ? Number(percent[m.id] || 0) : null,
      })),
    }
    setBusy(true)
    try {
      await onSave(body)
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal title={expense ? 'Edit expense' : 'Add expense'} onClose={onClose}>
      <form onSubmit={submit} className="stack">
        <label className="field">
          <span>Description</span>
          <input value={description} onChange={(e) => setDescription(e.target.value)}
                 placeholder="Dinner at the beach shack" required maxLength={120} autoFocus />
        </label>

        <div className="row">
          <label className="field grow">
            <span>Amount (₹)</span>
            <input type="number" inputMode="decimal" min="0.01" step="0.01" value={amount}
                   onChange={(e) => setAmount(e.target.value)} placeholder="0.00" required />
          </label>
          <label className="field grow">
            <span>Date</span>
            <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
          </label>
        </div>

        <label className="field">
          <span>Paid by</span>
          <select value={paidById} onChange={(e) => setPaidById(Number(e.target.value))}>
            {members.map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}
          </select>
        </label>

        <div className="field">
          <span>Split</span>
          <div className="segmented" role="radiogroup">
            {TYPES.map((t) => (
              <button type="button" key={t.value} role="radio" aria-checked={splitType === t.value}
                      className={splitType === t.value ? 'active' : ''}
                      onClick={() => setSplitType(t.value)}>
                {t.label}
              </button>
            ))}
          </div>
        </div>

        <div className="split-list">
          {members.map((m) => {
            const on = selected.has(m.id)
            return (
              <div key={m.id} className={`split-row ${on ? '' : 'off'}`}>
                <label className="check">
                  <input type="checkbox" checked={on} onChange={() => toggle(m.id)} />
                  <span>{m.name}</span>
                </label>
                {on && splitType === 'EXACT' && (
                  <input className="split-input" type="number" step="0.01" min="0" placeholder="0.00"
                         value={exact[m.id] ?? ''} aria-label={`${m.name}'s share`}
                         onChange={(e) => setExact({ ...exact, [m.id]: e.target.value })} />
                )}
                {on && splitType === 'PERCENTAGE' && (
                  <div className="pct">
                    <input className="split-input" type="number" step="0.01" min="0" max="100" placeholder="0"
                           value={percent[m.id] ?? ''} aria-label={`${m.name}'s percentage`}
                           onChange={(e) => setPercent({ ...percent, [m.id]: e.target.value })} />
                    <span>%</span>
                  </div>
                )}
              </div>
            )
          })}
        </div>

        <div className="split-status">
          <span className={status.ok ? 'ok' : 'warn'}>{status.text}</span>
          {splitType !== 'EQUAL' && participants.length > 0 && (
            <button type="button" className="link-btn" onClick={fillEvenly}>Fill evenly</button>
          )}
        </div>

        {error && <p className="error">{error}</p>}
        <div className="modal-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={busy}>
            {busy ? 'Saving…' : expense ? 'Save changes' : 'Add expense'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
