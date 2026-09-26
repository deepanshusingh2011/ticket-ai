const BASE = '';

async function parseError(res) {
  try {
    const data = await res.json();
    if (data.fields) {
      return Object.entries(data.fields)
        .map(([k, v]) => `${k}: ${v}`)
        .join('; ');
    }
    return data.error || `Request failed (${res.status})`;
  } catch {
    return `Request failed (${res.status})`;
  }
}

async function handle(res) {
  if (!res.ok) throw new Error(await parseError(res));
  if (res.status === 204) return null;
  return res.json();
}

export const api = {
  list: ({ status, q } = {}) => {
    const params = new URLSearchParams();
    if (status) params.set('status', status);
    if (q) params.set('q', q);
    const suffix = params.toString() ? `?${params}` : '';
    return fetch(`${BASE}/api/tickets${suffix}`).then(handle);
  },
  get: (id) => fetch(`${BASE}/api/tickets/${id}`).then(handle),
  create: (payload) =>
    fetch(`${BASE}/api/tickets`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    }).then(handle),
  update: (id, payload) =>
    fetch(`${BASE}/api/tickets/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    }).then(handle),
  changeStatus: (id, status) =>
    fetch(`${BASE}/api/tickets/${id}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status })
    }).then(handle),
  addComment: (id, payload) =>
    fetch(`${BASE}/api/tickets/${id}/comments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    }).then(handle)
};

export const STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'CANCELLED'];
export const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

/** Allowed next statuses per the backend state machine (for hints in the UI). */
export function allowedNext(status) {
  switch (status) {
    case 'OPEN':
      return ['IN_PROGRESS', 'CANCELLED'];
    case 'IN_PROGRESS':
      return ['RESOLVED', 'CANCELLED'];
    case 'RESOLVED':
      return ['CLOSED'];
    default:
      return [];
  }
}
