import axios, { type AxiosError, type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios';
import { buildRedirectQuery, clearStoredUser, isAdmin, loadStoredUser } from './auth';
import { getDocument, getLocation, replaceLocation } from './window';
import type { NoteDTO, NoteRevisionDTO, PageResponse, ShareLinkDTO, StoredUser } from './types';

type JsonObject = Record<string, unknown>;

const api: AxiosInstance = axios.create({
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
});

const publicApi: AxiosInstance = axios.create({
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
});

export class ApiError extends Error {
  status?: number;
  body?: unknown;
  title?: string;

  constructor(message: string, status?: number, body?: unknown, title?: string) {
    super(message);
    this.status = status;
    this.body = body;
    this.title = title;
  }
}

let refreshInFlight: Promise<JsonObject> | null = null;
let refreshDisabled = false;

function redirectToLogin() {
  const location = getLocation();
  const document = getDocument();
  if (!location || !document) return;
  const path = location.pathname || '';
  if (path.includes('/login') || path.includes('/register')) return;
  clearStoredUser();
  const redirect = buildRedirectQuery();
  if (document.body) {
    document.body.style.transition = 'opacity 120ms ease-in';
    document.body.style.opacity = '0';
  }
  setTimeout(() => replaceLocation(`/login?redirect=${redirect}`), 130);
}

function normalizeError(error: unknown): ApiError {
  if (error instanceof ApiError) return error;
  const axiosError = axios.isAxiosError(error) ? (error as AxiosError) : undefined;
  if (!axiosError) {
    const message = error instanceof Error && error.message ? error.message : 'Request failed';
    return new ApiError(message);
  }
  const status = axiosError?.response?.status;
  const body = axiosError?.response?.data;
  const title = (body as { title?: unknown } | undefined)?.title;
  const detail = (body as { detail?: unknown } | undefined)?.detail;
  const message =
    (typeof detail === 'string' && detail) ||
    (typeof title === 'string' && title) ||
    (axiosError?.message ? axiosError.message : undefined) ||
    'Request failed';
  return new ApiError(message, typeof status === 'number' ? status : undefined, body, typeof title === 'string' ? title : undefined);
}

async function refresh(): Promise<JsonObject> {
  if (refreshDisabled) {
    throw new ApiError('Unauthorized', 401);
  }
  if (!refreshInFlight) {
    refreshInFlight = publicApi
      .post('/api/auth/refresh')
      .then((res: AxiosResponse<JsonObject>) => res.data ?? {})
      .catch((err) => {
        const apiErr = normalizeError(err);
        if (apiErr.status === 401 || apiErr.status === 400) {
          refreshDisabled = true;
        }
        redirectToLogin();
        throw apiErr;
      })
      .finally(() => {
        refreshInFlight = null;
      });
  }
  return refreshInFlight;
}

const refreshExcludedPaths = ['/api/auth/login', '/api/auth/refresh', '/api/auth/register', '/api/auth/logout'];

function shouldAttemptRefresh(url: string | undefined): boolean {
  if (!url) return true;
  return !refreshExcludedPaths.some((path) => url.includes(path));
}

type RetriableRequestConfig = AxiosRequestConfig & {
  _retry?: boolean;
  skipAuthRedirect?: boolean;
};

api.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    if (!axios.isAxiosError(error)) {
      return Promise.reject(normalizeError(error));
    }
    const status = error.response?.status;
    const config = (error.config ?? {}) as RetriableRequestConfig;
    const url = config.url || '';
    if (status === 401 && !config._retry && shouldAttemptRefresh(url) && !config.skipAuthRedirect) {
      config._retry = true;
      try {
        if (refreshDisabled) {
          redirectToLogin();
          return Promise.reject(normalizeError(error));
        }
        await refresh();
        return api.request(config);
      } catch (err) {
        redirectToLogin();
        return Promise.reject(normalizeError(err));
      }
    }
    return Promise.reject(normalizeError(error));
  },
);

publicApi.interceptors.response.use(
  (response) => response,
  (error: unknown) => Promise.reject(normalizeError(error)),
);

function noteBase() {
  const user = loadStoredUser();
  const admin = isAdmin(user);
  return admin ? '/api/admin/notes' : '/api/notes';
}

const Api = {
  login: async (payload: { username: string; password: string; rememberMe?: boolean }): Promise<JsonObject> => {
    refreshDisabled = false;
    const res = await api.post<JsonObject>('/api/auth/login', payload);
    return res.data ?? {};
  },
  register: async (payload: JsonObject): Promise<JsonObject> => {
    const res = await api.post<JsonObject>('/api/auth/register', payload);
    return res.data ?? {};
  },
  changePassword: async (payload: JsonObject): Promise<JsonObject> => {
    const res = await api.post<JsonObject>('/api/auth/change-password', payload);
    return res.data ?? {};
  },
  currentUser: async (): Promise<StoredUser> => {
    const res = await api.get<StoredUser>('/api/auth/me');
    return (res.data ?? {}) as StoredUser;
  },
  logout: async (): Promise<JsonObject> => {
    refreshDisabled = true;
    const res = await api.post<JsonObject>('/api/auth/logout');
    return res.data ?? {};
  },
  fetchNotes: async (options: {
    view?: string;
    page: number;
    size: number;
    sort?: string;
    query?: string;
    tags?: string[];
    color?: string;
    pinned?: boolean;
  }): Promise<PageResponse<NoteDTO>> => {
    const { view, page, size, sort, query, tags, color, pinned } = options;
    const base = view === 'trash' ? `${noteBase()}/deleted` : noteBase();
    const searchParams = new URLSearchParams();
    searchParams.set('page', String(page));
    searchParams.set('size', String(size));
    if (sort) searchParams.set('sort', sort);
    if (query) searchParams.set('q', query);
    if (color) searchParams.set('color', color);
    if (typeof pinned === 'boolean') searchParams.set('pinned', String(pinned));
    if (tags && Array.isArray(tags)) {
      tags.forEach((tag) => searchParams.append('tags', tag));
    }
    const res = await api.get<PageResponse<NoteDTO>>(`${base}?${searchParams.toString()}`);
    return (res.data ?? { content: [] }) as PageResponse<NoteDTO>;
  },
  createNote: async (payload: JsonObject): Promise<NoteDTO> => {
    const res = await api.post<NoteDTO>(noteBase(), payload);
    return res.data as NoteDTO;
  },
  updateNote: async (id: string | number, payload: JsonObject): Promise<NoteDTO> => {
    const res = await api.put<NoteDTO>(`${noteBase()}/${id}`, payload);
    return res.data as NoteDTO;
  },
  patchNote: async (id: string | number, payload: JsonObject): Promise<NoteDTO> => {
    const res = await api.patch<NoteDTO>(`${noteBase()}/${id}`, payload);
    return res.data as NoteDTO;
  },
  softDelete: async (id: string | number): Promise<JsonObject> => {
    const res = await api.delete<JsonObject>(`${noteBase()}/${id}`);
    return res.data ?? {};
  },
  restore: async (id: string | number): Promise<JsonObject> => {
    const res = await api.post<JsonObject>(`${noteBase()}/${id}/restore`);
    return res.data ?? {};
  },
  deletePermanent: async (id: string | number): Promise<JsonObject> => {
    const res = await api.delete<JsonObject>(`${noteBase()}/${id}/permanent`);
    return res.data ?? {};
  },
  emptyTrash: async (): Promise<JsonObject> => {
    const res = await api.delete<JsonObject>(`${noteBase()}/deleted`);
    return res.data ?? {};
  },
  fetchNote: async (id: string | number): Promise<NoteDTO> => {
    const res = await api.get<NoteDTO>(`${noteBase()}/${id}`);
    return res.data as NoteDTO;
  },
  bulkAction: async (payload: JsonObject): Promise<JsonObject> => {
    const res = await api.post<JsonObject>(`${noteBase()}/bulk`, payload);
    return res.data ?? {};
  },
  fetchRevisions: async (id: string | number, sort: string | undefined, page = 0, size = 5): Promise<PageResponse<NoteRevisionDTO>> => {
    const sortParam = sort ? `&sort=${encodeURIComponent(sort)}` : '';
    const res = await api.get<PageResponse<NoteRevisionDTO>>(`${noteBase()}/${id}/revisions?page=${page}&size=${size}${sortParam}`);
    return (res.data ?? { content: [] }) as PageResponse<NoteRevisionDTO>;
  },
  fetchRevision: async (id: string | number, revisionId: string | number): Promise<NoteRevisionDTO> => {
    const res = await api.get<NoteRevisionDTO>(`${noteBase()}/${id}/revisions/${revisionId}`);
    return res.data as NoteRevisionDTO;
  },
  restoreRevision: async (id: string | number, revisionId: string | number): Promise<JsonObject> => {
    const res = await api.post<JsonObject>(`${noteBase()}/${id}/revisions/${revisionId}/restore`);
    return res.data ?? {};
  },
  fetchTags: async (query: string | undefined): Promise<string[]> => {
    const params = new URLSearchParams();
    params.set('page', '0');
    params.set('size', '10');
    if (query) {
      params.set('q', query);
    }
    const res = await api.get<unknown>(`/api/tags/suggest?${params.toString()}`);
    const body: unknown = res.data;
    if (Array.isArray(body)) {
      return body.filter((t): t is string => typeof t === 'string');
    }
    if (body && typeof body === 'object' && Array.isArray((body as { content?: unknown }).content)) {
      const content = (body as { content: unknown[] }).content;
      return content
        .map((value): unknown => {
          if (typeof value === 'string') return value;
          if (value && typeof value === 'object') {
            const record = value as Record<string, unknown>;
            return record.name ?? record.label ?? value;
          }
          return value;
        })
        .filter((t): t is string => typeof t === 'string');
    }
    return [];
  },
  searchUsers: async (query: string | undefined, page = 0, size = 10): Promise<PageResponse<StoredUser>> => {
    const params = new URLSearchParams();
    params.set('page', String(page));
    params.set('size', String(size));
    if (query) {
      params.set('q', query);
    }
    const res = await api.get<PageResponse<StoredUser>>(`/api/admin/users/search?${params.toString()}`);
    return (res.data ?? { content: [] }) as PageResponse<StoredUser>;
  },
  changeOwner: async (id: string | number, payload: JsonObject): Promise<JsonObject> => {
    const res = await api.post<JsonObject>(`/api/admin/notes/${id}/owner`, payload);
    return res.data ?? {};
  },
  fetchShareLinks: async (noteId: string | number, page: number, size: number): Promise<PageResponse<ShareLinkDTO>> => {
    const res = await api.get<PageResponse<ShareLinkDTO>>(`${noteBase()}/${noteId}/share?page=${page}&size=${size}`);
    return (res.data ?? { content: [] }) as PageResponse<ShareLinkDTO>;
  },
  createShareLink: async (noteId: string | number, payload: JsonObject): Promise<ShareLinkDTO> => {
    const res = await api.post<ShareLinkDTO>(`${noteBase()}/${noteId}/share`, payload);
    return (res.data ?? {}) as ShareLinkDTO;
  },
  revokeShareLink: async (tokenId: string | number): Promise<JsonObject> => {
    const res = await api.delete<JsonObject>(`${noteBase()}/share/${tokenId}`);
    return res.data ?? {};
  },
  fetchMyShareLinks: async (
    sort: string,
    query: string,
    status: string,
    createdFrom: string,
    createdTo: string,
    page: number,
    size: number,
  ): Promise<PageResponse<ShareLinkDTO>> => {
    const params = new URLSearchParams();
    if (sort) params.set('sort', sort);
    if (query) params.set('q', query);
    if (status && status !== 'all') params.set('status', status);
    if (createdFrom) params.set('createdFrom', createdFrom);
    if (createdTo) params.set('createdTo', createdTo);
    params.set('page', String(page));
    params.set('size', String(size));
    const res = await api.get<PageResponse<ShareLinkDTO>>(`${noteBase()}/share?${params.toString()}`);
    return (res.data ?? { content: [] }) as PageResponse<ShareLinkDTO>;
  },
  fetchNoteWithShareToken: async (token: string): Promise<NoteDTO> => {
    const res = await publicApi.get<NoteDTO>(`/api/share/${encodeURIComponent(token)}`);
    return res.data as NoteDTO;
  },
};

export default Api;
