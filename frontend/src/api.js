async function request(path, options = {}) {
  const opts = { ...options };
  if (!(opts.body instanceof FormData)) {
    opts.headers = { 'Content-Type': 'application/json', ...(opts.headers || {}) };
  }
  const res = await fetch('/api' + path, opts);
  if (!res.ok) {
    let msg = res.statusText;
    try {
      const j = await res.json();
      if (j.message) msg = j.message;
    } catch { /* ignore */ }
    throw new Error(msg);
  }
  return res.json();
}

export const api = {
  tickets: (status) => request('/tickets' + (status ? `?status=${status}` : '')),
  ticket: (id) => request(`/tickets/${id}`),
  createTicket: (data) => request('/tickets', { method: 'POST', body: JSON.stringify(data) }),
  startRepair: (id) => request(`/tickets/${id}/start-repair`, { method: 'POST' }),
  completeConstruction: (id, data) => request(`/tickets/${id}/construction`, { method: 'POST', body: JSON.stringify(data) }),
  recheck: (id, data) => request(`/tickets/${id}/recheck`, { method: 'POST', body: JSON.stringify(data) }),
  linkPoint: (id, pointId) => request(`/tickets/${id}/link-point/${pointId}`, { method: 'POST' }),
  recurrence: (id, data) => request(`/tickets/${id}/recurrence`, { method: 'POST', body: JSON.stringify(data) }),
  dispute: (id, data) => request(`/tickets/${id}/disputes`, { method: 'POST', body: JSON.stringify(data) }),
  replyDispute: (id, disputeId, data) => request(`/tickets/${id}/disputes/${disputeId}/reply`, { method: 'POST', body: JSON.stringify(data) }),
  uploadPhotos: (id, phase, files) => {
    const fd = new FormData();
    for (const f of files) fd.append('file', f);
    return request(`/tickets/${id}/photos?phase=${phase}`, { method: 'POST', body: fd });
  },
  points: () => request('/points'),
  weather: () => request('/weather'),
  addRain: (data) => request('/weather/events', { method: 'POST', body: JSON.stringify(data) }),
  reminders: () => request('/weather/reminders'),
};

export const STATUS = {
  REPORTED: { label: '待受理', cls: 'st-reported' },
  IN_REPAIR: { label: '维修中', cls: 'st-repair' },
  PENDING_RECHECK: { label: '待复查', cls: 'st-pending' },
  VERIFIED: { label: '复查通过', cls: 'st-verified' },
  DISPUTED: { label: '异议处理中', cls: 'st-disputed' },
};

export const RESOLUTIONS = {
  KEEP_VERIFIED: '维持复查通过结论',
  REOPEN_REPAIR: '重新安排维修',
  RECHECK_AGAIN: '安排再次复查',
};

export function fmtTime(t) {
  return t ? t.replace('T', ' ').slice(0, 16) : '';
}
