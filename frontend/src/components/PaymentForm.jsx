import { useState } from 'react'
import Modal from './Modal.jsx'

/** Record a payment that happened outside the app (e.g. a UPI transfer). */
export default function PaymentForm({ members, initial, onSave, onClose }) {
  const [fromId, setFromId] = useState(initial?.fromId ?? members[0]?.id)
  const [toId, setToId] = useState(initial?.toId ?? members[1]?.id)
  const [amount, setAmount] = useState(initial?.amount ? String(initial.amount) : '')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    if (Number(fromId) === Number(toId)) return setError('Payer and receiver must be different people')
    if (!(Number(amount) > 0)) return setError('Enter an amount greater than 0')
    setBusy(true)
    try {
      await onSave({ fromMemberId: Number(fromId), toMemberId: Number(toId), amount: Number(amount) })
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal title="Record a payment" onClose={onClose}>
      <form onSubmit={submit} className="stack">
        <div className="row">
          <label className="field grow">
            <span>Who paid</span>
            <select value={fromId} onChange={(e) => setFromId(Number(e.target.value))}>
              {members.map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}
            </select>
          </label>
          <label className="field grow">
            <span>Paid to</span>
            <select value={toId} onChange={(e) => setToId(Number(e.target.value))}>
              {members.map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}
            </select>
          </label>
        </div>
        <label className="field">
          <span>Amount (₹)</span>
          <input type="number" min="0.01" step="0.01" value={amount}
                 onChange={(e) => setAmount(e.target.value)} required autoFocus />
        </label>
        {error && <p className="error">{error}</p>}
        <div className="modal-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Record payment'}</button>
        </div>
      </form>
    </Modal>
  )
}
