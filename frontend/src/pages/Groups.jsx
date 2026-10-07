import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { money } from '../format.js'
import Avatar from '../components/Avatar.jsx'
import Modal from '../components/Modal.jsx'

export default function Groups() {
  const [groups, setGroups] = useState(null)
  const [error, setError] = useState('')
  const [creating, setCreating] = useState(false)

  useEffect(() => {
    api.groups().then(setGroups).catch((e) => setError(e.message))
  }, [])

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Your groups</h1>
          <p className="muted">Each group keeps its own expenses and balances.</p>
        </div>
        <button className="btn btn-primary" onClick={() => setCreating(true)}>+ New group</button>
      </div>

      {error && <p className="error">{error}</p>}
      {groups === null && !error && <p className="muted">Loading…</p>}

      {groups?.length === 0 && (
        <div className="empty">
          <h3>No groups yet</h3>
          <p className="muted">Create one for a trip, your flat, or a dinner with friends.</p>
          <button className="btn btn-primary" onClick={() => setCreating(true)}>Create your first group</button>
        </div>
      )}

      <div className="group-grid">
        {groups?.map((g) => (
          <Link to={`/groups/${g.id}`} key={g.id} className="card group-card">
            <h3>{g.name}</h3>
            <div className="avatars">
              {g.members.slice(0, 5).map((m) => <Avatar key={m.id} name={m.name} size={28} />)}
              {g.members.length > 5 && <span className="more">+{g.members.length - 5}</span>}
            </div>
            <div className="group-card-foot">
              <span className="muted">{g.members.length} members</span>
              <span className="mono">{money(g.totalSpent)}</span>
            </div>
          </Link>
        ))}
      </div>

      {creating && <CreateGroupModal onClose={() => setCreating(false)} />}
    </>
  )
}

function CreateGroupModal({ onClose }) {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [members, setMembers] = useState([user?.name || 'Me'])
  const [newMember, setNewMember] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const addMember = () => {
    const n = newMember.trim()
    if (!n) return
    if (members.some((m) => m.toLowerCase() === n.toLowerCase())) {
      return setError(`${n} is already added`)
    }
    setMembers([...members, n])
    setNewMember('')
    setError('')
  }

  const submit = async (e) => {
    e.preventDefault()
    if (members.length < 2) return setError('Add at least 2 people to split with')
    setBusy(true)
    try {
      const g = await api.createGroup({ name: name.trim(), memberNames: members })
      navigate(`/groups/${g.id}`)
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal title="New group" onClose={onClose}>
      <form onSubmit={submit} className="stack">
        <label className="field">
          <span>Group name</span>
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Goa trip"
                 required maxLength={80} autoFocus />
        </label>

        <div className="field">
          <span>Members</span>
          <div className="chips">
            {members.map((m) => (
              <span key={m} className="chip">
                {m}
                <button type="button" aria-label={`Remove ${m}`}
                        onClick={() => setMembers(members.filter((x) => x !== m))}>×</button>
              </span>
            ))}
          </div>
          <div className="row">
            <input value={newMember} onChange={(e) => setNewMember(e.target.value)}
                   placeholder="Friend's name" maxLength={60}
                   onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); addMember() } }} />
            <button type="button" className="btn btn-ghost" onClick={addMember}>Add</button>
          </div>
          <small className="muted">Friends don't need an account. Just add their names.</small>
        </div>

        {error && <p className="error">{error}</p>}
        <div className="modal-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={busy}>{busy ? 'Creating…' : 'Create group'}</button>
        </div>
      </form>
    </Modal>
  )
}
