'use client';

import { useEffect, useMemo, useState } from 'react';
import Container from 'react-bootstrap/Container';
import Alert from 'react-bootstrap/Alert';
import Card from 'react-bootstrap/Card';
import Badge from 'react-bootstrap/Badge';
import Spinner from 'react-bootstrap/Spinner';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCircle, faThumbtack, faUser } from '@fortawesome/free-solid-svg-icons';
import { faNoteSticky, faCalendar, faClock } from '@fortawesome/free-regular-svg-icons';
import { useTranslation } from 'next-i18next';
import AppNavbar from '@components/AppNavbar';
import Footer from '@components/Footer';
import Api, { ApiError } from '@lib/api';
import { getStaticPaths, makeStaticProps } from '@lib/getStatic';
import type { NoteDTO } from '@lib/types';
import { getLocation } from '@lib/window';

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
  const { t } = useTranslation();
  const searchParams = useMemo(() => new URLSearchParams(getLocation()?.search || ''), []);
  const tokenParam = searchParams.get('share_token') || '';
  const [note, setNote] = useState<NoteDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const tokenFromPath = useMemo(() => {
    const path = getLocation()?.pathname || '';
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
        setError(t('share.error.invalidLink'));
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
          setError(t('share.error.unavailable'));
        } else {
          setError(apiErr?.message || t('share.error.loadFailed'));
        }
      } finally {
        if (active) setLoading(false);
      }
    };
    loadNote();
    return () => {
      active = false;
    };
  }, [token, t]);

  const created = splitDateTime(note?.createdDate);
  const updated = splitDateTime(note?.lastModifiedDate);

  return (
    <>
      <AppNavbar badgeLabel={t('share.badge')} showHomeButton={true} showAuthDropdown={true} requireAuthForActions={true} />
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
                    <h1 className="h4 mb-0">{note.title || t('share.note.untitled')}</h1>
                    {note.pinned ? (
                      <FontAwesomeIcon icon={faThumbtack} className="text-warning" title={t('share.note.pinnedTitle')} />
                    ) : null}
                    {note.color ? (
                      <Badge bg="body-secondary" text="body" className="border" style={{ borderColor: note.color }}>
                        <FontAwesomeIcon icon={faCircle} style={{ color: note.color }} />
                      </Badge>
                    ) : null}
                  </div>
                  <Badge bg="secondary-subtle" text="secondary" className="text-uppercase">
                    {t('share.badge')}
                  </Badge>
                </div>
                <p className="text-muted">{note.content || ''}</p>
                <div className="d-flex flex-wrap gap-2 mb-3">
                  {(note.tags || []).map((tag) => {
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
                    {t('share.fields.owner')} {note.owner || '—'}
                  </span>
                  <span className="d-inline-flex align-items-center gap-2">
                    <FontAwesomeIcon icon={faUser} />
                    {t('share.fields.createdBy')} {note.createdBy || '—'}
                  </span>
                  <span className="d-inline-flex align-items-center gap-2">
                    <FontAwesomeIcon icon={faUser} />
                    {t('share.fields.updatedBy')} {note.lastModifiedBy || '—'}
                  </span>
                  <span className="d-inline-flex align-items-center gap-2">
                    <FontAwesomeIcon icon={faCalendar} />
                    {t('share.fields.created')} {created.date}{' '}
                    <span className="d-inline-flex align-items-center gap-1">
                      <FontAwesomeIcon icon={faClock} /> {created.time}
                    </span>
                  </span>
                  <span className="d-inline-flex align-items-center gap-2">
                    <FontAwesomeIcon icon={faCalendar} />
                    {t('share.fields.updated')} {updated.date}{' '}
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

const getStaticProps = makeStaticProps(['common']);

export { getStaticPaths, getStaticProps };
