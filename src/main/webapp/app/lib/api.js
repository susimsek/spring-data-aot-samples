import axios from 'axios';
import { buildRedirectQuery, clearStoredUser, isAdmin, loadStoredUser } from './auth.js';

const api = axios.create({
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
});

const publicApi = axios.create({
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
});

export class ApiError extends Error {
  constructor(message, status, body, title) {
    super(message);
    this.status = status;
    this.body = body;
    this.title = title;
  }
}

let refreshInFlight = null;

function redirectToLogin() {
  if (typeof window === 'undefined') return;
  const path = window.location.pathname || '';
  if (path.includes('/login') || path.includes('/register')) return;
  clearStoredUser();
  const redirect = buildRedirectQuery();
  document.body.style.transition = 'opacity 120ms ease-in';
  document.body.style.opacity = '0';
  setTimeout(() => window.location.replace(`/login?redirect=${redirect}`), 130);
}

function normalizeError(error) {
  if (error instanceof ApiError) return error;
  const status = error?.response?.status;
  const body = error?.response?.data;
  const title = body?.title;
  const message = body?.detail || title || error?.message || 'Request failed';
  return new ApiError(message, status, body, title);
}

async function refresh() {
  if (!refreshInFlight) {
    refreshInFlight = publicApi
      .post('/api/auth/refresh')
      .then(res => res.data ?? {})
      .catch(err => {
        redirectToLogin();
        throw normalizeError(err);
      })
      .finally(() => {
        refreshInFlight = null;
      });
  }
  return refreshInFlight;
}

const refreshExcludedPaths = ['/api/auth/login', '/api/auth/refresh', '/api/auth/register', '/api/auth/logout'];

function shouldAttemptRefresh(url) {
  if (!url) return true;
  return !refreshExcludedPaths.some(path => url.includes(path));
}

api.interceptors.response.use(
  response => response,
  async error => {
    const status = error?.response?.status;
    const config = error?.config || {};
    const url = config.url || '';
    if (status === 401 && !config._retry && shouldAttemptRefresh(url) && !config.skipAuthRedirect) {
      config._retry = true;
      try {
        await refresh();
        return api(config);
      } catch (err) {
        redirectToLogin();
        return Promise.reject(normalizeError(err));
      }
    }
    return Promise.reject(normalizeError(error));
  },
);

publicApi.interceptors.response.use(
  response => response,
  error => Promise.reject(normalizeError(error)),
);

function noteBase() {
  const user = loadStoredUser();
  const admin = isAdmin(user);
  return admin ? '/api/admin/notes' : '/api/notes';
}

const Api = {
  login: async payload => {
    const res = await api.post('/api/auth/login', payload);
    return res.data ?? {};
  },
  register: async payload => {
    const res = await api.post('/api/auth/register', payload);
    return res.data ?? {};
  },
  changePassword: async payload => {
    const res = await api.post('/api/auth/change-password', payload);
    return res.data ?? {};
  },
  currentUser: async () => {
    const res = await api.get('/api/auth/me');
    return res.data ?? {};
  },
  logout: async () => {
    const res = await api.post('/api/auth/logout');
    return res.data ?? {};
  },
  fetchNotes: async ({ view, page, size, sort, query, tags, color, pinned }) => {
    const base = view === 'trash' ? `${noteBase()}/deleted` : noteBase();
    const params = new URLSearchParams();
    params.set('page', page);
    params.set('size', size);
    if (sort) params.set('sort', sort);
    if (query) params.set('q', query);
    if (color) params.set('color', color);
    if (typeof pinned === 'boolean') params.set('pinned', pinned);
    if (tags && Array.isArray(tags)) {
      tags.forEach(tag => params.append('tags', tag));
    }
    const res = await api.get(`${base}?${params.toString()}`);
    return res.data ?? {};
  },
  createNote: async payload => {
    const res = await api.post(noteBase(), payload);
    return res.data ?? {};
  },
  updateNote: async (id, payload) => {
    const res = await api.put(`${noteBase()}/${id}`, payload);
    return res.data ?? {};
  },
  patchNote: async (id, payload) => {
    const res = await api.patch(`${noteBase()}/${id}`, payload);
    return res.data ?? {};
  },
  softDelete: async id => {
    const res = await api.delete(`${noteBase()}/${id}`);
    return res.data ?? {};
  },
  restore: async id => {
    const res = await api.post(`${noteBase()}/${id}/restore`);
    return res.data ?? {};
  },
  deletePermanent: async id => {
    const res = await api.delete(`${noteBase()}/${id}/permanent`);
    return res.data ?? {};
  },
  emptyTrash: async () => {
    const res = await api.delete(`${noteBase()}/deleted`);
    return res.data ?? {};
  },
  fetchNote: async id => {
    const res = await api.get(`${noteBase()}/${id}`);
    return res.data ?? {};
  },
  bulkAction: async payload => {
    const res = await api.post(`${noteBase()}/bulk`, payload);
    return res.data ?? {};
  },
  fetchRevisions: async (id, sort, page = 0, size = 5) => {
    const sortParam = sort ? `&sort=${encodeURIComponent(sort)}` : '';
    const res = await api.get(`${noteBase()}/${id}/revisions?page=${page}&size=${size}${sortParam}`);
    return res.data ?? {};
  },
  fetchRevision: async (id, revisionId) => {
    const res = await api.get(`${noteBase()}/${id}/revisions/${revisionId}`);
    return res.data ?? {};
  },
  restoreRevision: async (id, revisionId) => {
    const res = await api.post(`${noteBase()}/${id}/revisions/${revisionId}/restore`);
    return res.data ?? {};
  },
  fetchTags: async query => {
    const params = new URLSearchParams();
    params.set('page', '0');
    params.set('size', '10');
    if (query) {
      params.set('q', query);
    }
    const res = await api.get(`/api/tags/suggest?${params.toString()}`);
    const body = res.data ?? {};
    if (Array.isArray(body)) {
      return body;
    }
    if (body && Array.isArray(body.content)) {
      return body.content.map(t => t.name ?? t.label ?? t);
    }
    return [];
  },
  searchUsers: async (query, page = 0, size = 10) => {
    const params = new URLSearchParams();
    params.set('page', page);
    params.set('size', size);
    if (query) {
      params.set('q', query);
    }
    const res = await api.get(`/api/admin/users/search?${params.toString()}`);
    return res.data ?? {};
  },
  changeOwner: async (id, payload) => {
    const res = await api.post(`/api/admin/notes/${id}/owner`, payload);
    return res.data ?? {};
  },
  fetchShareLinks: async (noteId, page, size) => {
    const res = await api.get(`${noteBase()}/${noteId}/share?page=${page}&size=${size}`);
    return res.data ?? {};
  },
  createShareLink: async (noteId, payload) => {
    const res = await api.post(`${noteBase()}/${noteId}/share`, payload);
    return res.data ?? {};
  },
  revokeShareLink: async tokenId => {
    const res = await api.delete(`${noteBase()}/share/${tokenId}`);
    return res.data ?? {};
  },
  fetchMyShareLinks: async (sort, query, status, createdFrom, createdTo, page, size) => {
    const params = new URLSearchParams();
    if (sort) params.set('sort', sort);
    if (query) params.set('q', query);
    if (status && status !== 'all') params.set('status', status);
    if (createdFrom) params.set('createdFrom', createdFrom);
    if (createdTo) params.set('createdTo', createdTo);
    params.set('page', page);
    params.set('size', size);
    const res = await api.get(`${noteBase()}/share?${params.toString()}`);
    return res.data ?? {};
  },
  fetchNoteWithShareToken: async token => {
    const res = await publicApi.get(`/api/share/${encodeURIComponent(token)}`);
    return res.data ?? {};
  },
};

export default Api;
