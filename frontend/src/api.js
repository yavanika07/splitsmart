// Every call to the Spring Boot API goes through here.
const TOKEN_KEY = 'splitsmart_token'

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

let onUnauthorized = () => {}
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn
}

async function request(method, path, body) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const token = tokenStore.get()
  if (token) headers['Authorization'] = `Bearer ${token}`

  let res
  try {
    res = await fetch(`/api${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new Error('Cannot reach the server. Is the backend running on port 8080?')
  }

  // Token missing/expired on a protected route -> log out
  if (res.status === 401 && !path.startsWith('/auth/login') && !path.startsWith('/auth/register')) {
    onUnauthorized()
    throw new Error('Your session has expired. Please log in again.')
  }
  if (res.status === 204) return null

  const text = await res.text()
  let data = null
  try {
    data = text ? JSON.parse(text) : null
  } catch {
    data = null
  }
  if (!res.ok) throw new Error(data?.error || `Request failed (${res.status})`)
  return data
}

export const api = {
  // auth
  register: (body) => request('POST', '/auth/register', body),
  login: (body) => request('POST', '/auth/login', body),
  me: () => request('GET', '/auth/me'),

  // groups & members
  groups: () => request('GET', '/groups'),
  group: (id) => request('GET', `/groups/${id}`),
  createGroup: (body) => request('POST', '/groups', body),
  renameGroup: (id, name) => request('PATCH', `/groups/${id}`, { name }),
  deleteGroup: (id) => request('DELETE', `/groups/${id}`),
  addMember: (id, name) => request('POST', `/groups/${id}/members`, { name }),
  removeMember: (id, memberId) => request('DELETE', `/groups/${id}/members/${memberId}`),

  // expenses
  expenses: (gid) => request('GET', `/groups/${gid}/expenses`),
  createExpense: (gid, body) => request('POST', `/groups/${gid}/expenses`, body),
  updateExpense: (gid, eid, body) => request('PUT', `/groups/${gid}/expenses/${eid}`, body),
  deleteExpense: (gid, eid) => request('DELETE', `/groups/${gid}/expenses/${eid}`),

  // settlement & payments
  settlement: (gid) => request('GET', `/groups/${gid}/settlement`),
  payments: (gid) => request('GET', `/groups/${gid}/payments`),
  recordPayment: (gid, body) => request('POST', `/groups/${gid}/payments`, body),
  deletePayment: (gid, pid) => request('DELETE', `/groups/${gid}/payments/${pid}`),
}
