'use client';

import { useCallback, useEffect, useMemo, useState, type FormEvent, type JSX } from 'react';
import Container from 'react-bootstrap/Container';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Card from 'react-bootstrap/Card';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import InputGroup from 'react-bootstrap/InputGroup';
import Badge from 'react-bootstrap/Badge';
import Modal from 'react-bootstrap/Modal';
import Alert from 'react-bootstrap/Alert';
import Pagination from 'react-bootstrap/Pagination';
import Spinner from 'react-bootstrap/Spinner';
import ListGroup from 'react-bootstrap/ListGroup';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { Controller, useForm } from 'react-hook-form';
import {
  faBars,
  faCheck,
  faCircle,
  faClockRotateLeft,
  faCopy,
  faFilter,
  faLayerGroup,
  faLink,
  faPen,
  faPenToSquare,
  faPlus,
  faRotateLeft,
  faShareFromSquare,
  faThumbtack,
  faTrash,
  faTrashCan,
  faUser,
  faUserGear,
  faUserShield,
  faXmark,
} from '@fortawesome/free-solid-svg-icons';
import { faCalendar, faClock, faNoteSticky } from '@fortawesome/free-regular-svg-icons';
import AppNavbar from './components/AppNavbar';
import Footer from './components/Footer';
import TagInput from './components/TagInput';
import Api, { ApiError } from './lib/api';
import useAuth from './lib/useAuth';
import { useToasts } from './components/ToastProvider';
import { formatDate, toIsoString } from './lib/format';
import { diffLinesDetailed, diffTypes } from './lib/diff';
import type { DiffOp } from './lib/diff';
import type { NoteDTO, NoteRevisionDTO, ShareLinkDTO, StoredUser, TagDTO } from './types';

const TAG_PATTERN = /^[A-Za-z0-9_-]{1,30}$/;
const TAG_FORMAT_MESSAGE = 'Tags may include letters, digits, hyphen, or underscore.';
const DEFAULT_COLOR = '#2563eb';
const SHARE_LINKS_PAGE_SIZE = 3;
const REVISION_PAGE_SIZE = 5;
const OWNER_SEARCH_PAGE_SIZE = 5;

function tagLabel(tag: unknown): string {
  if (tag == null) return '';
  if (typeof tag === 'string') return tag;
  if (typeof tag === 'object') {
    const obj = tag as TagDTO;
    return (typeof obj.name === 'string' && obj.name) || (typeof obj.label === 'string' && obj.label) || '';
  }
  return String(tag);
}

function normalizeTags(tags: Array<string | TagDTO> | null | undefined): string[] {
  return (tags || []).map(tagLabel).filter(tag => tag && tag.trim().length > 0);
}

function validateNotePayload(payload: { title?: string; content?: string; tags?: Array<string | TagDTO> | null }): Record<string, string> {
  const errors: Record<string, string> = {};
  const title = String(payload.title || '').trim();
  const content = String(payload.content || '').trim();
  if (!title) {
    errors.title = 'This field is required.';
  } else if (title.length < 3 || title.length > 255) {
    errors.title = 'Size must be between 3 and 255 characters.';
  }
  if (!content) {
    errors.content = 'This field is required.';
  } else if (content.length < 10 || content.length > 1024) {
    errors.content = 'Size must be between 10 and 1024 characters.';
  }
  const tags = normalizeTags(payload.tags as Array<string | TagDTO> | undefined);
  if (tags.length > 5) {
    errors.tags = 'Up to 5 tags allowed.';
  } else if (tags.some(tag => tag.length < 1 || tag.length > 30)) {
    errors.tags = 'Tags must be 1-30 characters.';
  } else if (tags.some(tag => !TAG_PATTERN.test(tag))) {
    errors.tags = TAG_FORMAT_MESSAGE;
  }
  return errors;
}

function buildShareUrl(token: string): string {
  const origin = ((globalThis as any).location as Location | undefined)?.origin;
  if (!origin || !token) return '';
  return `${origin}/share?share_token=${encodeURIComponent(token)}`;
}

function messageFromError(err: unknown, fallback: string): string {
  if (err instanceof Error) return err.message || fallback;
  return fallback;
}

function renderDiffSpans(ops: DiffOp[], additionsOnly: boolean): Array<JSX.Element> {
  return ops
    .map((op, idx) => {
      const value = op.value || '';
      if (op.type === diffTypes.add) {
        return (
          <span key={`${idx}-${op.type}`} className="bg-success-subtle text-success border border-success-subtle rounded px-2">
            + {value}
          </span>
        );
      }
      if (op.type === diffTypes.del) {
        if (additionsOnly) return null;
        return (
          <span
            key={`${idx}-${op.type}`}
            className="bg-danger-subtle text-danger border border-danger-subtle rounded px-2 text-decoration-line-through"
          >
            − {value}
          </span>
        );
      }
      return (
        <span key={`${idx}-${op.type}`} className="text-muted">
          {value}
        </span>
      );
    })
    .filter((el): el is JSX.Element => el != null);
}

type NoteView = 'active' | 'trash';

type NoteAction =
  | 'edit-modal'
  | 'delete'
  | 'delete-forever'
  | 'restore'
  | 'copy'
  | 'toggle-pin'
  | 'share'
  | 'share-links'
  | 'revisions'
  | 'change-owner';

interface NoteDraft {
  title: string;
  content: string;
  color: string;
  pinned: boolean;
  tags: string[];
}

interface NoteFormValues {
  title: string;
  content: string;
  color: string;
  tags: string[];
  pinned: boolean;
}

interface ShareResult {
  url: string;
  permission: string;
  expiresAt: string | null;
  oneTime: boolean;
}

function NoteCard({
  note,
  view,
  showOwner,
  selected,
  onSelectToggle,
  onAction,
  loadTagSuggestions,
  onInlineSave,
}: Readonly<{
  note: NoteDTO;
  view: NoteView;
  showOwner: boolean;
  selected: boolean;
  onSelectToggle: (noteId: number, checked: boolean) => void;
  onAction: (action: NoteAction, note: NoteDTO) => void | Promise<void>;
  loadTagSuggestions: (query: string) => Promise<string[]>;
  onInlineSave: (
    noteId: number,
    payload: { title: string; content: string; color: string; pinned: boolean; tags: string[] },
  ) => Promise<NoteDTO | null>;
}>) {
  const [inlineMode, setInlineMode] = useState(false);
  const [draft, setDraft] = useState<NoteDraft>({
    title: note.title || '',
    content: note.content || '',
    color: note.color || DEFAULT_COLOR,
    pinned: !!note.pinned,
    tags: normalizeTags(note.tags),
  });
  const [inlineErrors, setInlineErrors] = useState<Record<string, string>>({});
  const [inlineSaving, setInlineSaving] = useState(false);

  useEffect(() => {
    if (!inlineMode) return;
    setDraft({
      title: note.title || '',
      content: note.content || '',
      color: note.color || DEFAULT_COLOR,
      pinned: !!note.pinned,
      tags: normalizeTags(note.tags),
    });
    setInlineErrors({});
  }, [inlineMode, note]);

  const meta = useMemo(() => {
    const createdText = formatDate(note.createdDate);
    const modifiedText = note.lastModifiedDate ? formatDate(note.lastModifiedDate) : createdText;
    const deletedText = note.deletedDate ? formatDate(note.deletedDate) : '';
    return {
      createdText,
      modifiedText,
      deletedText,
    };
  }, [note]);

  const handleInlineSave = async () => {
    const errors = validateNotePayload(draft);
    setInlineErrors(errors);
    if (Object.keys(errors).length > 0) return;
    setInlineSaving(true);
    try {
      const updated = await onInlineSave(note.id, {
        title: draft.title.trim(),
        content: draft.content.trim(),
        color: draft.color,
        pinned: !!draft.pinned,
        tags: normalizeTags(draft.tags),
      });
      if (updated) {
        setInlineMode(false);
      }
    } finally {
      setInlineSaving(false);
    }
  };

  return (
    <Col xs={12} md={6} xl={6}>
      <Card className="h-100 border-0 shadow-sm" id={`note-${note.id}`}>
        <Card.Body className="d-flex flex-column gap-2">
          <div className="d-flex justify-content-between align-items-start">
            <Form.Check
              type="checkbox"
              className="me-2 mt-1"
              checked={selected}
              onChange={event => onSelectToggle(note.id, event.target.checked)}
            />
            <div className="flex-grow-1">
              <div className="d-flex align-items-center gap-2">
                <div className="fw-bold text-primary mb-0">{note.title}</div>
                {note.pinned ? <FontAwesomeIcon icon={faThumbtack} className="text-warning" /> : null}
                {note.color ? (
                  <Badge bg="body-secondary" text="body" className="border" style={{ borderColor: note.color }}>
                    <FontAwesomeIcon icon={faCircle} style={{ color: note.color }} />
                  </Badge>
                ) : null}
              </div>
              <div className="text-muted small">{note.content}</div>
              {normalizeTags(note.tags).length ? (
                <div className="d-flex flex-wrap gap-1 mt-1">
                  {normalizeTags(note.tags).map(tag => (
                    <Badge key={tag} bg="secondary-subtle" text="secondary">
                      {tag}
                    </Badge>
                  ))}
                </div>
              ) : null}
            </div>
            {view === 'trash' ? (
              <div className="d-flex flex-wrap gap-1 justify-content-end">
                <Button variant="success" size="sm" onClick={() => onAction('restore', note)} title="Restore">
                  <FontAwesomeIcon icon={faRotateLeft} />
                </Button>
                <Button variant="outline-secondary" size="sm" onClick={() => onAction('copy', note)} title="Copy content">
                  <FontAwesomeIcon icon={faCopy} />
                </Button>
                {showOwner ? (
                  <Button variant="outline-secondary" size="sm" onClick={() => onAction('change-owner', note)} title="Change owner">
                    <FontAwesomeIcon icon={faUserGear} />
                  </Button>
                ) : null}
                <Button variant="outline-info" size="sm" onClick={() => onAction('revisions', note)} title="Revision history">
                  <FontAwesomeIcon icon={faClockRotateLeft} />
                </Button>
                <Button variant="outline-danger" size="sm" onClick={() => onAction('delete-forever', note)} title="Delete permanently">
                  <FontAwesomeIcon icon={faTrash} />
                </Button>
              </div>
            ) : (
              <div className="d-grid gap-1" style={{ gridTemplateColumns: 'repeat(3, 32px)' }}>
                <Button
                  variant="outline-warning"
                  size="sm"
                  style={{ width: 32, height: 32 }}
                  onClick={() => onAction('toggle-pin', note)}
                  title={note.pinned ? 'Unpin' : 'Pin'}
                >
                  <FontAwesomeIcon icon={faThumbtack} className={note.pinned ? '' : 'opacity-50'} />
                </Button>
                <Button
                  variant="outline-primary"
                  size="sm"
                  style={{ width: 32, height: 32 }}
                  onClick={() => onAction('edit-modal', note)}
                  title="Edit in modal"
                >
                  <FontAwesomeIcon icon={faPenToSquare} />
                </Button>
                <Button
                  variant="outline-secondary"
                  size="sm"
                  style={{ width: 32, height: 32 }}
                  onClick={() => setInlineMode(prev => !prev)}
                  title="Inline edit"
                >
                  <FontAwesomeIcon icon={faPen} />
                </Button>
                <Button
                  variant="outline-secondary"
                  size="sm"
                  style={{ width: 32, height: 32 }}
                  onClick={() => onAction('copy', note)}
                  title="Copy content"
                >
                  <FontAwesomeIcon icon={faCopy} />
                </Button>
                <Button
                  variant="outline-secondary"
                  size="sm"
                  style={{ width: 32, height: 32 }}
                  onClick={() => onAction('share-links', note)}
                  title="Existing links"
                >
                  <FontAwesomeIcon icon={faLink} />
                </Button>
                <Button
                  variant="outline-secondary"
                  size="sm"
                  style={{ width: 32, height: 32 }}
                  onClick={() => onAction('share', note)}
                  title="Create share link"
                >
                  <FontAwesomeIcon icon={faShareFromSquare} />
                </Button>
                {showOwner ? (
                  <Button
                    variant="outline-secondary"
                    size="sm"
                    style={{ width: 32, height: 32 }}
                    onClick={() => onAction('change-owner', note)}
                    title="Change owner"
                  >
                    <FontAwesomeIcon icon={faUserGear} />
                  </Button>
                ) : null}
                <Button
                  variant="outline-info"
                  size="sm"
                  style={{ width: 32, height: 32 }}
                  onClick={() => onAction('revisions', note)}
                  title="Revision history"
                >
                  <FontAwesomeIcon icon={faClockRotateLeft} />
                </Button>
                <Button
                  variant="outline-danger"
                  size="sm"
                  style={{ width: 32, height: 32 }}
                  onClick={() => onAction('delete', note)}
                  title="Delete"
                >
                  <FontAwesomeIcon icon={faTrash} />
                </Button>
              </div>
            )}
          </div>

          {inlineMode ? (
            <div className="mt-2 border-top pt-2">
              <Form.Group className="mb-2">
                <Form.Control
                  size="sm"
                  type="text"
                  placeholder="Title"
                  value={draft.title}
                  isInvalid={!!inlineErrors.title}
                  onChange={event => setDraft(prev => ({ ...prev, title: event.target.value }))}
                />
                {inlineErrors.title ? <div className="invalid-feedback d-block">{inlineErrors.title}</div> : null}
              </Form.Group>
              <Form.Group className="mb-2">
                <Form.Control
                  size="sm"
                  as="textarea"
                  rows={3}
                  placeholder="Content"
                  value={draft.content}
                  isInvalid={!!inlineErrors.content}
                  onChange={event => setDraft(prev => ({ ...prev, content: event.target.value }))}
                />
                {inlineErrors.content ? <div className="invalid-feedback d-block">{inlineErrors.content}</div> : null}
              </Form.Group>
              <Form.Group className="mb-2">
                <Form.Label className="small mb-1">Color</Form.Label>
                <Form.Control
                  type="color"
                  value={draft.color || DEFAULT_COLOR}
                  onChange={event => setDraft(prev => ({ ...prev, color: event.target.value }))}
                />
              </Form.Group>
              <TagInput
                id={`inline-tags-${note.id}`}
                label="Tags"
                tags={draft.tags}
                onChange={tags => setDraft(prev => ({ ...prev, tags }))}
                loadSuggestions={loadTagSuggestions}
                maxTags={5}
                errorMessage={TAG_FORMAT_MESSAGE}
              />
              {inlineErrors.tags ? <div className="invalid-feedback d-block">{inlineErrors.tags}</div> : null}
              <Form.Check
                type="switch"
                id={`inlinePinned-${note.id}`}
                label="Pin this note"
                checked={draft.pinned}
                onChange={event => setDraft(prev => ({ ...prev, pinned: event.target.checked }))}
                className="mb-3"
              />
              <div className="d-flex justify-content-end gap-2">
                <Button variant="outline-secondary" size="sm" onClick={() => setInlineMode(false)}>
                  <FontAwesomeIcon icon={faXmark} className="me-1" /> Cancel
                </Button>
                <Button variant="primary" size="sm" onClick={handleInlineSave} disabled={inlineSaving}>
                  {inlineSaving ? <Spinner size="sm" className="me-1" /> : <FontAwesomeIcon icon={faCheck} className="me-1" />}
                  Save
                </Button>
              </div>
            </div>
          ) : null}

          <div className="d-flex flex-column gap-1 text-muted small">
            {showOwner ? (
              <span>
                <FontAwesomeIcon icon={faUserShield} className="me-1" />
                Owner: {note.owner || '—'}
              </span>
            ) : null}
            <span>
              <FontAwesomeIcon icon={faUser} className="me-1" />
              Created by: {note.createdBy || '—'}
            </span>
            {note.lastModifiedBy ? (
              <span>
                <FontAwesomeIcon icon={faUser} className="me-1" />
                Updated by: {note.lastModifiedBy}
              </span>
            ) : null}
          </div>
          <div className="d-flex flex-column text-muted small gap-1">
            <div className="d-flex align-items-center gap-2 flex-wrap">
              <FontAwesomeIcon icon={faCalendar} className="me-1" />
              <span>Created:</span>
              <span className="text-nowrap">{meta.createdText.split(' ')[0]}</span>
              <span className="d-inline-flex align-items-center gap-1 text-nowrap">
                <FontAwesomeIcon icon={faClock} />
                {meta.createdText.split(' ')[1] || ''}
              </span>
            </div>
            <div className="d-flex align-items-center gap-2 flex-wrap">
              <FontAwesomeIcon icon={faCalendar} className="me-1" />
              <span>Updated:</span>
              <span className="text-nowrap">{meta.modifiedText.split(' ')[0]}</span>
              <span className="d-inline-flex align-items-center gap-1 text-nowrap">
                <FontAwesomeIcon icon={faClock} />
                {meta.modifiedText.split(' ')[1] || ''}
              </span>
            </div>
          </div>
          {view === 'trash' ? (
            <div className="d-flex gap-2 text-muted small">
              <span>
                <FontAwesomeIcon icon={faUser} className="me-1" />
                Deleted by: {note.deletedBy || '—'}
              </span>
            </div>
          ) : null}
          {view === 'trash' && meta.deletedText ? (
            <div className="d-flex text-muted small align-items-center gap-2 flex-wrap mt-1">
              <FontAwesomeIcon icon={faCalendar} className="me-1" />
              <span>Deleted:</span>
              <span className="text-nowrap">{meta.deletedText.split(' ')[0]}</span>
              <span className="d-inline-flex align-items-center gap-1 text-nowrap">
                <FontAwesomeIcon icon={faClock} />
                {meta.deletedText.split(' ')[1] || ''}
              </span>
            </div>
          ) : null}
        </Card.Body>
      </Card>
    </Col>
  );
}

export default function NotesPage() {
  const { pushToast } = useToasts();
  const { loading: authLoading, isAdmin, isAuthenticated } = useAuth();
  const [notes, setNotes] = useState<NoteDTO[]>([]);
  const [view, setView] = useState<NoteView>('active');
  const [loading, setLoading] = useState(false);
  const [alert, setAlert] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [sort, setSort] = useState('createdDate,desc');
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [selected, setSelected] = useState<Set<number>>(new Set());

  const [filterTags, setFilterTags] = useState<string[]>([]);
  const [filterColor, setFilterColor] = useState('');
  const [filterPinned, setFilterPinned] = useState('');
  const [appliedFilters, setAppliedFilters] = useState<{ tags: string[]; color: string; pinned: string }>({
    tags: [],
    color: '',
    pinned: '',
  });

  const [noteModalOpen, setNoteModalOpen] = useState(false);
  const [editingNote, setEditingNote] = useState<NoteDTO | null>(null);
  const {
    register: registerNote,
    handleSubmit: handleNoteSubmit,
    reset: resetNoteForm,
    control,
    formState: { errors: noteFormErrors, isSubmitting: noteSubmitting },
  } = useForm<NoteFormValues>({
    mode: 'onChange',
    defaultValues: {
      title: '',
      content: '',
      color: DEFAULT_COLOR,
      tags: [],
      pinned: false,
    },
  });

  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<NoteDTO | null>(null);
  const [deleteForeverModalOpen, setDeleteForeverModalOpen] = useState(false);
  const [deleteForeverTarget, setDeleteForeverTarget] = useState<NoteDTO | null>(null);
  const [emptyTrashModalOpen, setEmptyTrashModalOpen] = useState(false);
  const [bulkModalOpen, setBulkModalOpen] = useState(false);
  const [bulkAction, setBulkAction] = useState('');

  const [shareModalOpen, setShareModalOpen] = useState(false);
  const [shareNote, setShareNote] = useState<NoteDTO | null>(null);
  const [shareExpiry, setShareExpiry] = useState('24h');
  const [shareExpiresAt, setShareExpiresAt] = useState('');
  const [shareOneTime, setShareOneTime] = useState(false);
  const [shareSubmitting, setShareSubmitting] = useState(false);
  const [shareAlert, setShareAlert] = useState('');
  const [shareResult, setShareResult] = useState<ShareResult | null>(null);
  const [shareLinks, setShareLinks] = useState<ShareLinkDTO[]>([]);
  const [shareLinksPage, setShareLinksPage] = useState(0);
  const [shareLinksHasMore, setShareLinksHasMore] = useState(false);
  const [shareLinksLoading, setShareLinksLoading] = useState(false);

  const [revisionModalOpen, setRevisionModalOpen] = useState(false);
  const [revisionNote, setRevisionNote] = useState<NoteDTO | null>(null);
  const [revisions, setRevisions] = useState<NoteRevisionDTO[]>([]);
  const [revisionPage, setRevisionPage] = useState(0);
  const [revisionHasMore, setRevisionHasMore] = useState(false);
  const [revisionLoading, setRevisionLoading] = useState(false);
  const [revisionError, setRevisionError] = useState('');
  const [revisionTotal, setRevisionTotal] = useState(0);
  const [diffOpen, setDiffOpen] = useState<Set<number>>(new Set());

  const [ownerModalOpen, setOwnerModalOpen] = useState(false);
  const [ownerTarget, setOwnerTarget] = useState<NoteDTO | null>(null);
  const [ownerQuery, setOwnerQuery] = useState('');
  const [ownerSuggestions, setOwnerSuggestions] = useState<StoredUser[]>([]);
  const [ownerLoading, setOwnerLoading] = useState(false);
  const [ownerPage, setOwnerPage] = useState(0);
  const [ownerHasMore, setOwnerHasMore] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(search.trim()), 300);
    return () => clearTimeout(timer);
  }, [search]);

  useEffect(() => {
    setSelected(new Set<number>());
  }, [view]);

  useEffect(() => {
    setPage(0);
  }, [debouncedSearch, pageSize, sort, view, appliedFilters]);

  const loadNotes = useCallback(async () => {
    if (authLoading || !isAuthenticated) return;
    setLoading(true);
    setAlert('');
    try {
      const res = await Api.fetchNotes({
        view,
        page,
        size: pageSize,
        sort,
        query: debouncedSearch,
        tags: appliedFilters.tags,
        color: appliedFilters.color || undefined,
        pinned: appliedFilters.pinned === '' ? undefined : appliedFilters.pinned === 'true',
      });
      const content = res?.content || [];
      const meta = res?.page ?? res ?? {};
      setNotes(content);
      setTotalElements(meta.totalElements ?? content.length);
      setTotalPages(Math.max(meta.totalPages ?? 1, 1));
      setPage(meta.number ?? page);
      setSelected(prev => {
        const next = new Set<number>();
        content.forEach(note => {
          if (prev.has(note.id)) next.add(note.id);
        });
        return next;
      });
    } catch (err) {
      setAlert(messageFromError(err, 'Could not load notes.'));
    } finally {
      setLoading(false);
    }
  }, [authLoading, isAuthenticated, view, page, pageSize, sort, debouncedSearch, appliedFilters]);

  useEffect(() => {
    loadNotes();
  }, [loadNotes]);

  const loadTagSuggestions = useCallback(async (query: string) => Api.fetchTags(query), []);

  const handleApplyFilters = () => {
    setAppliedFilters({
      tags: filterTags,
      color: filterColor,
      pinned: filterPinned,
    });
  };

  const handleResetFilters = () => {
    setFilterTags([]);
    setFilterColor('');
    setFilterPinned('');
    setAppliedFilters({ tags: [], color: '', pinned: '' });
  };

  const toggleSelect = (id: number, checked: boolean) => {
    setSelected(prev => {
      const next = new Set<number>(prev);
      if (checked) {
        next.add(id);
      } else {
        next.delete(id);
      }
      return next;
    });
  };

  const toggleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelected(new Set<number>(notes.map(note => note.id)));
    } else {
      setSelected(new Set<number>());
    }
  };

  const openNewNote = () => {
    setEditingNote(null);
    resetNoteForm({
      title: '',
      content: '',
      color: DEFAULT_COLOR,
      tags: [],
      pinned: false,
    });
    setNoteModalOpen(true);
  };

  const openEditNote = (note: NoteDTO) => {
    setEditingNote(note);
    resetNoteForm({
      title: note.title || '',
      content: note.content || '',
      color: note.color || DEFAULT_COLOR,
      tags: normalizeTags(note.tags),
      pinned: !!note.pinned,
    });
    setNoteModalOpen(true);
  };

  const saveNote = handleNoteSubmit(async data => {
    try {
      const payload = {
        title: data.title.trim(),
        content: data.content.trim(),
        color: data.color || DEFAULT_COLOR,
        pinned: !!data.pinned,
        tags: normalizeTags(data.tags),
      };
      if (editingNote) {
        const updated = await Api.updateNote(editingNote.id, payload);
        setNotes(prev => prev.map(note => (note.id === editingNote.id ? updated : note)));
        pushToast('Note updated', 'success');
      } else {
        await Api.createNote(payload);
        pushToast('Note created', 'success');
      }
      setNoteModalOpen(false);
      loadNotes();
    } catch (err) {
      pushToast(messageFromError(err, 'Failed to save note.'), 'danger');
    }
  });

  const handleInlineSave = async (
    id: number,
    payload: { title: string; content: string; color: string; pinned: boolean; tags: string[] },
  ): Promise<NoteDTO | null> => {
    try {
      const updated = await Api.updateNote(id, payload);
      setNotes(prev => prev.map(note => (note.id === id ? updated : note)));
      pushToast('Note updated', 'success');
      return updated;
    } catch (err) {
      pushToast(messageFromError(err, 'Failed to save note.'), 'danger');
      return null;
    }
  };

  const handleNoteAction = async (action: NoteAction, note: NoteDTO) => {
    if (action === 'edit-modal') {
      openEditNote(note);
      return;
    }
    if (action === 'delete') {
      setDeleteTarget(note);
      setDeleteModalOpen(true);
      return;
    }
    if (action === 'delete-forever') {
      setDeleteForeverTarget(note);
      setDeleteForeverModalOpen(true);
      return;
    }
    if (action === 'restore') {
      try {
        await Api.restore(note.id);
        pushToast('Note restored', 'success');
        loadNotes();
      } catch (err) {
        pushToast(messageFromError(err, 'Failed to restore note.'), 'danger');
      }
      return;
    }
    if (action === 'copy') {
      try {
        await navigator.clipboard.writeText(note.content || '');
        pushToast('Note content copied', 'success');
      } catch {
        pushToast('Could not copy note. Copy manually.', 'warning');
      }
      return;
    }
    if (action === 'toggle-pin') {
      try {
        const updated = await Api.patchNote(note.id, { pinned: !note.pinned });
        setNotes(prev => prev.map(item => (item.id === note.id ? updated : item)));
      } catch (err) {
        pushToast(messageFromError(err, 'Failed to update pin.'), 'danger');
      }
      return;
    }
    if (action === 'share' || action === 'share-links') {
      openShareModal(note);
      return;
    }
    if (action === 'revisions') {
      openRevisionModal(note);
      return;
    }
    if (action === 'change-owner') {
      if (!isAdmin) return;
      setOwnerTarget(note);
      setOwnerQuery('');
      setOwnerSuggestions([]);
      setOwnerPage(0);
      setOwnerHasMore(false);
      setOwnerModalOpen(true);
      return;
    }
  };

  const confirmDelete = async () => {
    if (!deleteTarget) return;
    try {
      await Api.softDelete(deleteTarget.id);
      pushToast('Note deleted', 'success');
      loadNotes();
    } catch (err) {
      pushToast(messageFromError(err, 'Failed to delete note.'), 'danger');
    } finally {
      setDeleteModalOpen(false);
      setDeleteTarget(null);
    }
  };

  const confirmDeleteForever = async () => {
    if (!deleteForeverTarget) return;
    try {
      await Api.deletePermanent(deleteForeverTarget.id);
      pushToast('Note deleted permanently', 'success');
      loadNotes();
    } catch (err) {
      pushToast(messageFromError(err, 'Failed to delete note.'), 'danger');
    } finally {
      setDeleteForeverModalOpen(false);
      setDeleteForeverTarget(null);
    }
  };

  const confirmEmptyTrash = async () => {
    try {
      await Api.emptyTrash();
      pushToast('Trash emptied', 'success');
      loadNotes();
    } catch (err) {
      pushToast(messageFromError(err, 'Failed to empty trash.'), 'danger');
    } finally {
      setEmptyTrashModalOpen(false);
    }
  };

  const openBulkModal = (action: string) => {
    setBulkAction(action);
    setBulkModalOpen(true);
  };

  const confirmBulkAction = async () => {
    const ids = Array.from(selected);
    if (!ids.length) return;
    try {
      await Api.bulkAction({ action: bulkAction, ids });
      pushToast('Bulk action completed', 'success');
      setSelected(new Set<number>());
      loadNotes();
    } catch (err) {
      pushToast(messageFromError(err, 'Bulk action failed.'), 'danger');
    } finally {
      setBulkModalOpen(false);
    }
  };

  const openShareModal = (note: NoteDTO) => {
    setShareNote(note);
    setShareExpiry('24');
    setShareExpiresAt('');
    setShareOneTime(false);
    setShareAlert('');
    setShareResult(null);
    setShareLinks([]);
    setShareLinksPage(0);
    setShareLinksHasMore(false);
    setShareLinksLoading(false);
    setShareModalOpen(true);
    loadShareLinks(note, false);
  };

  const loadShareLinks = async (note: NoteDTO, append: boolean) => {
    if (!note || shareLinksLoading) return;
    setShareLinksLoading(true);
    try {
      const pageToLoad = append ? shareLinksPage + 1 : 0;
      const res = await Api.fetchShareLinks(note.id, pageToLoad, SHARE_LINKS_PAGE_SIZE);
      const content = res?.content ?? [];
      const meta = res.page ?? res;
      const totalPagesLocal = typeof meta.totalPages === 'number' ? meta.totalPages : 1;
      setShareLinks(prev => (append ? [...prev, ...content] : content));
      const nextPageNumber = typeof meta.number === 'number' ? meta.number : pageToLoad;
      setShareLinksPage(nextPageNumber);
      setShareLinksHasMore(nextPageNumber < totalPagesLocal - 1);
    } catch (err) {
      setShareAlert(messageFromError(err, 'Failed to load share links.'));
    } finally {
      setShareLinksLoading(false);
    }
  };

  const handleShareSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!shareNote) return;
    setShareSubmitting(true);
    setShareAlert('');
    try {
      let expiresAt = null;
      if (shareExpiry === 'custom') {
        const iso = toIsoString(shareExpiresAt);
        if (!iso) {
          setShareAlert('Custom expiry is required.');
          return;
        }
        expiresAt = iso;
      } else if (shareExpiry !== 'never') {
        const hours = Number.parseInt(shareExpiry, 10);
        if (!Number.isNaN(hours)) {
          expiresAt = new Date(Date.now() + hours * 60 * 60 * 1000).toISOString();
        }
      }
      const payload = {
        permission: 'READ',
        oneTime: !!shareOneTime,
        expiresAt,
      };
      const result = await Api.createShareLink(shareNote.id, payload);
      const token = result.token;
      if (!token) {
        setShareAlert('Share token is missing.');
        return;
      }
      const url = buildShareUrl(token);
      setShareResult({
        url,
        permission: typeof result.permission === 'string' && result.permission ? result.permission : 'READ',
        expiresAt: (result.expiresAt ?? null) as string | null,
        oneTime: !!result.oneTime,
      });
      pushToast('Share link created', 'success');
      loadShareLinks(shareNote, false);
    } catch (err) {
      setShareAlert(messageFromError(err, 'Failed to create share link.'));
    } finally {
      setShareSubmitting(false);
    }
  };

  const handleShareCopy = async (value?: string, isUrl = false) => {
    if (!value) return;
    const url = isUrl ? value : buildShareUrl(value);
    if (!url) return;
    try {
      await navigator.clipboard.writeText(url);
      pushToast('Share link copied', 'success');
    } catch {
      pushToast('Could not copy link. Copy manually.', 'warning');
    }
  };

  const handleShareRevoke = async (id: string | number) => {
    try {
      await Api.revokeShareLink(id);
      pushToast('Share link revoked', 'success');
      if (shareNote) {
        loadShareLinks(shareNote, false);
      }
    } catch (err) {
      pushToast(messageFromError(err, 'Failed to revoke share link.'), 'danger');
    }
  };

  const openRevisionModal = (note: NoteDTO) => {
    setRevisionNote(note);
    setRevisionModalOpen(true);
    setRevisions([]);
    setRevisionPage(0);
    setRevisionHasMore(false);
    setRevisionError('');
    setRevisionTotal(0);
    setDiffOpen(new Set<number>());
    loadRevisions(note, false);
  };

  const loadRevisions = async (note: NoteDTO, append: boolean) => {
    if (!note || revisionLoading) return;
    setRevisionLoading(true);
    setRevisionError('');
    try {
      const pageToLoad = append ? revisionPage + 1 : 0;
      const res = await Api.fetchRevisions(note.id, undefined, pageToLoad, REVISION_PAGE_SIZE);
      const content = res?.content ?? [];
      const meta = res.page ?? res;
      const totalPagesLocal = typeof meta.totalPages === 'number' ? meta.totalPages : 1;
      setRevisionTotal(typeof meta.totalElements === 'number' ? meta.totalElements : content.length);
      setRevisions(prev => (append ? [...prev, ...content] : content));
      const nextPageNumber = typeof meta.number === 'number' ? meta.number : pageToLoad;
      setRevisionPage(nextPageNumber);
      setRevisionHasMore(nextPageNumber < totalPagesLocal - 1);
    } catch (err) {
      setRevisionError(messageFromError(err, 'Failed to load revisions.'));
    } finally {
      setRevisionLoading(false);
    }
  };

  const restoreRevision = async (noteId: number, revisionId: number) => {
    try {
      await Api.restoreRevision(noteId, revisionId);
      pushToast('Revision restored', 'success');
      setRevisionModalOpen(false);
      loadNotes();
    } catch (err) {
      pushToast(messageFromError(err, 'Failed to restore revision.'), 'danger');
    }
  };

  const loadOwnerSuggestions = async (query: string, append: boolean) => {
    if (!query || ownerLoading) return;
    setOwnerLoading(true);
    try {
      const pageToLoad = append ? ownerPage + 1 : 0;
      const res = await Api.searchUsers(query, pageToLoad, OWNER_SEARCH_PAGE_SIZE);
      const content = res?.content ?? [];
      const meta = res.page ?? res;
      const totalPagesLocal = typeof meta.totalPages === 'number' ? meta.totalPages : 1;
      setOwnerSuggestions(prev => (append ? [...prev, ...content] : content));
      const nextPageNumber = typeof meta.number === 'number' ? meta.number : pageToLoad;
      setOwnerPage(nextPageNumber);
      setOwnerHasMore(nextPageNumber < totalPagesLocal - 1);
    } catch (err) {
      pushToast(messageFromError(err, 'Failed to search users.'), 'danger');
    } finally {
      setOwnerLoading(false);
    }
  };

  const submitOwnerChange = async () => {
    if (!ownerTarget) return;
    const owner = ownerQuery.trim();
    if (!owner) {
      pushToast('Owner is required.', 'warning');
      return;
    }
    try {
      await Api.changeOwner(ownerTarget.id, { owner });
      pushToast('Owner updated', 'success');
      setOwnerModalOpen(false);
      setOwnerTarget(null);
      loadNotes();
    } catch (err) {
      pushToast(messageFromError(err, 'Failed to change owner.'), 'danger');
    }
  };

  const totalLabel = view === 'trash' ? `Total (trash): ${totalElements}` : `Total: ${totalElements}`;
  const selectedCount = selected.size;
  const allSelected = notes.length > 0 && notes.every(note => selected.has(note.id));
  const showEmptyMessage = !loading && notes.length === 0 && !alert;
  const pageInfo = notes.length ? `Page ${totalPages ? page + 1 : 0} of ${totalPages}` : '';

  const paginationItems = useMemo(() => {
    if (totalPages <= 1) return [];
    const items = [];
    items.push(<Pagination.Prev key="prev" disabled={page === 0} onClick={() => setPage(Math.max(page - 1, 0))} />);
    for (let i = 0; i < totalPages; i += 1) {
      items.push(
        <Pagination.Item key={i} active={i === page} onClick={() => setPage(i)}>
          {i + 1}
        </Pagination.Item>,
      );
    }
    items.push(
      <Pagination.Next key="next" disabled={page >= totalPages - 1} onClick={() => setPage(Math.min(page + 1, totalPages - 1))} />,
    );
    return items;
  }, [page, totalPages]);

  return (
    <>
      <AppNavbar showSearch={true} search={search} onSearchChange={setSearch} onSearchClear={() => setSearch('')} />
      <header className="py-4 mb-4 shadow-sm bg-body-tertiary">
        <Container className="d-flex flex-column flex-md-row align-items-md-center justify-content-between">
          <div>
            <h1 className="h4 mb-1 text-body">Notes</h1>
            <p className="mb-0 text-muted">Create, edit, delete — audit-ready. Sign in to get a JWT; audit metadata uses your user.</p>
          </div>
          <div className="mt-3 mt-md-0 d-flex gap-2 flex-wrap">
            <Badge bg="primary-subtle" text="primary">
              JPA Auditing
            </Badge>
            <Badge bg="primary-subtle" text="primary">
              Liquibase
            </Badge>
            <Badge bg="primary-subtle" text="primary">
              Springdoc
            </Badge>
          </div>
        </Container>
      </header>
      <main className="bg-body-tertiary flex-grow-1">
        <Container className="pb-5">
          <Row className="g-4 align-items-start">
            <Col lg={3} className="order-1 order-lg-2 d-flex flex-column gap-2">
              <Button className="btn btn-primary shadow-sm px-3 py-2 w-100" onClick={openNewNote} disabled={view === 'trash'}>
                <FontAwesomeIcon icon={faPlus} className="me-1" /> New Note
              </Button>
            </Col>
            <Col lg={9} className="order-2 order-lg-1">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h2 className="h5 mb-0 d-flex align-items-center gap-2">
                  <FontAwesomeIcon icon={faLayerGroup} className="text-primary" />
                  <span>{view === 'trash' ? 'Trash' : 'All Notes'}</span>
                </h2>
                {notes.length ? <span className="text-muted small">{totalLabel}</span> : null}
              </div>
              <div className="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
                <div className="nav nav-tabs nav-tabs-sm shadow-sm">
                  <button
                    className={`nav-link d-inline-flex align-items-center gap-2 ${view === 'active' ? 'active' : ''}`}
                    onClick={() => setView('active')}
                    type="button"
                  >
                    <FontAwesomeIcon icon={faLayerGroup} />
                    <span>Active</span>
                  </button>
                  <button
                    className={`nav-link d-inline-flex align-items-center gap-2 ${view === 'trash' ? 'active' : ''}`}
                    onClick={() => setView('trash')}
                    type="button"
                  >
                    <FontAwesomeIcon icon={faTrashCan} />
                    <span>Trash</span>
                  </button>
                </div>
                {notes.length ? <div className="text-muted small">{pageInfo}</div> : null}
              </div>

              <Card className="shadow-sm border-0 mb-3">
                <Card.Body>
                  <div className="d-flex flex-wrap align-items-start gap-3">
                    <div className="d-flex flex-column gap-2 flex-grow-1" style={{ minWidth: 320 }}>
                      <Form.Label className="small text-muted mb-0">Filter by tags</Form.Label>
                      <TagInput
                        id="filter-tags"
                        tags={filterTags}
                        onChange={setFilterTags}
                        loadSuggestions={loadTagSuggestions}
                        maxTags={5}
                        errorMessage={TAG_FORMAT_MESSAGE}
                        className="mb-0"
                      />
                    </div>
                    <div className="d-flex flex-column gap-2 flex-shrink-0" style={{ minWidth: 180 }}>
                      <Form.Label className="small text-muted mb-0">Color</Form.Label>
                      <div className="d-flex align-items-center gap-2">
                        <Form.Control
                          type="color"
                          value={filterColor || DEFAULT_COLOR}
                          onChange={event => setFilterColor(event.target.value)}
                          style={{ width: 48, height: 38 }}
                        />
                        <Button variant="outline-secondary" size="sm" onClick={() => setFilterColor('')}>
                          <FontAwesomeIcon icon={faXmark} className="me-1" /> Clear
                        </Button>
                      </div>
                    </div>
                    <div className="d-flex flex-column gap-2 flex-shrink-0" style={{ minWidth: 160 }}>
                      <Form.Label className="small text-muted mb-0">Pinned</Form.Label>
                      <Form.Select size="sm" value={filterPinned} onChange={event => setFilterPinned(event.target.value)}>
                        <option value="">All</option>
                        <option value="true">Pinned</option>
                        <option value="false">Unpinned</option>
                      </Form.Select>
                    </div>
                  </div>
                  <div className="d-flex gap-2 flex-wrap mt-3">
                    <Button variant="outline-secondary" size="sm" onClick={handleResetFilters}>
                      <FontAwesomeIcon icon={faRotateLeft} className="me-1" /> Reset
                    </Button>
                    <Button variant="primary" size="sm" onClick={handleApplyFilters}>
                      <FontAwesomeIcon icon={faFilter} className="me-1" /> Apply
                    </Button>
                  </div>
                </Card.Body>
              </Card>

              <div className="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
                <div className="d-flex flex-wrap align-items-center gap-3">
                  <div className="d-flex align-items-center gap-2">
                    <Form.Label className="text-muted small mb-0 text-nowrap">Page size</Form.Label>
                    <Form.Select
                      size="sm"
                      value={pageSize}
                      onChange={event => setPageSize(Number(event.target.value))}
                      style={{ width: 'auto' }}
                    >
                      <option value={5}>5</option>
                      <option value={10}>10</option>
                      <option value={25}>25</option>
                    </Form.Select>
                  </div>
                  <div className="d-flex align-items-center gap-2">
                    <Form.Label className="text-muted small mb-0 text-nowrap">Sort</Form.Label>
                    <Form.Select size="sm" value={sort} onChange={event => setSort(event.target.value)} style={{ width: 'auto' }}>
                      <option value="createdDate,desc">Created (newest)</option>
                      <option value="createdDate,asc">Created (oldest)</option>
                      <option value="lastModifiedDate,desc">Updated (newest)</option>
                      <option value="lastModifiedDate,asc">Updated (oldest)</option>
                      <option value="title,asc">Title A-Z</option>
                      <option value="title,desc">Title Z-A</option>
                    </Form.Select>
                  </div>
                </div>
                {view === 'trash' && notes.length ? (
                  <Button variant="outline-danger" size="sm" onClick={() => setEmptyTrashModalOpen(true)}>
                    <FontAwesomeIcon icon={faTrash} className="me-1" /> Empty Trash
                  </Button>
                ) : null}
              </div>

              {notes.length ? (
                <div className="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
                  <Form.Check
                    type="checkbox"
                    checked={allSelected}
                    label="Select all on page"
                    onChange={event => toggleSelectAll(event.target.checked)}
                  />
                  <div className="d-flex flex-wrap gap-2 align-items-center">
                    {selectedCount ? <span className="text-muted small">Selected: {selectedCount}</span> : null}
                    {view === 'trash' ? (
                      <>
                        <Button variant="outline-success" size="sm" disabled={!selectedCount} onClick={() => openBulkModal('RESTORE')}>
                          <FontAwesomeIcon icon={faRotateLeft} className="me-1" /> Restore
                        </Button>
                        <Button
                          variant="outline-danger"
                          size="sm"
                          disabled={!selectedCount}
                          onClick={() => openBulkModal('DELETE_FOREVER')}
                        >
                          <FontAwesomeIcon icon={faTrash} className="me-1" /> Delete permanently
                        </Button>
                      </>
                    ) : (
                      <Button variant="outline-danger" size="sm" disabled={!selectedCount} onClick={() => openBulkModal('DELETE_SOFT')}>
                        <FontAwesomeIcon icon={faTrash} className="me-1" /> Delete
                      </Button>
                    )}
                  </div>
                </div>
              ) : null}

              {alert ? <Alert variant="danger">{alert}</Alert> : null}
              {loading ? (
                <div className="text-center py-3">
                  <Spinner animation="border" variant="primary" />
                </div>
              ) : null}
              {showEmptyMessage ? (
                <ListGroup>
                  <ListGroup.Item className="text-muted small d-flex align-items-center gap-2">
                    <FontAwesomeIcon icon={view === 'trash' ? faTrashCan : faNoteSticky} />
                    <span>
                      {debouncedSearch
                        ? 'No notes match your search.'
                        : view === 'trash'
                          ? 'Trash is empty.'
                          : 'No notes found. Create a new one to get started.'}
                    </span>
                  </ListGroup.Item>
                </ListGroup>
              ) : null}

              <Row className="g-3">
                {notes.map(note => (
                  <NoteCard
                    key={note.id}
                    note={note}
                    view={view}
                    showOwner={isAdmin}
                    selected={selected.has(note.id)}
                    onSelectToggle={toggleSelect}
                    onAction={handleNoteAction}
                    loadTagSuggestions={loadTagSuggestions}
                    onInlineSave={handleInlineSave}
                  />
                ))}
              </Row>

              {totalPages > 1 && notes.length ? (
                <div className="d-flex justify-content-center mt-3">
                  <Pagination>{paginationItems}</Pagination>
                </div>
              ) : null}
            </Col>
          </Row>
        </Container>
      </main>
      <Footer />

      <Modal show={noteModalOpen} onHide={() => setNoteModalOpen(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>{editingNote ? 'Edit Note' : 'New Note'}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form onSubmit={saveNote} noValidate>
            <Form.Group className="mb-3">
              <Form.Label>Title</Form.Label>
              <Form.Control
                type="text"
                placeholder="Enter a concise title"
                isInvalid={!!noteFormErrors.title}
                {...registerNote('title', {
                  validate: value => {
                    const trimmed = value.trim();
                    if (!trimmed) return 'This field is required.';
                    if (trimmed.length < 3 || trimmed.length > 255) {
                      return 'Size must be between 3 and 255 characters.';
                    }
                    return true;
                  },
                })}
              />
              {noteFormErrors.title ? <Form.Control.Feedback type="invalid">{noteFormErrors.title.message}</Form.Control.Feedback> : null}
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Content</Form.Label>
              <Form.Control
                as="textarea"
                rows={4}
                placeholder="Describe your note"
                isInvalid={!!noteFormErrors.content}
                {...registerNote('content', {
                  validate: value => {
                    const trimmed = value.trim();
                    if (!trimmed) return 'This field is required.';
                    if (trimmed.length < 10 || trimmed.length > 1024) {
                      return 'Size must be between 10 and 1024 characters.';
                    }
                    return true;
                  },
                })}
              />
              {noteFormErrors.content ? (
                <Form.Control.Feedback type="invalid">{noteFormErrors.content.message}</Form.Control.Feedback>
              ) : null}
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Color</Form.Label>
              <Form.Control type="color" {...registerNote('color')} />
            </Form.Group>
            <Controller
              name="tags"
              control={control}
              rules={{
                validate: value => {
                  const normalized = normalizeTags(value);
                  if (normalized.length > 5) return 'Up to 5 tags allowed.';
                  if (normalized.some(tag => tag.length < 1 || tag.length > 30)) return 'Tags must be 1-30 characters.';
                  if (normalized.some(tag => !TAG_PATTERN.test(tag))) return TAG_FORMAT_MESSAGE;
                  return true;
                },
              }}
              render={({ field }) => (
                <TagInput
                  id="note-tags"
                  label="Tags"
                  tags={field.value}
                  onChange={field.onChange}
                  loadSuggestions={loadTagSuggestions}
                  maxTags={5}
                  errorMessage={TAG_FORMAT_MESSAGE}
                  isInvalid={!!noteFormErrors.tags}
                  externalError={noteFormErrors.tags?.message}
                />
              )}
            />
            <Form.Check type="switch" id="note-pinned" label="Pin this note" className="mb-3" {...registerNote('pinned')} />
            <div className="d-flex justify-content-end">
              <Button type="submit" disabled={noteSubmitting}>
                {noteSubmitting ? <Spinner size="sm" className="me-2" /> : <FontAwesomeIcon icon={faCheck} className="me-2" />}
                {editingNote ? 'Save' : 'Create'}
              </Button>
            </div>
          </Form>
        </Modal.Body>
      </Modal>

      <Modal show={deleteModalOpen} onHide={() => setDeleteModalOpen(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>Delete Note</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p>Are you sure you want to delete this note?</p>
          <div className="d-flex justify-content-end gap-2">
            <Button variant="outline-secondary" onClick={() => setDeleteModalOpen(false)}>
              <FontAwesomeIcon icon={faXmark} className="me-1" /> Cancel
            </Button>
            <Button variant="danger" onClick={confirmDelete}>
              <FontAwesomeIcon icon={faTrash} className="me-1" /> Delete
            </Button>
          </div>
        </Modal.Body>
      </Modal>

      <Modal show={deleteForeverModalOpen} onHide={() => setDeleteForeverModalOpen(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>Delete Permanently</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p>This action cannot be undone. Delete this note permanently?</p>
          <div className="d-flex justify-content-end gap-2">
            <Button variant="outline-secondary" onClick={() => setDeleteForeverModalOpen(false)}>
              <FontAwesomeIcon icon={faXmark} className="me-1" /> Cancel
            </Button>
            <Button variant="danger" onClick={confirmDeleteForever}>
              <FontAwesomeIcon icon={faTrash} className="me-1" /> Delete
            </Button>
          </div>
        </Modal.Body>
      </Modal>

      <Modal show={emptyTrashModalOpen} onHide={() => setEmptyTrashModalOpen(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>Empty Trash</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p>This will permanently delete all items in Trash. Continue?</p>
          <div className="d-flex justify-content-end gap-2">
            <Button variant="outline-secondary" onClick={() => setEmptyTrashModalOpen(false)}>
              <FontAwesomeIcon icon={faXmark} className="me-1" /> Cancel
            </Button>
            <Button variant="danger" onClick={confirmEmptyTrash}>
              <FontAwesomeIcon icon={faTrash} className="me-1" /> Empty Trash
            </Button>
          </div>
        </Modal.Body>
      </Modal>

      <Modal show={bulkModalOpen} onHide={() => setBulkModalOpen(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>Confirm bulk action</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p>Apply action to selected items?</p>
          <div className="d-flex justify-content-end gap-2">
            <Button variant="outline-secondary" onClick={() => setBulkModalOpen(false)}>
              <FontAwesomeIcon icon={faXmark} className="me-1" /> Cancel
            </Button>
            <Button variant="danger" onClick={confirmBulkAction}>
              <FontAwesomeIcon icon={faCheck} className="me-1" /> Confirm
            </Button>
          </div>
        </Modal.Body>
      </Modal>

      <Modal show={shareModalOpen} onHide={() => setShareModalOpen(false)} centered size="lg">
        <Modal.Header closeButton>
          <Modal.Title className="d-flex align-items-center gap-2">
            <FontAwesomeIcon icon={faLink} className="text-primary" />
            <span>Share Note</span>
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {shareAlert ? <Alert variant="danger">{shareAlert}</Alert> : null}
          <p className="text-muted small mb-3">{shareNote?.title || ''}</p>
          <Form onSubmit={handleShareSubmit}>
            <input type="hidden" value="READ" />
            <Form.Group className="mb-3">
              <Form.Label>Expires</Form.Label>
              <Form.Select value={shareExpiry} onChange={event => setShareExpiry(event.target.value)}>
                <option value="1">1 hour</option>
                <option value="24">24 hours</option>
                <option value="72">3 days</option>
                <option value="168">7 days</option>
                <option value="720">30 days</option>
                <option value="custom">Custom…</option>
                <option value="never">No expiry</option>
              </Form.Select>
              {shareExpiry === 'custom' ? (
                <Form.Control
                  type="datetime-local"
                  className="mt-2"
                  value={shareExpiresAt}
                  onChange={event => setShareExpiresAt(event.target.value)}
                />
              ) : null}
              <div className="form-text">Default is 24 hours unless custom or no-expiry selected.</div>
            </Form.Group>
            <Form.Check
              type="switch"
              label="One-time use"
              checked={shareOneTime}
              onChange={event => setShareOneTime(event.target.checked)}
              className="mb-3"
            />
            <div className="d-flex justify-content-end gap-2">
              <Button variant="outline-secondary" onClick={() => setShareModalOpen(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={shareSubmitting}>
                {shareSubmitting ? <Spinner size="sm" className="me-2" /> : null}
                Create link
              </Button>
            </div>
          </Form>
          {shareResult ? (
            <div className="mt-3">
              <Form.Group className="mb-2">
                <Form.Label>Share link</Form.Label>
                <InputGroup>
                  <Form.Control value={shareResult.url} readOnly />
                  <Button variant="outline-secondary" onClick={() => handleShareCopy(shareResult.url, true)}>
                    <FontAwesomeIcon icon={faCopy} className="me-1" /> Copy
                  </Button>
                </InputGroup>
              </Form.Group>
              <div className="d-flex align-items-center gap-2">
                <Badge bg="success-subtle" text="success">
                  {shareResult.permission}
                </Badge>
                <span className="text-muted small">
                  {shareResult.expiresAt ? `Expires ${formatDate(shareResult.expiresAt)}` : 'No expiry'}
                </span>
                {shareResult.oneTime ? (
                  <Badge bg="secondary-subtle" text="secondary">
                    One-time
                  </Badge>
                ) : null}
              </div>
            </div>
          ) : null}
          <div className="mt-4">
            <div className="d-flex align-items-center gap-2 mb-2">
              <span className="spinner-border spinner-border-sm text-secondary d-none" aria-hidden="true" />
            </div>
            {shareLinks.length === 0 && !shareLinksLoading ? <div className="text-muted small">No share links yet.</div> : null}
            <ListGroup style={{ maxHeight: '36rem', overflowY: 'auto' }}>
              {shareLinks.map(link => (
                <ListGroup.Item key={link.id} className="d-flex justify-content-between align-items-center flex-wrap gap-2">
                  <div>
                    <div className="fw-semibold text-primary">
                      {link.token ? `${link.token.slice(0, 6)}…${link.token.slice(-4)}` : `#${link.id}`}
                    </div>
                    <div className="text-muted small">
                      {link.revoked ? 'Revoked' : link.expired ? 'Expired' : 'Active'} ·{' '}
                      {link.expiresAt ? formatDate(link.expiresAt) : 'No expiry'}
                      {link.oneTime ? ' · One-time' : ''}
                    </div>
                  </div>
                  <div className="d-flex gap-2">
                    <Button variant="outline-secondary" size="sm" onClick={() => handleShareCopy(link.token)} disabled={link.revoked}>
                      <FontAwesomeIcon icon={faCopy} className="me-1" /> Copy
                    </Button>
                    <Button variant="outline-danger" size="sm" onClick={() => handleShareRevoke(link.id)} disabled={link.revoked}>
                      <FontAwesomeIcon icon={faTrash} className="me-1" /> Revoke
                    </Button>
                  </div>
                </ListGroup.Item>
              ))}
            </ListGroup>
            <div className="d-flex justify-content-center mt-2">
              {shareLinksHasMore ? (
                <Button
                  variant="outline-secondary"
                  size="sm"
                  onClick={() => shareNote && loadShareLinks(shareNote, true)}
                  disabled={shareLinksLoading}
                >
                  {shareLinksLoading ? <Spinner size="sm" className="me-1" /> : null}
                  Load more
                </Button>
              ) : null}
            </div>
          </div>
        </Modal.Body>
      </Modal>

      <Modal show={revisionModalOpen} onHide={() => setRevisionModalOpen(false)} centered size="lg">
        <Modal.Header closeButton>
          <Modal.Title className="d-flex align-items-center gap-2">
            <FontAwesomeIcon icon={faClockRotateLeft} className="text-primary" />
            <span>Revisions</span>
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {revisionError ? <Alert variant="danger">{revisionError}</Alert> : null}
          {revisionLoading && revisions.length === 0 ? (
            <div className="d-flex justify-content-center py-3">
              <Spinner animation="border" variant="primary" />
            </div>
          ) : null}
          <ListGroup>
            {revisions.length === 0 && !revisionLoading ? <ListGroup.Item className="text-muted">No revisions yet.</ListGroup.Item> : null}
            {revisions.map((rev, idx) => {
              const noteData = rev.note;
              const localNumber = revisionTotal ? revisionTotal - idx : idx + 1;
              const showDiff = diffOpen.has(rev.revision);
              const prev = revisions[idx + 1];
              const additionsOnly = !prev;
              const titleDiff = renderDiffSpans(diffLinesDetailed(prev?.note?.title ?? '', noteData?.title ?? ''), additionsOnly);
              const contentDiff = renderDiffSpans(diffLinesDetailed(prev?.note?.content ?? '', noteData?.content ?? ''), additionsOnly);
              return (
                <ListGroup.Item key={rev.revision}>
                  <div className="d-flex flex-column flex-md-row justify-content-between gap-3">
                    <div className="flex-grow-1">
                      <div className="d-flex align-items-center gap-2 flex-wrap mb-1">
                        <Badge bg="primary-subtle" text="primary">
                          v{localNumber}
                        </Badge>
                        <Badge bg="secondary-subtle" text="secondary" className="text-uppercase">
                          {rev.revisionType || 'N/A'}
                        </Badge>
                        <span className="text-muted small">
                          <FontAwesomeIcon icon={faClock} className="me-1" />
                          {formatDate(rev.revisionDate) || '—'}
                        </span>
                        <span className="text-muted small">
                          <FontAwesomeIcon icon={faUser} className="me-1" />
                          {rev.auditor || 'unknown'}
                        </span>
                        {noteData?.color ? (
                          <Badge bg="body-secondary" text="body" className="border" style={{ borderColor: noteData.color }}>
                            <FontAwesomeIcon icon={faCircle} style={{ color: noteData.color }} />
                          </Badge>
                        ) : null}
                        <Badge bg="warning-subtle" text="warning">
                          {noteData?.pinned ? 'Pinned' : 'Unpinned'}
                        </Badge>
                      </div>
                      <div className="fw-semibold">{noteData?.title || '(no title)'}</div>
                      <div className="text-muted small">{noteData?.content || ''}</div>
                      {normalizeTags(noteData?.tags).length ? (
                        <div className="d-flex flex-wrap gap-1 mt-1">
                          {normalizeTags(noteData?.tags).map(tag => (
                            <Badge key={tag} bg="secondary-subtle" text="secondary">
                              {tag}
                            </Badge>
                          ))}
                        </div>
                      ) : null}
                    </div>
                    <div className="d-flex flex-column align-items-end gap-2">
                      <Button
                        variant="outline-secondary"
                        size="sm"
                        onClick={() =>
                          setDiffOpen(prev => {
                            const next = new Set<number>(prev);
                            if (next.has(rev.revision)) {
                              next.delete(rev.revision);
                            } else {
                              next.add(rev.revision);
                            }
                            return next;
                          })
                        }
                      >
                        <FontAwesomeIcon icon={faBars} className="me-1" /> {showDiff ? 'Hide diff' : 'Diff'}
                      </Button>
                      <Button
                        variant="outline-primary"
                        size="sm"
                        onClick={() => {
                          if (revisionNote) {
                            restoreRevision(revisionNote.id, rev.revision);
                          }
                        }}
                      >
                        <FontAwesomeIcon icon={faRotateLeft} className="me-1" /> Restore
                      </Button>
                    </div>
                  </div>
                  {showDiff ? (
                    <div className="mt-3 border rounded p-3 bg-body-secondary">
                      <div className="fw-semibold mb-2">Title</div>
                      <div className="d-flex flex-wrap gap-2">{titleDiff}</div>
                      <div className="fw-semibold mb-2 mt-3">Content</div>
                      <div className="d-flex flex-column gap-2">{contentDiff}</div>
                    </div>
                  ) : null}
                </ListGroup.Item>
              );
            })}
          </ListGroup>
          {revisionHasMore ? (
            <div className="d-flex justify-content-center mt-3">
              <Button
                variant="outline-secondary"
                size="sm"
                onClick={() => revisionNote && loadRevisions(revisionNote, true)}
                disabled={revisionLoading}
              >
                {revisionLoading ? <Spinner size="sm" className="me-1" /> : null}
                Load more
              </Button>
            </div>
          ) : null}
        </Modal.Body>
      </Modal>

      <Modal show={ownerModalOpen} onHide={() => setOwnerModalOpen(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title className="d-flex align-items-center gap-2">
            <FontAwesomeIcon icon={faUserGear} className="text-primary" />
            <span>Change owner</span>
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form>
            <Form.Label>New owner</Form.Label>
            <InputGroup className="mb-2">
              <Form.Control
                value={ownerQuery}
                onChange={event => {
                  const next = event.target.value;
                  setOwnerQuery(next);
                  if (next.trim().length >= 2) {
                    loadOwnerSuggestions(next.trim(), false);
                  } else {
                    setOwnerSuggestions([]);
                  }
                }}
                placeholder="Search username"
              />
            </InputGroup>
            <ListGroup className="mb-2">
              {ownerSuggestions.map(user => {
                const login = user.login ?? user.username;
                if (!login) return null;
                return (
                  <ListGroup.Item key={login} action onClick={() => setOwnerQuery(login)}>
                    {login}
                  </ListGroup.Item>
                );
              })}
            </ListGroup>
            {ownerHasMore ? (
              <div className="d-grid">
                <Button
                  variant="outline-secondary"
                  size="sm"
                  onClick={() => loadOwnerSuggestions(ownerQuery.trim(), true)}
                  disabled={ownerLoading}
                >
                  {ownerLoading ? <Spinner size="sm" className="me-1" /> : null}
                  Load more
                </Button>
              </div>
            ) : null}
            <div className="form-text text-muted">Type to search and pick a user to set as owner.</div>
          </Form>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-secondary" onClick={() => setOwnerModalOpen(false)}>
            <FontAwesomeIcon icon={faXmark} className="me-1" /> Cancel
          </Button>
          <Button variant="primary" onClick={submitOwnerChange}>
            <FontAwesomeIcon icon={faCheck} className="me-1" /> Change owner
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  );
}
