export type Theme = 'light' | 'dark';

export interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
  type?: string;
  instance?: string;
  violations?: unknown;
  [key: string]: unknown;
}

export interface StoredUser {
  login?: string;
  username?: string;
  email?: string;
  authorities?: string[];
  [key: string]: unknown;
}

export type AuthStatus = 'idle' | 'loading' | 'succeeded' | 'failed';

export interface ApiErrorPayload {
  message: string;
  status?: number;
  title?: string;
  body?: unknown;
}

export interface TagDTO {
  name?: string;
  label?: string;
  [key: string]: unknown;
}

export interface NoteDTO {
  id: number;
  title?: string;
  content?: string;
  color?: string | null;
  pinned?: boolean | null;
  tags?: Array<string | TagDTO> | null;
  createdDate?: string | null;
  lastModifiedDate?: string | null;
  deletedDate?: string | null;
  createdBy?: string | null;
  lastModifiedBy?: string | null;
  deletedBy?: string | null;
  owner?: string | null;
  [key: string]: unknown;
}

export interface ShareLinkDTO {
  id: string | number;
  token?: string;
  permission?: string;
  noteTitle?: string;
  noteOwner?: string;
  createdDate?: string;
  expiresAt?: string | null;
  expired?: boolean;
  oneTime?: boolean;
  revoked?: boolean;
  lastUsedAt?: string | null;
  useCount?: number;
  [key: string]: unknown;
}

export interface PageMeta {
  number?: number;
  size?: number;
  totalPages?: number;
  totalElements?: number;
  [key: string]: unknown;
}

export interface PageResponse<T> {
  content: T[];
  page?: PageMeta;
  number?: number;
  size?: number;
  totalPages?: number;
  totalElements?: number;
  [key: string]: unknown;
}

export interface NoteRevisionDTO {
  revision: number;
  revisionType?: string;
  revisionDate?: string;
  auditor?: string;
  note?: NoteDTO;
  [key: string]: unknown;
}
