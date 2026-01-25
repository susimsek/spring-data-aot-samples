'use client';

import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import Container from 'react-bootstrap/Container';
import Alert from 'react-bootstrap/Alert';
import Card from 'react-bootstrap/Card';
import Badge from 'react-bootstrap/Badge';
import Spinner from 'react-bootstrap/Spinner';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCircle, faThumbtack, faUser } from '@fortawesome/free-solid-svg-icons';
import { faNoteSticky, faCalendar, faClock } from '@fortawesome/free-regular-svg-icons';
import AppNavbar from '../components/AppNavbar';
import Footer from '../components/Footer';
import Api, { ApiError } from '../lib/api';
import type { NoteDTO } from '../types';

function splitDateTime(value: string | null | undefined): { date: string; time: string } {
  if (!value) return { date: '—', time: '—' };
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return { date: String(value), time: '' };
  return {
    date: date.toLocaleDateString(),
    time: date.toLocaleTimeString(),
  };
}

export default function SharePageClient() {
  const searchParams = useSearchParams();
  const tokenParam = searchParams?.get('share_token') || '';
  const [note, setNote] = useState<NoteDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const tokenFromPath = useMemo(() => {
    const path = ((globalThis as any).location as Location | undefined)?.pathname || '';
    const parts = path.split('/').filter(Boolean);
    if (parts.length >= 2 && parts[0] === 'share') {
      return parts[1];
    }
    return '';
  }, []);

  const token = tokenParam || tokenFromPath;

  useEffect(() => {
    let active = true;
    const loadNote = async () => {
      if (!token) {
        setError('Invalid share link.');
        setLoading(false);
        return;
      }
      try {
        const data = await Api.fetchNoteWithShareToken(token);
        if (!active) return;
        setNote(data);
        setError('');
      } catch (err: unknown) {
        if (!active) return;
        const apiErr = err instanceof ApiError ? err : null;
        if (apiErr?.status === 401 || apiErr?.status === 403) {
          setError('This share link is no longer available. Please request a new link.');
        } else {
          setError(apiErr?.message || 'Could not load note.');
        }
      } finally {
        if (active) setLoading(false);
      }
    };
    loadNote();
    return () => {
      active = false;
    };
  }, [token]);

  const created = splitDateTime(note?.createdDate);
  const updated = splitDateTime(note?.lastModifiedDate);

  return (
    <>
      <AppNavbar badgeLabel="Shared" showHomeButton={true} showAuthDropdown={true} requireAuthForActions={true} />
      <main className="bg-body-tertiary flex-grow-1">
        <Container className="py-5">
          {loading ? (
            <div className="d-flex justify-content-center py-5">
              <Spinner animation="border" variant="primary" />
            </div>
          ) : null}
          {error ? <Alert variant="danger">{error}</Alert> : null}
          {!loading && note ? (
            <Card className="shadow-sm border-0">
              <Card.Body>
                <div className="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-3">
                  <div className="d-flex align-items-center gap-2 flex-wrap">
                    <h1 className="h4 mb-0">{note.title || 'Untitled'}</h1>
                    {note.pinned ? <FontAwesomeIcon icon={faThumbtack} className="text-warning" title="Pinned" /> : null}
                    {note.color ? (
                      <Badge bg="body-secondary" text="body" className="border" style={{ borderColor: note.color }}>
                        <FontAwesomeIcon icon={faCircle} style={{ color: note.color }} />
                      </Badge>
                    ) : null}
                  </div>
                  <Badge bg="secondary-subtle" text="secondary" className="text-uppercase">
                    Shared
                  </Badge>
                </div>
                <p className="text-muted">{note.content || ''}</p>
                <div className="d-flex flex-wrap gap-2 mb-3">
                  {(note.tags || []).map(tag => {
                    const label = typeof tag === 'string' ? tag : (tag.name ?? tag.label ?? String(tag));
                    return (
                      <Badge key={label} bg="secondary-subtle" text="secondary">
                        {label}
                      </Badge>
                    );
                  })}
                </div>
                <div className="text-muted small d-flex flex-column gap-2">
                  <span className="d-inline-flex align-items-center gap-2">
                    <FontAwesomeIcon icon={faNoteSticky} />
                    Owner: {note.owner || '—'}
                  </span>
                  <span className="d-inline-flex align-items-center gap-2">
                    <FontAwesomeIcon icon={faUser} />
                    Created by: {note.createdBy || '—'}
                  </span>
                  <span className="d-inline-flex align-items-center gap-2">
                    <FontAwesomeIcon icon={faUser} />
                    Updated by: {note.lastModifiedBy || '—'}
                  </span>
                  <span className="d-inline-flex align-items-center gap-2">
                    <FontAwesomeIcon icon={faCalendar} />
                    Created: {created.date}{' '}
                    <span className="d-inline-flex align-items-center gap-1">
                      <FontAwesomeIcon icon={faClock} /> {created.time}
                    </span>
                  </span>
                  <span className="d-inline-flex align-items-center gap-2">
                    <FontAwesomeIcon icon={faCalendar} />
                    Updated: {updated.date}{' '}
                    <span className="d-inline-flex align-items-center gap-1">
                      <FontAwesomeIcon icon={faClock} /> {updated.time}
                    </span>
                  </span>
                </div>
              </Card.Body>
            </Card>
          ) : null}
        </Container>
      </main>
      <Footer />
    </>
  );
}
