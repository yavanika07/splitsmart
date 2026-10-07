import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api.js'
import { money, shortDate } from '../format.js'
import Avatar from '../components/Avatar.jsx'
import ExpenseForm from '../components/ExpenseForm.jsx'
import PaymentForm from '../components/PaymentForm.jsx'

const SPLIT_LABEL = { EQUAL: 'Split equally', EXACT: 'Exact amounts', PERCENTAGE: 'By percentage' }

export default function GroupDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [group, setGroup] = useState(null)
  const [expenses, setExpenses] = useState([])
  const [summary, setSummary] = useState(null)
  const [payments, setPayments] = useState([])
  const [loadError, setLoadError] = useState('')
  const [actionError, setActionError] = useState('')

  const [expenseModal, setExpenseModal] = useState(null) // null | {} (new) | expense (edit)
  const [paymentModal, setPaymentModal] = useState(null) // null | {} | suggestion
  const [newMember, setNewMember] = useState('')

  const load = useCallback(async () => {
    const [g, e, s, p] = await Promise.all([
      api.group(id), api.expenses(id), api.settlement(id), api.payments(id),
    ])
    setGroup(g); setExpenses(e); setSummary(s); setPayments(p)
  }, [id])

  useEffect(() => {
    load().catch((e) => setLoadError(e.message))
  }, [load])

  // Runs an action, shows any error, then refreshes everything
  const run = async (fn) => {
    setActionError('')
    try {
      await fn()
      await load()
    } catch (e) {
      setActionError(e.message)
    }
  }

  if (loadError) {
    return (
      <div className="empty">
        <h3>Couldn't open this group</h3>
        <p className="muted">{loadError}</p>
        <Link to="/" className="btn btn-ghost">Back to groups</Link>
      </div>
    )
  }
  if (!group || !summary) return <p className="muted">Loading…</p>

  const members = group.members
  const suggestions = summary.suggestedPayments

  const saveExpense = async (body) => {
    if (expenseModal?.id) await api.updateExpense(id, expenseModal.id, body)
    else await api.createExpense(id, body)
    setExpenseModal(null)
    await load()
  }

  const savePayment = async (body) => {
    await api.recordPayment(id, body)
    setPaymentModal(null)
    await load()
  }

  const rename = () => {
    const name = window.prompt('Rename group', group.name)
    if (name && name.trim() && name.trim() !== group.name) run(() => api.renameGroup(id, name.trim()))
  }

  const deleteGroup = async () => {
    if (!window.confirm(`Delete "${group.name}" with all its expenses and payments? This can't be undone.`)) return
    try {
      await api.deleteGroup(id)
      navigate('/')
    } catch (e) {
      setActionError(e.message)
    }
  }

  const addMember = (e) => {
    e.preventDefault()
    const name = newMember.trim()
    if (!name) return
    run(async () => {
      await api.addMember(id, name)
      setNewMember('')
    })
  }

  return (
    <>
      <Link to="/" className="back">← All groups</Link>

      <div className="page-head">
        <div>
          <h1>{group.name}</h1>
          <div className="stats">
            <div><span className="muted">Total spent</span><b className="mono">{money(summary.totalSpent)}</b></div>
            <div><span className="muted">Expenses</span><b>{expenses.length}</b></div>
            <div><span className="muted">Members</span><b>{members.length}</b></div>
          </div>
        </div>
        <div className="head-actions">
          <button className="btn btn-ghost btn-sm" onClick={rename}>Rename</button>
          <button className="btn btn-danger-ghost btn-sm" onClick={deleteGroup}>Delete</button>
          <button className="btn btn-primary" onClick={() => setExpenseModal({})}
                  disabled={members.length === 0}>+ Add expense</button>
        </div>
      </div>

      {actionError && (
        <div className="banner-error">
          <span>{actionError}</span>
          <button className="icon-btn" onClick={() => setActionError('')} aria-label="Dismiss">×</button>
        </div>
      )}

      <div className="detail-grid">
        {/* ---------------- Left: expenses ---------------- */}
        <section className="card">
          <div className="card-head"><h2>Expenses</h2></div>
          {expenses.length === 0 ? (
            <div className="empty small">
              <p className="muted">No expenses yet. Add the first one, like a hotel booking or a dinner bill.</p>
              <button className="btn btn-primary btn-sm" onClick={() => setExpenseModal({})}>+ Add expense</button>
            </div>
          ) : (
            <ul className="expense-list">
              {expenses.map((e) => (
                <li key={e.id} className="expense">
                  <div className="date-pill">{shortDate(e.expenseDate)}</div>
                  <div className="expense-body">
                    <div className="expense-top">
                      <b>{e.description}</b>
                      <span className="mono">{money(e.amount)}</span>
                    </div>
                    <div className="muted small-text">
                      {e.paidBy.name} paid · {SPLIT_LABEL[e.splitType]}
                    </div>
                    <div className="split-chips">
                      {e.splits.map((s) => (
                        <span key={s.memberId}>
                          {s.memberName} {money(s.share)}
                          {s.percent != null && <em> ({Number(s.percent)}%)</em>}
                        </span>
                      ))}
                    </div>
                  </div>
                  <div className="row-actions">
                    <button className="link-btn" onClick={() => setExpenseModal(e)}>Edit</button>
                    <button className="link-btn danger"
                            onClick={() => window.confirm(`Delete "${e.description}"?`) &&
                              run(() => api.deleteExpense(id, e.id))}>
                      Delete
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

        {/* ---------------- Right: settle up, balances, members, history ---------------- */}
        <div className="side">
          <section className="card settle-card">
            <div className="card-head">
              <h2>Settle up</h2>
              <button className="link-btn" onClick={() => setPaymentModal({})}
                      disabled={members.length < 2}>Record payment</button>
            </div>
            {suggestions.length === 0 ? (
              <p className="settled">Everyone is settled up.</p>
            ) : (
              <>
                <p className="muted small-text">
                  The fewest payments that clear every debt in this group:
                </p>
                <ul className="suggestions">
                  {suggestions.map((t) => (
                    <li key={`${t.fromId}-${t.toId}`}>
                      <div className="who">
                        <Avatar name={t.fromName} size={26} />
                        <span><b>{t.fromName}</b> pays <b>{t.toName}</b></span>
                      </div>
                      <div className="pay">
                        <span className="mono">{money(t.amount)}</span>
                        <button className="btn btn-primary btn-xs"
                                onClick={() => run(() => api.recordPayment(id, {
                                  fromMemberId: t.fromId, toMemberId: t.toId, amount: Number(t.amount),
                                }))}>
                          Mark paid
                        </button>
                      </div>
                    </li>
                  ))}
                </ul>
              </>
            )}
          </section>

          <section className="card">
            <div className="card-head"><h2>Balances</h2></div>
            <ul className="balances">
              {summary.balances.map((b) => {
                const net = Number(b.net)
                const cls = net > 0.004 ? 'pos' : net < -0.004 ? 'neg' : 'zero'
                return (
                  <li key={b.memberId}>
                    <div className="who">
                      <Avatar name={b.memberName} size={30} />
                      <div>
                        <b>{b.memberName}</b>
                        <div className="muted small-text">paid {money(b.paid)} · share {money(b.owed)}</div>
                      </div>
                    </div>
                    <span className={`net ${cls}`}>
                      {cls === 'pos' && <>gets back <b className="mono">{money(net)}</b></>}
                      {cls === 'neg' && <>owes <b className="mono">{money(-net)}</b></>}
                      {cls === 'zero' && 'settled'}
                    </span>
                  </li>
                )
              })}
            </ul>
          </section>

          <section className="card">
            <div className="card-head"><h2>Members</h2></div>
            <div className="chips">
              {members.map((m) => (
                <span key={m.id} className="chip">
                  {m.name}
                  <button aria-label={`Remove ${m.name}`}
                          onClick={() => window.confirm(`Remove ${m.name} from the group?`) &&
                            run(() => api.removeMember(id, m.id))}>×</button>
                </span>
              ))}
            </div>
            <form className="row" onSubmit={addMember}>
              <input value={newMember} onChange={(e) => setNewMember(e.target.value)}
                     placeholder="Add a member" maxLength={60} />
              <button className="btn btn-ghost">Add</button>
            </form>
          </section>

          {payments.length > 0 && (
            <section className="card">
              <div className="card-head"><h2>Payment history</h2></div>
              <ul className="history">
                {payments.map((p) => (
                  <li key={p.id}>
                    <span><b>{p.from.name}</b> paid <b>{p.to.name}</b></span>
                    <span className="mono">{money(p.amount)}</span>
                    <span className="muted small-text">{shortDate(p.createdAt)}</span>
                    <button className="link-btn danger"
                            onClick={() => window.confirm('Undo this payment?') &&
                              run(() => api.deletePayment(id, p.id))}>
                      Undo
                    </button>
                  </li>
                ))}
              </ul>
            </section>
          )}
        </div>
      </div>

      {expenseModal && (
        <ExpenseForm members={members} expense={expenseModal.id ? expenseModal : null}
                     onSave={saveExpense} onClose={() => setExpenseModal(null)} />
      )}
      {paymentModal && (
        <PaymentForm members={members} initial={paymentModal}
                     onSave={savePayment} onClose={() => setPaymentModal(null)} />
      )}
    </>
  )
}
