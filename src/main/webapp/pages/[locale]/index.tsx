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
import { Controller, useForm, type Control, type FieldErrors, type UseFormRegister } from 'react-hook-form';
import { useTranslation } from 'next-i18next';
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
import AppNavbar from '@components/AppNavbar';
import Footer from '@components/Footer';
import TagInput from '@components/TagInput';
import { useToasts } from '@components/ToastProvider';
import Api from '@lib/api';
import { diffLinesDetailed, diffTypes } from '@lib/diff';
import type { DiffOp } from '@lib/diff';
import { formatDate, toIsoString } from '@lib/format';
import { makeStaticProps } from '@lib/getStatic';
import { getLocation } from '@lib/window';
import useAuth from '@lib/useAuth';
import type { NoteDTO, NoteRevisionDTO, ShareLinkDTO, StoredUser, TagDTO } from '@lib/types';

const TAG_PATTERN = /^[A-Za-z0-9_-]{1,30}$/;
const DEFAULT_COLOR = '#2563eb';
const SHARE_LINKS_PAGE_SIZE = 3;
const REVISION_PAGE_SIZE = 5;
const OWNER_SEARCH_PAGE_SIZE = 5;

type TranslateFn = (key: string, options?: Record<string, unknown>) => string;
type IconProp = Parameters<typeof FontAwesomeIcon>[0]['icon'];

function replaceNoteById(notes: NoteDTO[], id: number, updated: NoteDTO): NoteDTO[] {
  const next = [...notes];
  const idx = next.findIndex((note) => note.id === id);
  if (idx !== -1) {
    next[idx] = updated;
  }
  return next;
}

function tagLabel(tag: unknown): string {
  if (tag == null) return '';
  if (typeof tag === 'string') return tag;
  if (typeof tag === 'number' || typeof tag === 'boolean' || typeof tag === 'bigint') return String(tag);
  if (typeof tag === 'symbol') return tag.description ?? '';
  if (typeof tag === 'object') {
    const obj = tag as TagDTO;
    return (typeof obj.name === 'string' && obj.name) || (typeof obj.label === 'string' && obj.label) || '';
  }
  return '';
}

function normalizeTags(tags: Array<string | TagDTO> | null | undefined): string[] {
  return (tags || []).map(tagLabel).filter((tag) => tag && tag.trim().length > 0);
}

function validateNotePayload(
  t: TranslateFn,
  payload: { title?: string; content?: string; tags?: Array<string | TagDTO> | null },
): Record<string, string> {
  const errors: Record<string, string> = {};
  const title = String(payload.title || '').trim();
  const content = String(payload.content || '').trim();
  if (!title) {
    errors.title = t('validation.required');
  } else if (title.length < 3 || title.length > 255) {
    errors.title = t('notes.validation.title.size');
  }
  if (!content) {
    errors.content = t('validation.required');
  } else if (content.length < 10 || content.length > 1024) {
    errors.content = t('notes.validation.content.size');
  }
  const tags = normalizeTags(payload.tags as Array<string | TagDTO> | undefined);
  if (tags.length > 5) {
    errors.tags = t('notes.validation.tags.max');
  } else if (tags.some((tag) => tag.length < 1 || tag.length > 30)) {
    errors.tags = t('notes.validation.tags.size');
  } else if (tags.some((tag) => !TAG_PATTERN.test(tag))) {
    errors.tags = t('notes.validation.tags.format');
  }
  return errors;
}

function buildShareUrl(token: string): string {
  const origin = getLocation()?.origin;
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

type NoteCardProps = Readonly<{
  note: NoteDTO;
  view: NoteView;
  showOwner: boolean;
  selected: boolean;
  onSelect: (noteId: number) => void;
  onDeselect: (noteId: number) => void;
  onAction: (action: NoteAction, note: NoteDTO) => void | Promise<void>;
  loadTagSuggestions: (query: string) => Promise<string[]>;
  onInlineSave: (
    noteId: number,
    payload: { title: string; content: string; color: string; pinned: boolean; tags: string[] },
  ) => Promise<NoteDTO | null>;
}>;

type DateTimeText = Readonly<{ date: string; time: string }>;

function splitDateTimeText(text: string): DateTimeText {
  const trimmed = String(text || '').trim();
  if (!trimmed) return { date: '', time: '' };
  const firstSpace = trimmed.indexOf(' ');
  if (firstSpace === -1) return { date: trimmed, time: '' };
  return { date: trimmed.slice(0, firstSpace), time: trimmed.slice(firstSpace + 1).trim() };
}

function NoteTagBadges({ tags }: Readonly<{ tags: string[] }>) {
  if (!tags.length) return null;
  return (
    <div className="d-flex flex-wrap gap-1 mt-1">
      {tags.map((tag) => (
        <Badge key={tag} bg="secondary-subtle" text="secondary">
          {tag}
        </Badge>
      ))}
    </div>
  );
}

function NoteSummary({ note, tags }: Readonly<{ note: NoteDTO; tags: string[] }>) {
  const title = note.title || '';
  const content = note.content || '';
  const pinned = !!note.pinned;
  const color = note.color || '';

  return (
    <>
      <div className="d-flex align-items-center gap-2">
        <div className="fw-bold text-primary mb-0">{title}</div>
        {pinned && <FontAwesomeIcon icon={faThumbtack} className="text-warning" />}
        {color && (
          <Badge bg="body-secondary" text="body" className="border" style={{ borderColor: color }}>
            <FontAwesomeIcon icon={faCircle} style={{ color }} />
          </Badge>
        )}
      </div>
      <div className="text-muted small">{content}</div>
      <NoteTagBadges tags={tags} />
    </>
  );
}

function NoteActions({
  view,
  note,
  showOwner,
  onAction,
  onInlineToggle,
  t,
}: Readonly<{
  view: NoteView;
  note: NoteDTO;
  showOwner: boolean;
  onAction: (action: NoteAction, note: NoteDTO) => void | Promise<void>;
  onInlineToggle: () => void;
  t: TranslateFn;
}>) {
  if (view === 'trash') {
    return (
      <div className="d-flex flex-wrap gap-1 justify-content-end">
        <Button variant="success" size="sm" onClick={() => onAction('restore', note)} title={t('notes.actions.restore')}>
          <FontAwesomeIcon icon={faRotateLeft} />
        </Button>
        <Button variant="outline-secondary" size="sm" onClick={() => onAction('copy', note)} title={t('notes.actions.copyContent')}>
          <FontAwesomeIcon icon={faCopy} />
        </Button>
        {showOwner && (
          <Button
            variant="outline-secondary"
            size="sm"
            onClick={() => onAction('change-owner', note)}
            title={t('notes.actions.changeOwner')}
          >
            <FontAwesomeIcon icon={faUserGear} />
          </Button>
        )}
        <Button variant="outline-info" size="sm" onClick={() => onAction('revisions', note)} title={t('notes.actions.revisions')}>
          <FontAwesomeIcon icon={faClockRotateLeft} />
        </Button>
        <Button
          variant="outline-danger"
          size="sm"
          onClick={() => onAction('delete-forever', note)}
          title={t('notes.actions.deletePermanently')}
        >
          <FontAwesomeIcon icon={faTrash} />
        </Button>
      </div>
    );
  }

  const pinned = !!note.pinned;

  return (
    <div className="d-grid gap-1" style={{ gridTemplateColumns: 'repeat(3, 32px)' }}>
      <Button
        variant="outline-warning"
        size="sm"
        style={{ width: 32, height: 32 }}
        onClick={() => onAction('toggle-pin', note)}
        title={pinned ? t('notes.actions.unpin') : t('notes.actions.pin')}
      >
        <FontAwesomeIcon icon={faThumbtack} className={pinned ? '' : 'opacity-50'} />
      </Button>
      <Button
        variant="outline-primary"
        size="sm"
        style={{ width: 32, height: 32 }}
        onClick={() => onAction('edit-modal', note)}
        title={t('notes.actions.editInModal')}
      >
        <FontAwesomeIcon icon={faPenToSquare} />
      </Button>
      <Button
        variant="outline-secondary"
        size="sm"
        style={{ width: 32, height: 32 }}
        onClick={onInlineToggle}
        title={t('notes.actions.inlineEdit')}
      >
        <FontAwesomeIcon icon={faPen} />
      </Button>
      <Button
        variant="outline-secondary"
        size="sm"
        style={{ width: 32, height: 32 }}
        onClick={() => onAction('copy', note)}
        title={t('notes.actions.copyContent')}
      >
        <FontAwesomeIcon icon={faCopy} />
      </Button>
      <Button
        variant="outline-secondary"
        size="sm"
        style={{ width: 32, height: 32 }}
        onClick={() => onAction('share-links', note)}
        title={t('notes.actions.existingLinks')}
      >
        <FontAwesomeIcon icon={faLink} />
      </Button>
      <Button
        variant="outline-secondary"
        size="sm"
        style={{ width: 32, height: 32 }}
        onClick={() => onAction('share', note)}
        title={t('notes.actions.createShareLink')}
      >
        <FontAwesomeIcon icon={faShareFromSquare} />
      </Button>
      {showOwner && (
        <Button
          variant="outline-secondary"
          size="sm"
          style={{ width: 32, height: 32 }}
          onClick={() => onAction('change-owner', note)}
          title={t('notes.actions.changeOwner')}
        >
          <FontAwesomeIcon icon={faUserGear} />
        </Button>
      )}
      <Button
        variant="outline-info"
        size="sm"
        style={{ width: 32, height: 32 }}
        onClick={() => onAction('revisions', note)}
        title={t('notes.actions.revisions')}
      >
        <FontAwesomeIcon icon={faClockRotateLeft} />
      </Button>
      <Button
        variant="outline-danger"
        size="sm"
        style={{ width: 32, height: 32 }}
        onClick={() => onAction('delete', note)}
        title={t('notes.actions.delete')}
      >
        <FontAwesomeIcon icon={faTrash} />
      </Button>
    </div>
  );
}

function NoteInlineEditor({
  noteId,
  draft,
  inlineErrors,
  inlineSaving,
  onDraftChange,
  loadTagSuggestions,
  onCancel,
  onSave,
  t,
}: Readonly<{
  noteId: number;
  draft: NoteDraft;
  inlineErrors: Record<string, string>;
  inlineSaving: boolean;
  onDraftChange: (draft: NoteDraft) => void;
  loadTagSuggestions: (query: string) => Promise<string[]>;
  onCancel: () => void;
  onSave: () => void;
  t: TranslateFn;
}>) {
  return (
    <div className="mt-2 border-top pt-2">
      <Form.Group className="mb-2">
        <Form.Control
          size="sm"
          type="text"
          placeholder={t('notes.inline.title.placeholder')}
          value={draft.title}
          isInvalid={!!inlineErrors.title}
          onChange={(event) => onDraftChange({ ...draft, title: event.target.value })}
        />
        {inlineErrors.title && <div className="invalid-feedback d-block">{inlineErrors.title}</div>}
      </Form.Group>
      <Form.Group className="mb-2">
        <Form.Control
          size="sm"
          as="textarea"
          rows={3}
          placeholder={t('notes.inline.content.placeholder')}
          value={draft.content}
          isInvalid={!!inlineErrors.content}
          onChange={(event) => onDraftChange({ ...draft, content: event.target.value })}
        />
        {inlineErrors.content && <div className="invalid-feedback d-block">{inlineErrors.content}</div>}
      </Form.Group>
      <Form.Group className="mb-2">
        <Form.Label className="small mb-1">{t('notes.inline.color.label')}</Form.Label>
        <Form.Control
          type="color"
          value={draft.color || DEFAULT_COLOR}
          onChange={(event) => onDraftChange({ ...draft, color: event.target.value })}
        />
      </Form.Group>
      <TagInput
        id={`inline-tags-${noteId}`}
        label={t('notes.inline.tags.label')}
        tags={draft.tags}
        onChange={(tags) => onDraftChange({ ...draft, tags })}
        loadSuggestions={loadTagSuggestions}
        maxTags={5}
        errorMessage={t('notes.validation.tags.format')}
      />
      {inlineErrors.tags && <div className="invalid-feedback d-block">{inlineErrors.tags}</div>}
      <Form.Check
        type="switch"
        id={`inlinePinned-${noteId}`}
        label={t('notes.inline.pinned.label')}
        checked={draft.pinned}
        onChange={(event) => onDraftChange({ ...draft, pinned: event.target.checked })}
        className="mb-3"
      />
      <div className="d-flex justify-content-end gap-2">
        <Button variant="outline-secondary" size="sm" onClick={onCancel}>
          <FontAwesomeIcon icon={faXmark} className="me-1" /> {t('common.cancel')}
        </Button>
        <Button variant="primary" size="sm" onClick={onSave} disabled={inlineSaving}>
          {inlineSaving ? <Spinner size="sm" className="me-1" /> : <FontAwesomeIcon icon={faCheck} className="me-1" />}
          {t('common.save')}
        </Button>
      </div>
    </div>
  );
}

function NoteMetaInfo({
  note,
  showOwner,
  view,
  meta,
  t,
}: Readonly<{
  note: NoteDTO;
  showOwner: boolean;
  view: NoteView;
  meta: { created: DateTimeText; modified: DateTimeText; deleted: DateTimeText | null };
  t: TranslateFn;
}>) {
  return (
    <>
      <div className="d-flex flex-column gap-1 text-muted small">
        {showOwner && (
          <span>
            <FontAwesomeIcon icon={faUserShield} className="me-1" />
            {t('notes.fields.owner')} {note.owner || '—'}
          </span>
        )}
        <span>
          <FontAwesomeIcon icon={faUser} className="me-1" />
          {t('notes.fields.createdBy')} {note.createdBy || '—'}
        </span>
        {note.lastModifiedBy && (
          <span>
            <FontAwesomeIcon icon={faUser} className="me-1" />
            {t('notes.fields.updatedBy')} {note.lastModifiedBy}
          </span>
        )}
      </div>
      <div className="d-flex flex-column text-muted small gap-1">
        <div className="d-flex align-items-center gap-2 flex-wrap">
          <FontAwesomeIcon icon={faCalendar} className="me-1" />
          <span>{t('notes.fields.createdAt')}</span>
          <span className="text-nowrap">{meta.created.date}</span>
          <span className="d-inline-flex align-items-center gap-1 text-nowrap">
            <FontAwesomeIcon icon={faClock} />
            {meta.created.time}
          </span>
        </div>
        <div className="d-flex align-items-center gap-2 flex-wrap">
          <FontAwesomeIcon icon={faCalendar} className="me-1" />
          <span>{t('notes.fields.updatedAt')}</span>
          <span className="text-nowrap">{meta.modified.date}</span>
          <span className="d-inline-flex align-items-center gap-1 text-nowrap">
            <FontAwesomeIcon icon={faClock} />
            {meta.modified.time}
          </span>
        </div>
      </div>
      {view === 'trash' && (
        <div className="d-flex gap-2 text-muted small">
          <span>
            <FontAwesomeIcon icon={faUser} className="me-1" />
            {t('notes.fields.deletedBy')} {note.deletedBy || '—'}
          </span>
        </div>
      )}
      {view === 'trash' && meta.deleted && (
        <div className="d-flex text-muted small align-items-center gap-2 flex-wrap mt-1">
          <FontAwesomeIcon icon={faCalendar} className="me-1" />
          <span>{t('notes.fields.deletedAt')}</span>
          <span className="text-nowrap">{meta.deleted.date}</span>
          <span className="d-inline-flex align-items-center gap-1 text-nowrap">
            <FontAwesomeIcon icon={faClock} />
            {meta.deleted.time}
          </span>
        </div>
      )}
    </>
  );
}

function NoteCard({ note, view, showOwner, selected, onSelect, onDeselect, onAction, loadTagSuggestions, onInlineSave }: NoteCardProps) {
  const { t } = useTranslation();
  const tags = useMemo(() => normalizeTags(note.tags), [note.tags]);
  const [inlineMode, setInlineMode] = useState(false);
  const [draft, setDraft] = useState<NoteDraft>({
    title: note.title || '',
    content: note.content || '',
    color: note.color || DEFAULT_COLOR,
    pinned: !!note.pinned,
    tags,
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
      tags,
    });
    setInlineErrors({});
  }, [inlineMode, note, tags]);

  const meta = useMemo(() => {
    const createdText = formatDate(note.createdDate);
    const modifiedText = note.lastModifiedDate ? formatDate(note.lastModifiedDate) : createdText;
    const deletedText = note.deletedDate ? formatDate(note.deletedDate) : '';
    return {
      created: splitDateTimeText(createdText),
      modified: splitDateTimeText(modifiedText),
      deleted: deletedText ? splitDateTimeText(deletedText) : null,
    };
  }, [note]);

  const handleInlineSave = async () => {
    const errors = validateNotePayload(t, draft);
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
              onChange={(event) => (event.target.checked ? onSelect(note.id) : onDeselect(note.id))}
            />
            <div className="flex-grow-1">
              <NoteSummary note={note} tags={tags} />
            </div>
            <NoteActions
              view={view}
              note={note}
              showOwner={showOwner}
              onAction={onAction}
              onInlineToggle={() => setInlineMode((prev) => !prev)}
              t={t}
            />
          </div>

          {inlineMode && (
            <NoteInlineEditor
              noteId={note.id}
              draft={draft}
              inlineErrors={inlineErrors}
              inlineSaving={inlineSaving}
              onDraftChange={setDraft}
              loadTagSuggestions={loadTagSuggestions}
              onCancel={() => setInlineMode(false)}
              onSave={handleInlineSave}
              t={t}
            />
          )}

          <NoteMetaInfo note={note} showOwner={showOwner} view={view} meta={meta} t={t} />
        </Card.Body>
      </Card>
    </Col>
  );
}

function NotesPageHero({ t }: Readonly<{ t: TranslateFn }>) {
  return (
    <header className="py-4 mb-4 shadow-sm bg-body-tertiary">
      <Container className="d-flex flex-column flex-md-row align-items-md-center justify-content-between">
        <div>
          <h1 className="h4 mb-1 text-body">{t('notes.title')}</h1>
          <p className="mb-0 text-muted">{t('notes.subtitle')}</p>
        </div>
        <div className="mt-3 mt-md-0 d-flex gap-2 flex-wrap">
          <Badge bg="primary-subtle" text="primary">
            {t('notes.badges.jpaAuditing')}
          </Badge>
          <Badge bg="primary-subtle" text="primary">
            {t('notes.badges.liquibase')}
          </Badge>
          <Badge bg="primary-subtle" text="primary">
            {t('notes.badges.springdoc')}
          </Badge>
        </div>
      </Container>
    </header>
  );
}

function NotesViewTabs({
  view,
  onViewChange,
  pageInfo,
  showPageInfo,
  t,
}: Readonly<{
  view: NoteView;
  onViewChange: (next: NoteView) => void;
  pageInfo: string;
  showPageInfo: boolean;
  t: TranslateFn;
}>) {
  return (
    <div className="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
      <div className="nav nav-tabs nav-tabs-sm shadow-sm">
        <button
          className={`nav-link d-inline-flex align-items-center gap-2 ${view === 'active' ? 'active' : ''}`}
          onClick={() => onViewChange('active')}
          type="button"
        >
          <FontAwesomeIcon icon={faLayerGroup} />
          <span>{t('notes.tabs.active')}</span>
        </button>
        <button
          className={`nav-link d-inline-flex align-items-center gap-2 ${view === 'trash' ? 'active' : ''}`}
          onClick={() => onViewChange('trash')}
          type="button"
        >
          <FontAwesomeIcon icon={faTrashCan} />
          <span>{t('notes.tabs.trash')}</span>
        </button>
      </div>
      {showPageInfo && <div className="text-muted small">{pageInfo}</div>}
    </div>
  );
}

function NotesFiltersCard({
  filterTags,
  setFilterTags,
  filterColor,
  setFilterColor,
  filterPinned,
  setFilterPinned,
  loadTagSuggestions,
  onReset,
  onApply,
  t,
}: Readonly<{
  filterTags: string[];
  setFilterTags: (tags: string[]) => void;
  filterColor: string;
  setFilterColor: (color: string) => void;
  filterPinned: string;
  setFilterPinned: (value: string) => void;
  loadTagSuggestions: (query: string) => Promise<string[]>;
  onReset: () => void;
  onApply: () => void;
  t: TranslateFn;
}>) {
  return (
    <Card className="shadow-sm border-0 mb-3">
      <Card.Body>
        <div className="d-flex flex-wrap align-items-start gap-3">
          <div className="d-flex flex-column gap-2 flex-grow-1" style={{ minWidth: 320 }}>
            <Form.Label className="small text-muted mb-0">{t('notes.filters.tags')}</Form.Label>
            <TagInput
              id="filter-tags"
              tags={filterTags}
              onChange={setFilterTags}
              loadSuggestions={loadTagSuggestions}
              maxTags={5}
              errorMessage={t('notes.validation.tags.format')}
              className="mb-0"
            />
          </div>
          <div className="d-flex flex-column gap-2 flex-shrink-0" style={{ minWidth: 180 }}>
            <Form.Label className="small text-muted mb-0">{t('notes.filters.color')}</Form.Label>
            <div className="d-flex align-items-center gap-2">
              <Form.Control
                type="color"
                value={filterColor || DEFAULT_COLOR}
                onChange={(event) => setFilterColor(event.target.value)}
                style={{ width: 48, height: 38 }}
              />
              <Button variant="outline-secondary" size="sm" onClick={() => setFilterColor('')}>
                <FontAwesomeIcon icon={faXmark} className="me-1" /> {t('common.clear')}
              </Button>
            </div>
          </div>
          <div className="d-flex flex-column gap-2 flex-shrink-0" style={{ minWidth: 160 }}>
            <Form.Label className="small text-muted mb-0">{t('notes.filters.pinned.label')}</Form.Label>
            <Form.Select size="sm" value={filterPinned} onChange={(event) => setFilterPinned(event.target.value)}>
              <option value="">{t('notes.filters.pinned.options.all')}</option>
              <option value="true">{t('notes.filters.pinned.options.pinned')}</option>
              <option value="false">{t('notes.filters.pinned.options.unpinned')}</option>
            </Form.Select>
          </div>
        </div>
        <div className="d-flex gap-2 flex-wrap mt-3">
          <Button variant="outline-secondary" size="sm" onClick={onReset}>
            <FontAwesomeIcon icon={faRotateLeft} className="me-1" /> {t('common.reset')}
          </Button>
          <Button variant="primary" size="sm" onClick={onApply}>
            <FontAwesomeIcon icon={faFilter} className="me-1" /> {t('common.apply')}
          </Button>
        </div>
      </Card.Body>
    </Card>
  );
}

function NotesListControls({
  pageSize,
  onPageSizeChange,
  sort,
  onSortChange,
  showEmptyTrash,
  onEmptyTrash,
  t,
}: Readonly<{
  pageSize: number;
  onPageSizeChange: (next: number) => void;
  sort: string;
  onSortChange: (next: string) => void;
  showEmptyTrash: boolean;
  onEmptyTrash: () => void;
  t: TranslateFn;
}>) {
  return (
    <div className="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
      <div className="d-flex flex-wrap align-items-center gap-3">
        <div className="d-flex align-items-center gap-2">
          <Form.Label className="text-muted small mb-0 text-nowrap">{t('pagination.pageSize.label')}</Form.Label>
          <Form.Select
            size="sm"
            value={pageSize}
            onChange={(event) => onPageSizeChange(Number(event.target.value))}
            style={{ width: 'auto' }}
          >
            <option value={5}>5</option>
            <option value={10}>10</option>
            <option value={25}>25</option>
          </Form.Select>
        </div>
        <div className="d-flex align-items-center gap-2">
          <Form.Label className="text-muted small mb-0 text-nowrap">{t('pagination.sort.label')}</Form.Label>
          <Form.Select size="sm" value={sort} onChange={(event) => onSortChange(event.target.value)} style={{ width: 'auto' }}>
            <option value="createdDate,desc">{t('notes.sort.options.createdNewest')}</option>
            <option value="createdDate,asc">{t('notes.sort.options.createdOldest')}</option>
            <option value="lastModifiedDate,desc">{t('notes.sort.options.updatedNewest')}</option>
            <option value="lastModifiedDate,asc">{t('notes.sort.options.updatedOldest')}</option>
            <option value="title,asc">{t('notes.sort.options.titleAsc')}</option>
            <option value="title,desc">{t('notes.sort.options.titleDesc')}</option>
          </Form.Select>
        </div>
      </div>
      {showEmptyTrash && (
        <Button variant="outline-danger" size="sm" onClick={onEmptyTrash}>
          <FontAwesomeIcon icon={faTrash} className="me-1" /> {t('notes.actions.emptyTrash')}
        </Button>
      )}
    </div>
  );
}

function NotesSelectionBar({
  allSelected,
  onSelectAll,
  onClearSelection,
  selectedCount,
  view,
  openBulkModal,
  t,
}: Readonly<{
  allSelected: boolean;
  onSelectAll: () => void;
  onClearSelection: () => void;
  selectedCount: number;
  view: NoteView;
  openBulkModal: (action: string) => void;
  t: TranslateFn;
}>) {
  const hasSelected = selectedCount > 0;

  return (
    <div className="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
      <Form.Check
        type="checkbox"
        checked={allSelected}
        label={t('notes.selection.selectAllOnPage')}
        onChange={(event) => (event.target.checked ? onSelectAll() : onClearSelection())}
      />
      <div className="d-flex flex-wrap gap-2 align-items-center">
        {hasSelected && <span className="text-muted small">{t('notes.selection.selected', { count: selectedCount })}</span>}
        {view === 'trash' ? (
          <>
            <Button variant="outline-success" size="sm" disabled={!hasSelected} onClick={() => openBulkModal('RESTORE')}>
              <FontAwesomeIcon icon={faRotateLeft} className="me-1" /> {t('notes.actions.restore')}
            </Button>
            <Button variant="outline-danger" size="sm" disabled={!hasSelected} onClick={() => openBulkModal('DELETE_FOREVER')}>
              <FontAwesomeIcon icon={faTrash} className="me-1" /> {t('notes.actions.deletePermanently')}
            </Button>
          </>
        ) : (
          <Button variant="outline-danger" size="sm" disabled={!hasSelected} onClick={() => openBulkModal('DELETE_SOFT')}>
            <FontAwesomeIcon icon={faTrash} className="me-1" /> {t('notes.actions.delete')}
          </Button>
        )}
      </div>
    </div>
  );
}

function NotesListStatus({
  alert,
  loading,
  showEmptyMessage,
  emptyMessage,
  emptyIcon,
}: Readonly<{
  alert: string;
  loading: boolean;
  showEmptyMessage: boolean;
  emptyMessage: string;
  emptyIcon: IconProp;
}>) {
  return (
    <>
      {alert && <Alert variant="danger">{alert}</Alert>}
      {loading && (
        <div className="text-center py-3">
          <Spinner animation="border" variant="primary" />
        </div>
      )}
      {showEmptyMessage && (
        <ListGroup>
          <ListGroup.Item className="text-muted small d-flex align-items-center gap-2">
            <FontAwesomeIcon icon={emptyIcon} />
            <span>{emptyMessage}</span>
          </ListGroup.Item>
        </ListGroup>
      )}
    </>
  );
}

function NotesGrid({
  notes,
  view,
  showOwner,
  selected,
  onSelect,
  onDeselect,
  onAction,
  loadTagSuggestions,
  onInlineSave,
}: Readonly<{
  notes: NoteDTO[];
  view: NoteView;
  showOwner: boolean;
  selected: Set<number>;
  onSelect: (noteId: number) => void;
  onDeselect: (noteId: number) => void;
  onAction: (action: NoteAction, note: NoteDTO) => void | Promise<void>;
  loadTagSuggestions: (query: string) => Promise<string[]>;
  onInlineSave: (
    noteId: number,
    payload: { title: string; content: string; color: string; pinned: boolean; tags: string[] },
  ) => Promise<NoteDTO | null>;
}>) {
  return (
    <Row className="g-3">
      {notes.map((note) => (
        <NoteCard
          key={note.id}
          note={note}
          view={view}
          showOwner={showOwner}
          selected={selected.has(note.id)}
          onSelect={onSelect}
          onDeselect={onDeselect}
          onAction={onAction}
          loadTagSuggestions={loadTagSuggestions}
          onInlineSave={onInlineSave}
        />
      ))}
    </Row>
  );
}

function NotesPagination({
  show,
  items,
}: Readonly<{
  show: boolean;
  items: JSX.Element[];
}>) {
  if (!show) return null;
  return (
    <div className="d-flex justify-content-center mt-3">
      <Pagination>{items}</Pagination>
    </div>
  );
}

function ConfirmModal({
  show,
  title,
  message,
  confirmLabel,
  onCancel,
  onConfirm,
  t,
}: Readonly<{
  show: boolean;
  title: string;
  message: string;
  confirmLabel: string;
  onCancel: () => void;
  onConfirm: () => void;
  t: TranslateFn;
}>) {
  return (
    <Modal show={show} onHide={onCancel} centered>
      <Modal.Header closeButton>
        <Modal.Title>{title}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <p>{message}</p>
        <div className="d-flex justify-content-end gap-2">
          <Button variant="outline-secondary" onClick={onCancel}>
            <FontAwesomeIcon icon={faXmark} className="me-1" /> {t('common.cancel')}
          </Button>
          <Button variant="danger" onClick={onConfirm}>
            <FontAwesomeIcon icon={faTrash} className="me-1" /> {confirmLabel}
          </Button>
        </div>
      </Modal.Body>
    </Modal>
  );
}

function BulkConfirmModal({
  show,
  onCancel,
  onConfirm,
  t,
}: Readonly<{
  show: boolean;
  onCancel: () => void;
  onConfirm: () => void;
  t: TranslateFn;
}>) {
  return (
    <Modal show={show} onHide={onCancel} centered>
      <Modal.Header closeButton>
        <Modal.Title>{t('notes.bulk.title')}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <p>{t('notes.bulk.message')}</p>
        <div className="d-flex justify-content-end gap-2">
          <Button variant="outline-secondary" onClick={onCancel}>
            <FontAwesomeIcon icon={faXmark} className="me-1" /> {t('common.cancel')}
          </Button>
          <Button variant="danger" onClick={onConfirm}>
            <FontAwesomeIcon icon={faCheck} className="me-1" /> {t('common.confirm')}
          </Button>
        </div>
      </Modal.Body>
    </Modal>
  );
}

function NoteEditModal({
  show,
  onHide,
  editingNote,
  saveNote,
  registerNote,
  control,
  noteFormErrors,
  noteSubmitting,
  loadTagSuggestions,
  t,
}: Readonly<{
  show: boolean;
  onHide: () => void;
  editingNote: NoteDTO | null;
  saveNote: (event: FormEvent<HTMLFormElement>) => void;
  registerNote: UseFormRegister<NoteFormValues>;
  control: Control<NoteFormValues>;
  noteFormErrors: FieldErrors<NoteFormValues>;
  noteSubmitting: boolean;
  loadTagSuggestions: (query: string) => Promise<string[]>;
  t: TranslateFn;
}>) {
  const titleMessage = noteFormErrors.title?.message;
  const contentMessage = noteFormErrors.content?.message;
  return (
    <Modal show={show} onHide={onHide} centered>
      <Modal.Header closeButton>
        <Modal.Title>{editingNote ? t('notes.modal.editTitle') : t('notes.modal.newTitle')}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Form onSubmit={saveNote} noValidate>
          <Form.Group className="mb-3">
            <Form.Label>{t('notes.form.title.label')}</Form.Label>
            <Form.Control
              type="text"
              placeholder={t('notes.form.title.placeholder')}
              isInvalid={!!noteFormErrors.title}
              {...registerNote('title', {
                validate: (value) => {
                  const trimmed = value.trim();
                  if (!trimmed) return t('validation.required');
                  if (trimmed.length < 3 || trimmed.length > 255) {
                    return t('notes.validation.title.size');
                  }
                  return true;
                },
              })}
            />
            {noteFormErrors.title ? <Form.Control.Feedback type="invalid">{String(titleMessage ?? '')}</Form.Control.Feedback> : null}
          </Form.Group>
          <Form.Group className="mb-3">
            <Form.Label>{t('notes.form.content.label')}</Form.Label>
            <Form.Control
              as="textarea"
              rows={4}
              placeholder={t('notes.form.content.placeholder')}
              isInvalid={!!noteFormErrors.content}
              {...registerNote('content', {
                validate: (value) => {
                  const trimmed = value.trim();
                  if (!trimmed) return t('validation.required');
                  if (trimmed.length < 10 || trimmed.length > 1024) {
                    return t('notes.validation.content.size');
                  }
                  return true;
                },
              })}
            />
            {noteFormErrors.content ? <Form.Control.Feedback type="invalid">{String(contentMessage ?? '')}</Form.Control.Feedback> : null}
          </Form.Group>
          <Form.Group className="mb-3">
            <Form.Label>{t('notes.form.color.label')}</Form.Label>
            <Form.Control type="color" {...registerNote('color')} />
          </Form.Group>
          <Controller
            name="tags"
            control={control}
            rules={{
              validate: (value) => {
                const normalized = normalizeTags(value);
                if (normalized.length > 5) return t('notes.validation.tags.max');
                if (normalized.some((tag) => tag.length < 1 || tag.length > 30)) return t('notes.validation.tags.size');
                if (normalized.some((tag) => !TAG_PATTERN.test(tag))) return t('notes.validation.tags.format');
                return true;
              },
            }}
            render={({ field }) => (
              <TagInput
                id="note-tags"
                label={t('notes.form.tags.label')}
                tags={field.value}
                onChange={field.onChange}
                loadSuggestions={loadTagSuggestions}
                maxTags={5}
                errorMessage={t('notes.validation.tags.format')}
                isInvalid={!!noteFormErrors.tags}
                externalError={noteFormErrors.tags?.message}
              />
            )}
          />
          <Form.Check type="switch" id="note-pinned" label={t('notes.form.pinned.label')} className="mb-3" {...registerNote('pinned')} />
          <div className="d-flex justify-content-end">
            <Button type="submit" disabled={noteSubmitting}>
              {noteSubmitting ? <Spinner size="sm" className="me-2" /> : <FontAwesomeIcon icon={faCheck} className="me-2" />}
              {editingNote ? t('common.save') : t('common.create')}
            </Button>
          </div>
        </Form>
      </Modal.Body>
    </Modal>
  );
}

function ShareLinkItem({
  link,
  onCopy,
  onRevoke,
  t,
}: Readonly<{
  link: ShareLinkDTO;
  onCopy: (token: string | undefined) => void;
  onRevoke: (id: string | number) => void;
  t: TranslateFn;
}>) {
  const token = link.token || '';
  const label = token ? `${token.slice(0, 6)}…${token.slice(-4)}` : `#${link.id}`;
  let status = t('notes.share.status.active');
  if (link.revoked) {
    status = t('notes.share.status.revoked');
  } else if (link.expired) {
    status = t('notes.share.status.expired');
  }
  const expiresAtText = link.expiresAt ? formatDate(link.expiresAt) : t('notes.share.link.noExpiry');
  const oneTimeSuffix = link.oneTime ? ` · ${t('notes.share.oneTime')}` : '';
  return (
    <ListGroup.Item className="d-flex justify-content-between align-items-center flex-wrap gap-2">
      <div>
        <div className="fw-semibold text-primary">{label}</div>
        <div className="text-muted small">
          {status}
          {' · '}
          {expiresAtText}
          {oneTimeSuffix}
        </div>
      </div>
      <div className="d-flex gap-2">
        <Button variant="outline-secondary" size="sm" onClick={() => onCopy(token || undefined)} disabled={link.revoked}>
          <FontAwesomeIcon icon={faCopy} className="me-1" /> {t('common.copy')}
        </Button>
        <Button variant="outline-danger" size="sm" onClick={() => onRevoke(link.id)} disabled={link.revoked}>
          <FontAwesomeIcon icon={faTrash} className="me-1" /> {t('common.revoke')}
        </Button>
      </div>
    </ListGroup.Item>
  );
}

function ShareResultBlock({
  shareResult,
  onCopyUrl,
  t,
}: Readonly<{
  shareResult: ShareResult;
  onCopyUrl: (url: string) => void;
  t: TranslateFn;
}>) {
  return (
    <div className="mt-3">
      <Form.Group className="mb-2">
        <Form.Label>{t('notes.share.link.label')}</Form.Label>
        <InputGroup>
          <Form.Control value={shareResult.url} readOnly />
          <Button variant="outline-secondary" onClick={() => onCopyUrl(shareResult.url)}>
            <FontAwesomeIcon icon={faCopy} className="me-1" /> {t('common.copy')}
          </Button>
        </InputGroup>
      </Form.Group>
      <div className="d-flex align-items-center gap-2">
        <Badge bg="success-subtle" text="success">
          {shareResult.permission}
        </Badge>
        <span className="text-muted small">
          {shareResult.expiresAt
            ? t('notes.share.link.expiresAt', { date: formatDate(shareResult.expiresAt) })
            : t('notes.share.link.noExpiry')}
        </span>
        {shareResult.oneTime && (
          <Badge bg="secondary-subtle" text="secondary">
            {t('notes.share.oneTime')}
          </Badge>
        )}
      </div>
    </div>
  );
}

function ShareModal({
  show,
  onHide,
  shareAlert,
  shareNoteTitle,
  shareExpiry,
  setShareExpiry,
  shareExpiresAt,
  setShareExpiresAt,
  shareOneTime,
  setShareOneTime,
  shareSubmitting,
  onSubmit,
  shareResult,
  onCopyUrl,
  shareLinks,
  shareLinksLoading,
  shareLinksHasMore,
  onCopyToken,
  onRevokeLink,
  onLoadMore,
  t,
}: Readonly<{
  show: boolean;
  onHide: () => void;
  shareAlert: string;
  shareNoteTitle: string;
  shareExpiry: string;
  setShareExpiry: (next: string) => void;
  shareExpiresAt: string;
  setShareExpiresAt: (next: string) => void;
  shareOneTime: boolean;
  setShareOneTime: (next: boolean) => void;
  shareSubmitting: boolean;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  shareResult: ShareResult | null;
  onCopyUrl: (url: string) => void;
  shareLinks: ShareLinkDTO[];
  shareLinksLoading: boolean;
  shareLinksHasMore: boolean;
  onCopyToken: (token: string | undefined) => void;
  onRevokeLink: (id: string | number) => void;
  onLoadMore: () => void;
  t: TranslateFn;
}>) {
  const showCustomExpiry = shareExpiry === 'custom';
  const showEmptyLinksMessage = shareLinks.length === 0 && !shareLinksLoading;

  return (
    <Modal show={show} onHide={onHide} centered size="lg">
      <Modal.Header closeButton>
        <Modal.Title className="d-flex align-items-center gap-2">
          <FontAwesomeIcon icon={faLink} className="text-primary" />
          <span>{t('notes.share.title')}</span>
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {shareAlert && <Alert variant="danger">{shareAlert}</Alert>}
        <p className="text-muted small mb-3">{shareNoteTitle}</p>
        <Form onSubmit={onSubmit}>
          <input type="hidden" value="READ" />
          <Form.Group className="mb-3">
            <Form.Label>{t('notes.share.expires.label')}</Form.Label>
            <Form.Select value={shareExpiry} onChange={(event) => setShareExpiry(event.target.value)}>
              <option value="1">{t('notes.share.expires.options.1h')}</option>
              <option value="24">{t('notes.share.expires.options.24h')}</option>
              <option value="72">{t('notes.share.expires.options.3d')}</option>
              <option value="168">{t('notes.share.expires.options.7d')}</option>
              <option value="720">{t('notes.share.expires.options.30d')}</option>
              <option value="custom">{t('notes.share.expires.options.custom')}</option>
              <option value="never">{t('notes.share.expires.options.never')}</option>
            </Form.Select>
            {showCustomExpiry && (
              <Form.Control
                type="datetime-local"
                className="mt-2"
                value={shareExpiresAt}
                onChange={(event) => setShareExpiresAt(event.target.value)}
              />
            )}
            <div className="form-text">{t('notes.share.expires.helper')}</div>
          </Form.Group>
          <Form.Check
            type="switch"
            label={t('notes.share.oneTime')}
            checked={shareOneTime}
            onChange={(event) => setShareOneTime(event.target.checked)}
            className="mb-3"
          />
          <div className="d-flex justify-content-end gap-2">
            <Button variant="outline-secondary" onClick={onHide}>
              {t('common.cancel')}
            </Button>
            <Button type="submit" disabled={shareSubmitting}>
              {shareSubmitting && <Spinner size="sm" className="me-2" />}
              {t('notes.share.createLink')}
            </Button>
          </div>
        </Form>
        {shareResult && <ShareResultBlock shareResult={shareResult} onCopyUrl={onCopyUrl} t={t} />}
        <div className="mt-4">
          <div className="d-flex align-items-center gap-2 mb-2">
            <span className="spinner-border spinner-border-sm text-secondary d-none" aria-hidden="true" />
          </div>
          {showEmptyLinksMessage && <div className="text-muted small">{t('notes.share.links.empty')}</div>}
          <ListGroup style={{ maxHeight: '36rem', overflowY: 'auto' }}>
            {shareLinks.map((link) => (
              <ShareLinkItem key={link.id} link={link} onCopy={onCopyToken} onRevoke={onRevokeLink} t={t} />
            ))}
          </ListGroup>
          <div className="d-flex justify-content-center mt-2">
            {shareLinksHasMore && (
              <Button variant="outline-secondary" size="sm" onClick={onLoadMore} disabled={shareLinksLoading}>
                {shareLinksLoading && <Spinner size="sm" className="me-1" />}
                {t('common.loadMore')}
              </Button>
            )}
          </div>
        </div>
      </Modal.Body>
    </Modal>
  );
}

function RevisionListItem({
  rev,
  localNumber,
  previous,
  showDiff,
  onToggleDiff,
  onRestore,
  t,
}: Readonly<{
  rev: NoteRevisionDTO;
  localNumber: number;
  previous: NoteRevisionDTO | undefined;
  showDiff: boolean;
  onToggleDiff: (revisionId: number) => void;
  onRestore: (revisionId: number) => void;
  t: TranslateFn;
}>) {
  const noteData = rev.note;
  const additionsOnly = !previous;
  const titleDiff = renderDiffSpans(diffLinesDetailed(previous?.note?.title ?? '', noteData?.title ?? ''), additionsOnly);
  const contentDiff = renderDiffSpans(diffLinesDetailed(previous?.note?.content ?? '', noteData?.content ?? ''), additionsOnly);
  const tags = normalizeTags(noteData?.tags);

  return (
    <ListGroup.Item>
      <div className="d-flex flex-column flex-md-row justify-content-between gap-3">
        <div className="flex-grow-1">
          <div className="d-flex align-items-center gap-2 flex-wrap mb-1">
            <Badge bg="primary-subtle" text="primary">
              v{localNumber}
            </Badge>
            <Badge bg="secondary-subtle" text="secondary" className="text-uppercase">
              {rev.revisionType || t('common.na')}
            </Badge>
            <span className="text-muted small">
              <FontAwesomeIcon icon={faClock} className="me-1" />
              {formatDate(rev.revisionDate) || '—'}
            </span>
            <span className="text-muted small">
              <FontAwesomeIcon icon={faUser} className="me-1" />
              {rev.auditor || t('common.unknown')}
            </span>
            {noteData?.color && (
              <Badge bg="body-secondary" text="body" className="border" style={{ borderColor: noteData.color }}>
                <FontAwesomeIcon icon={faCircle} style={{ color: noteData.color }} />
              </Badge>
            )}
            <Badge bg="warning-subtle" text="warning">
              {noteData?.pinned ? t('notes.pinned') : t('notes.unpinned')}
            </Badge>
          </div>
          <div className="fw-semibold">{noteData?.title || t('notes.noTitle')}</div>
          <div className="text-muted small">{noteData?.content || ''}</div>
          <NoteTagBadges tags={tags} />
        </div>
        <div className="d-flex flex-column align-items-end gap-2">
          <Button variant="outline-secondary" size="sm" onClick={() => onToggleDiff(rev.revision)}>
            <FontAwesomeIcon icon={faBars} className="me-1" /> {showDiff ? t('notes.revisions.hideDiff') : t('notes.revisions.diff')}
          </Button>
          <Button variant="outline-primary" size="sm" onClick={() => onRestore(rev.revision)}>
            <FontAwesomeIcon icon={faRotateLeft} className="me-1" /> {t('notes.actions.restore')}
          </Button>
        </div>
      </div>
      {showDiff && (
        <div className="mt-3 border rounded p-3 bg-body-secondary">
          <div className="fw-semibold mb-2">{t('notes.diff.title')}</div>
          <div className="d-flex flex-wrap gap-2">{titleDiff}</div>
          <div className="fw-semibold mb-2 mt-3">{t('notes.diff.content')}</div>
          <div className="d-flex flex-column gap-2">{contentDiff}</div>
        </div>
      )}
    </ListGroup.Item>
  );
}

function RevisionModal({
  show,
  onHide,
  revisionError,
  revisionLoading,
  revisions,
  revisionTotal,
  diffOpen,
  onToggleDiff,
  onRestoreRevision,
  hasMore,
  onLoadMore,
  t,
}: Readonly<{
  show: boolean;
  onHide: () => void;
  revisionError: string;
  revisionLoading: boolean;
  revisions: NoteRevisionDTO[];
  revisionTotal: number;
  diffOpen: Set<number>;
  onToggleDiff: (revisionId: number) => void;
  onRestoreRevision: (revisionId: number) => void;
  hasMore: boolean;
  onLoadMore: () => void;
  t: TranslateFn;
}>) {
  const showInitialLoading = revisionLoading && revisions.length === 0;
  const showEmpty = revisions.length === 0 && !revisionLoading;

  return (
    <Modal show={show} onHide={onHide} centered size="lg">
      <Modal.Header closeButton>
        <Modal.Title className="d-flex align-items-center gap-2">
          <FontAwesomeIcon icon={faClockRotateLeft} className="text-primary" />
          <span>{t('notes.revisions.title')}</span>
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {revisionError && <Alert variant="danger">{revisionError}</Alert>}
        {showInitialLoading && (
          <div className="d-flex justify-content-center py-3">
            <Spinner animation="border" variant="primary" />
          </div>
        )}
        <ListGroup>
          {showEmpty && <ListGroup.Item className="text-muted">{t('notes.revisions.empty')}</ListGroup.Item>}
          {revisions.map((rev, idx) => {
            const localNumber = revisionTotal ? revisionTotal - idx : idx + 1;
            const previous = revisions[idx + 1];
            const showDiff = diffOpen.has(rev.revision);
            return (
              <RevisionListItem
                key={rev.revision}
                rev={rev}
                localNumber={localNumber}
                previous={previous}
                showDiff={showDiff}
                onToggleDiff={onToggleDiff}
                onRestore={onRestoreRevision}
                t={t}
              />
            );
          })}
        </ListGroup>
        {hasMore && (
          <div className="d-flex justify-content-center mt-3">
            <Button variant="outline-secondary" size="sm" onClick={onLoadMore} disabled={revisionLoading}>
              {revisionLoading && <Spinner size="sm" className="me-1" />}
              {t('common.loadMore')}
            </Button>
          </div>
        )}
      </Modal.Body>
    </Modal>
  );
}

function OwnerModal({
  show,
  onHide,
  ownerQuery,
  onQueryChange,
  suggestions,
  onSelectSuggestion,
  hasMore,
  onLoadMore,
  loading,
  onSubmit,
  t,
}: Readonly<{
  show: boolean;
  onHide: () => void;
  ownerQuery: string;
  onQueryChange: (next: string) => void;
  suggestions: StoredUser[];
  onSelectSuggestion: (login: string) => void;
  hasMore: boolean;
  onLoadMore: () => void;
  loading: boolean;
  onSubmit: () => void;
  t: TranslateFn;
}>) {
  const suggestionLogins = useMemo(
    () =>
      suggestions
        .map((user) => user.login ?? user.username)
        .filter((login): login is string => typeof login === 'string' && login.length > 0),
    [suggestions],
  );

  return (
    <Modal show={show} onHide={onHide} centered>
      <Modal.Header closeButton>
        <Modal.Title className="d-flex align-items-center gap-2">
          <FontAwesomeIcon icon={faUserGear} className="text-primary" />
          <span>{t('notes.owner.title')}</span>
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Form>
          <Form.Label>{t('notes.owner.form.newOwner')}</Form.Label>
          <InputGroup className="mb-2">
            <Form.Control
              value={ownerQuery}
              onChange={(event) => onQueryChange(event.target.value)}
              placeholder={t('notes.owner.form.searchPlaceholder')}
            />
          </InputGroup>
          <ListGroup className="mb-2">
            {suggestionLogins.map((login) => (
              <ListGroup.Item key={login} action onClick={() => onSelectSuggestion(login)}>
                {login}
              </ListGroup.Item>
            ))}
          </ListGroup>
          {hasMore && (
            <div className="d-grid">
              <Button variant="outline-secondary" size="sm" onClick={onLoadMore} disabled={loading}>
                {loading && <Spinner size="sm" className="me-1" />}
                {t('common.loadMore')}
              </Button>
            </div>
          )}
          <div className="form-text text-muted">{t('notes.owner.form.helper')}</div>
        </Form>
      </Modal.Body>
      <Modal.Footer>
        <Button variant="outline-secondary" onClick={onHide}>
          <FontAwesomeIcon icon={faXmark} className="me-1" /> {t('common.cancel')}
        </Button>
        <Button variant="primary" onClick={onSubmit}>
          <FontAwesomeIcon icon={faCheck} className="me-1" /> {t('notes.owner.submit')}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}

export default function NotesPage() {
  const { pushToast } = useToasts();
  const { loading: authLoading, isAdmin, isAuthenticated } = useAuth();
  const { t } = useTranslation();
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
      setSelected((prev) => {
        const next = new Set<number>();
        content.forEach((note) => {
          if (prev.has(note.id)) next.add(note.id);
        });
        return next;
      });
    } catch (err) {
      setAlert(messageFromError(err, t('notes.error.loadFailed')));
    } finally {
      setLoading(false);
    }
  }, [authLoading, isAuthenticated, view, page, pageSize, sort, debouncedSearch, appliedFilters, t]);

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

  const selectNote = (id: number) => {
    setSelected((prev) => {
      const next = new Set<number>(prev);
      next.add(id);
      return next;
    });
  };

  const deselectNote = (id: number) => {
    setSelected((prev) => {
      const next = new Set<number>(prev);
      next.delete(id);
      return next;
    });
  };

  const selectAllOnPage = () => {
    setSelected(new Set<number>(notes.map((note) => note.id)));
  };

  const clearSelection = () => {
    setSelected(new Set<number>());
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

  const saveNote = handleNoteSubmit(async (data) => {
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
        setNotes((prev) => prev.map((note) => (note.id === editingNote.id ? updated : note)));
        pushToast(t('notes.toast.updated'), 'success');
      } else {
        await Api.createNote(payload);
        pushToast(t('notes.toast.created'), 'success');
      }
      setNoteModalOpen(false);
      loadNotes();
    } catch (err) {
      pushToast(messageFromError(err, t('notes.toast.saveFailed')), 'danger');
    }
  });

  const handleInlineSave = async (
    id: number,
    payload: { title: string; content: string; color: string; pinned: boolean; tags: string[] },
  ): Promise<NoteDTO | null> => {
    try {
      const updated = await Api.updateNote(id, payload);
      setNotes((prev) => prev.map((note) => (note.id === id ? updated : note)));
      pushToast(t('notes.toast.updated'), 'success');
      return updated;
    } catch (err) {
      pushToast(messageFromError(err, t('notes.toast.saveFailed')), 'danger');
      return null;
    }
  };

  const handleNoteAction = async (action: NoteAction, note: NoteDTO) => {
    const handlers: Record<NoteAction, (note: NoteDTO) => void | Promise<void>> = {
      'edit-modal': (note) => openEditNote(note),
      delete: (note) => {
        setDeleteTarget(note);
        setDeleteModalOpen(true);
      },
      'delete-forever': (note) => {
        setDeleteForeverTarget(note);
        setDeleteForeverModalOpen(true);
      },
      restore: async (note) => {
        try {
          await Api.restore(note.id);
          pushToast(t('notes.toast.restored'), 'success');
          loadNotes();
        } catch (err) {
          pushToast(messageFromError(err, t('notes.toast.restoreFailed')), 'danger');
        }
      },
      copy: async (note) => {
        try {
          await navigator.clipboard.writeText(note.content || '');
          pushToast(t('notes.toast.copied'), 'success');
        } catch {
          pushToast(t('notes.toast.copyFailed'), 'warning');
        }
      },
      'toggle-pin': async (note) => {
        try {
          const updated = await Api.patchNote(note.id, { pinned: !note.pinned });
          setNotes((prev) => replaceNoteById(prev, note.id, updated));
        } catch (err) {
          pushToast(messageFromError(err, t('notes.toast.pinFailed')), 'danger');
        }
      },
      share: (note) => openShareModal(note),
      'share-links': (note) => openShareModal(note),
      revisions: (note) => openRevisionModal(note),
      'change-owner': (note) => {
        if (!isAdmin) return;
        setOwnerTarget(note);
        setOwnerQuery('');
        setOwnerSuggestions([]);
        setOwnerPage(0);
        setOwnerHasMore(false);
        setOwnerModalOpen(true);
      },
    };

    await handlers[action](note);
  };

  const confirmDelete = async () => {
    if (!deleteTarget) return;
    try {
      await Api.softDelete(deleteTarget.id);
      pushToast(t('notes.toast.deleted'), 'success');
      loadNotes();
    } catch (err) {
      pushToast(messageFromError(err, t('notes.toast.deleteFailed')), 'danger');
    } finally {
      setDeleteModalOpen(false);
      setDeleteTarget(null);
    }
  };

  const confirmDeleteForever = async () => {
    if (!deleteForeverTarget) return;
    try {
      await Api.deletePermanent(deleteForeverTarget.id);
      pushToast(t('notes.toast.deletedPermanent'), 'success');
      loadNotes();
    } catch (err) {
      pushToast(messageFromError(err, t('notes.toast.deleteFailed')), 'danger');
    } finally {
      setDeleteForeverModalOpen(false);
      setDeleteForeverTarget(null);
    }
  };

  const confirmEmptyTrash = async () => {
    try {
      await Api.emptyTrash();
      pushToast(t('notes.toast.trashEmptied'), 'success');
      loadNotes();
    } catch (err) {
      pushToast(messageFromError(err, t('notes.toast.emptyTrashFailed')), 'danger');
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
      pushToast(t('notes.toast.bulkCompleted'), 'success');
      setSelected(new Set<number>());
      loadNotes();
    } catch (err) {
      pushToast(messageFromError(err, t('notes.toast.bulkFailed')), 'danger');
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
      setShareLinks((prev) => (append ? [...prev, ...content] : content));
      const nextPageNumber = typeof meta.number === 'number' ? meta.number : pageToLoad;
      setShareLinksPage(nextPageNumber);
      setShareLinksHasMore(nextPageNumber < totalPagesLocal - 1);
    } catch (err) {
      setShareAlert(messageFromError(err, t('notes.share.error.loadLinksFailed')));
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
          setShareAlert(t('notes.share.error.customExpiryRequired'));
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
        setShareAlert(t('notes.share.error.tokenMissing'));
        return;
      }
      const url = buildShareUrl(token);
      setShareResult({
        url,
        permission: typeof result.permission === 'string' && result.permission ? result.permission : 'READ',
        expiresAt: (result.expiresAt ?? null) as string | null,
        oneTime: !!result.oneTime,
      });
      pushToast(t('notes.share.toast.created'), 'success');
      loadShareLinks(shareNote, false);
    } catch (err) {
      setShareAlert(messageFromError(err, t('notes.share.error.createFailed')));
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
      pushToast(t('notes.share.toast.copied'), 'success');
    } catch {
      pushToast(t('notes.share.toast.copyFailed'), 'warning');
    }
  };

  const handleShareRevoke = async (id: string | number) => {
    try {
      await Api.revokeShareLink(id);
      pushToast(t('notes.share.toast.revoked'), 'success');
      if (shareNote) {
        loadShareLinks(shareNote, false);
      }
    } catch (err) {
      pushToast(messageFromError(err, t('notes.share.toast.revokeFailed')), 'danger');
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
      setRevisions((prev) => (append ? [...prev, ...content] : content));
      const nextPageNumber = typeof meta.number === 'number' ? meta.number : pageToLoad;
      setRevisionPage(nextPageNumber);
      setRevisionHasMore(nextPageNumber < totalPagesLocal - 1);
    } catch (err) {
      setRevisionError(messageFromError(err, t('notes.revisions.error.loadFailed')));
    } finally {
      setRevisionLoading(false);
    }
  };

  const restoreRevision = async (noteId: number, revisionId: number) => {
    try {
      await Api.restoreRevision(noteId, revisionId);
      pushToast(t('notes.revisions.toast.restored'), 'success');
      setRevisionModalOpen(false);
      loadNotes();
    } catch (err) {
      pushToast(messageFromError(err, t('notes.revisions.toast.restoreFailed')), 'danger');
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
      setOwnerSuggestions((prev) => (append ? [...prev, ...content] : content));
      const nextPageNumber = typeof meta.number === 'number' ? meta.number : pageToLoad;
      setOwnerPage(nextPageNumber);
      setOwnerHasMore(nextPageNumber < totalPagesLocal - 1);
    } catch (err) {
      pushToast(messageFromError(err, t('notes.owner.searchFailed')), 'danger');
    } finally {
      setOwnerLoading(false);
    }
  };

  const submitOwnerChange = async () => {
    if (!ownerTarget) return;
    const owner = ownerQuery.trim();
    if (!owner) {
      pushToast(t('notes.owner.required'), 'warning');
      return;
    }
    try {
      await Api.changeOwner(ownerTarget.id, { owner });
      pushToast(t('notes.owner.updated'), 'success');
      setOwnerModalOpen(false);
      setOwnerTarget(null);
      loadNotes();
    } catch (err) {
      pushToast(messageFromError(err, t('notes.owner.updateFailed')), 'danger');
    }
  };

  const totalLabel =
    view === 'trash' ? t('notes.summary.totalTrash', { count: totalElements }) : t('pagination.summary.total', { count: totalElements });
  const selectedCount = selected.size;
  const allSelected = notes.length > 0 && notes.every((note) => selected.has(note.id));
  const showEmptyMessage = !loading && notes.length === 0 && !alert;
  const pageInfo = notes.length ? t('pagination.summary.pageOf', { current: totalPages ? page + 1 : 0, total: totalPages }) : '';

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

  const viewLabel = view === 'trash' ? t('notes.view.trash') : t('notes.view.all');
  const emptyIcon = view === 'trash' ? faTrashCan : faNoteSticky;
  const emptyMessage = debouncedSearch ? t('notes.empty.search') : view === 'trash' ? t('notes.empty.trash') : t('notes.empty.none');

  const toggleRevisionDiff = (revisionId: number) => {
    setDiffOpen((prev) => {
      const next = new Set<number>(prev);
      if (next.has(revisionId)) {
        next.delete(revisionId);
      } else {
        next.add(revisionId);
      }
      return next;
    });
  };

  const handleRestoreRevision = (revisionId: number) => {
    if (!revisionNote) return;
    restoreRevision(revisionNote.id, revisionId);
  };

  const loadMoreRevisions = () => {
    if (!revisionNote) return;
    loadRevisions(revisionNote, true);
  };

  const handleOwnerQueryChange = (next: string) => {
    setOwnerQuery(next);
    if (next.trim().length >= 2) {
      loadOwnerSuggestions(next.trim(), false);
      return;
    }
    setOwnerSuggestions([]);
  };

  return (
    <>
      <AppNavbar showSearch={true} search={search} onSearchChange={setSearch} onSearchClear={() => setSearch('')} />
      <NotesPageHero t={t} />
      <main className="bg-body-tertiary flex-grow-1">
        <Container className="pb-5">
          <Row className="g-4 align-items-start">
            <Col lg={3} className="order-1 order-lg-2 d-flex flex-column gap-2">
              <Button className="btn btn-primary shadow-sm px-3 py-2 w-100" onClick={openNewNote} disabled={view === 'trash'}>
                <FontAwesomeIcon icon={faPlus} className="me-1" /> {t('notes.actions.new')}
              </Button>
            </Col>
            <Col lg={9} className="order-2 order-lg-1">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h2 className="h5 mb-0 d-flex align-items-center gap-2">
                  <FontAwesomeIcon icon={faLayerGroup} className="text-primary" />
                  <span>{viewLabel}</span>
                </h2>
                {notes.length ? <span className="text-muted small">{totalLabel}</span> : null}
              </div>
              <NotesViewTabs view={view} onViewChange={setView} pageInfo={pageInfo} showPageInfo={!!notes.length} t={t} />
              <NotesFiltersCard
                filterTags={filterTags}
                setFilterTags={setFilterTags}
                filterColor={filterColor}
                setFilterColor={setFilterColor}
                filterPinned={filterPinned}
                setFilterPinned={setFilterPinned}
                loadTagSuggestions={loadTagSuggestions}
                onReset={handleResetFilters}
                onApply={handleApplyFilters}
                t={t}
              />
              <NotesListControls
                pageSize={pageSize}
                onPageSizeChange={setPageSize}
                sort={sort}
                onSortChange={setSort}
                showEmptyTrash={view === 'trash' && notes.length > 0}
                onEmptyTrash={() => setEmptyTrashModalOpen(true)}
                t={t}
              />
              {notes.length > 0 && (
                <NotesSelectionBar
                  allSelected={allSelected}
                  onSelectAll={selectAllOnPage}
                  onClearSelection={clearSelection}
                  selectedCount={selectedCount}
                  view={view}
                  openBulkModal={openBulkModal}
                  t={t}
                />
              )}
              <NotesListStatus
                alert={alert}
                loading={loading}
                showEmptyMessage={showEmptyMessage}
                emptyMessage={emptyMessage}
                emptyIcon={emptyIcon}
              />
              <NotesGrid
                notes={notes}
                view={view}
                showOwner={isAdmin}
                selected={selected}
                onSelect={selectNote}
                onDeselect={deselectNote}
                onAction={handleNoteAction}
                loadTagSuggestions={loadTagSuggestions}
                onInlineSave={handleInlineSave}
              />
              <NotesPagination show={totalPages > 1 && notes.length > 0} items={paginationItems} />
            </Col>
          </Row>
        </Container>
      </main>
      <Footer />

      <NoteEditModal
        show={noteModalOpen}
        onHide={() => setNoteModalOpen(false)}
        editingNote={editingNote}
        saveNote={saveNote}
        registerNote={registerNote}
        control={control}
        noteFormErrors={noteFormErrors}
        noteSubmitting={noteSubmitting}
        loadTagSuggestions={loadTagSuggestions}
        t={t}
      />
      <ConfirmModal
        show={deleteModalOpen}
        title={t('notes.delete.title')}
        message={t('notes.delete.message')}
        confirmLabel={t('common.delete')}
        onCancel={() => setDeleteModalOpen(false)}
        onConfirm={confirmDelete}
        t={t}
      />
      <ConfirmModal
        show={deleteForeverModalOpen}
        title={t('notes.deleteForever.title')}
        message={t('notes.deleteForever.message')}
        confirmLabel={t('common.delete')}
        onCancel={() => setDeleteForeverModalOpen(false)}
        onConfirm={confirmDeleteForever}
        t={t}
      />
      <ConfirmModal
        show={emptyTrashModalOpen}
        title={t('notes.emptyTrash.title')}
        message={t('notes.emptyTrash.message')}
        confirmLabel={t('notes.actions.emptyTrash')}
        onCancel={() => setEmptyTrashModalOpen(false)}
        onConfirm={confirmEmptyTrash}
        t={t}
      />
      <BulkConfirmModal show={bulkModalOpen} onCancel={() => setBulkModalOpen(false)} onConfirm={confirmBulkAction} t={t} />
      <ShareModal
        show={shareModalOpen}
        onHide={() => setShareModalOpen(false)}
        shareAlert={shareAlert}
        shareNoteTitle={shareNote?.title || ''}
        shareExpiry={shareExpiry}
        setShareExpiry={setShareExpiry}
        shareExpiresAt={shareExpiresAt}
        setShareExpiresAt={setShareExpiresAt}
        shareOneTime={shareOneTime}
        setShareOneTime={setShareOneTime}
        shareSubmitting={shareSubmitting}
        onSubmit={handleShareSubmit}
        shareResult={shareResult}
        onCopyUrl={(url) => handleShareCopy(url, true)}
        shareLinks={shareLinks}
        shareLinksLoading={shareLinksLoading}
        shareLinksHasMore={shareLinksHasMore}
        onCopyToken={handleShareCopy}
        onRevokeLink={handleShareRevoke}
        onLoadMore={() => (shareNote ? loadShareLinks(shareNote, true) : undefined)}
        t={t}
      />
      <RevisionModal
        show={revisionModalOpen}
        onHide={() => setRevisionModalOpen(false)}
        revisionError={revisionError}
        revisionLoading={revisionLoading}
        revisions={revisions}
        revisionTotal={revisionTotal}
        diffOpen={diffOpen}
        onToggleDiff={toggleRevisionDiff}
        onRestoreRevision={handleRestoreRevision}
        hasMore={revisionHasMore}
        onLoadMore={loadMoreRevisions}
        t={t}
      />
      <OwnerModal
        show={ownerModalOpen}
        onHide={() => setOwnerModalOpen(false)}
        ownerQuery={ownerQuery}
        onQueryChange={handleOwnerQueryChange}
        suggestions={ownerSuggestions}
        onSelectSuggestion={setOwnerQuery}
        hasMore={ownerHasMore}
        onLoadMore={() => loadOwnerSuggestions(ownerQuery.trim(), true)}
        loading={ownerLoading}
        onSubmit={submitOwnerChange}
        t={t}
      />
    </>
  );
}

export { getStaticPaths } from '@lib/getStatic';

export const getStaticProps = makeStaticProps(['common']);
