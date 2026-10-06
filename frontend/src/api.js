// 后端接口封装
async function request(path, options = {}) {
  const res = await fetch(path, options);
  const ct = res.headers.get('content-type') || '';
  if (!res.ok) {
    let msg = `请求失败 (${res.status})`;
    if (ct.includes('application/json')) {
      const body = await res.json();
      msg = body.error || msg;
    }
    throw new Error(msg);
  }
  return ct.includes('application/json') ? res.json() : res.text();
}

export const api = {
  listReports: () => request('/api/reports'),
  getReport: (id) => request(`/api/reports/${id}`),
  listSpots: () => request('/api/spots'),
  weather: () => request('/api/weather'),

  submitReport: (fd) =>
    request('/api/reports', { method: 'POST', body: fd }),
  addRepair: (id, fd) =>
    request(`/api/reports/${id}/repairs`, { method: 'POST', body: fd }),
  addRecheck: (id, fd) =>
    request(`/api/reports/${id}/rechecks`, { method: 'POST', body: fd }),
  addObjection: (id, fd) =>
    request(`/api/reports/${id}/objections`, { method: 'POST', body: fd }),
  replyObjection: (id, payload) =>
    request(`/api/objections/${id}/reply`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    }),
  addRain: (payload) =>
    request('/api/weather/rain', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    }),
  deleteRain: (id) =>
    request(`/api/weather/rain/${id}`, { method: 'DELETE' })
};

export function formData(obj) {
  const fd = new FormData();
  for (const [k, v] of Object.entries(obj)) {
    if (v === null || v === undefined || v === '') continue;
    fd.append(k, v);
  }
  return fd;
}

// multipart 多文件
export function formDataWithFiles(obj, fileKey, files) {
  const fd = formData(obj);
  for (const f of files || []) fd.append(fileKey, f);
  return fd;
}

export function fmtDateTime(s) {
  if (!s) return '';
  return s.replace('T', ' ').slice(0, 16);
}
