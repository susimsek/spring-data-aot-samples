'use client';

import { useCallback, useEffect, useMemo, useRef, useState, type JSX } from 'react';
import Container from 'react-bootstrap/Container';
import Form from 'react-bootstrap/Form';
import InputGroup from 'react-bootstrap/InputGroup';
import Button from 'react-bootstrap/Button';
import Alert from 'react-bootstrap/Alert';
import Badge from 'react-bootstrap/Badge';
import Card from 'react-bootstrap/Card';
import ListGroup from 'react-bootstrap/ListGroup';
import Modal from 'react-bootstrap/Modal';
import Pagination from 'react-bootstrap/Pagination';
import Spinner from 'react-bootstrap/Spinner';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faCalendar,
  faCalendarCheck,
  faCalendarXmark,
  faChartColumn,
  faCopy,
  faLinkSlash,
  faMagnifyingGlass,
  faTrash,
  faUser,
} from '@fortawesome/free-solid-svg-icons';
import { faNoteSticky } from '@fortawesome/free-regular-svg-icons';
import { useTranslation } from 'next-i18next';
import AppNavbar from '@components/AppNavbar';
import Footer from '@components/Footer';
import { useToasts } from '@components/ToastProvider';
import Api, { ApiError } from '@lib/api';
import { addDays, addHours, formatDate, toIsoString } from '@lib/format';
import { getStaticPaths, makeStaticProps } from '@lib/getStatic';
import type { ShareLinkDTO } from '@lib/types';
import useAuth from '@lib/useAuth';
import { getLocation } from '@lib/window';

const defaultPageSize = 10;

function buildShareUrl(token: string | undefined) {
  const origin = getLocation()?.origin;
  if (!origin || !token) return '';
  return `${origin}/share?share_token=${encodeURIComponent(token)}`;
}

export default function SharedLinksPage() {
  const { pushToast } = useToasts();
  const { loading: authLoading, isAuthenticated } = useAuth();
  const { t } = useTranslation();
  const loadingRef = useRef(false);
  const [links, setLinks] = useState<ShareLinkDTO[]>([]);
  const [alert, setAlert] = useState('');
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [pageSize, setPageSize] = useState(defaultPageSize);
  const [sort, setSort] = useState('createdDate,desc');
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [status, setStatus] = useState('all');
  const [dateFilter, setDateFilter] = useState('none');
  const [createdFrom, setCreatedFrom] = useState('');
  const [createdTo, setCreatedTo] = useState('');
  const [customModalOpen, setCustomModalOpen] = useState(false);
  const [customFrom, setCustomFrom] = useState('');
  const [customTo, setCustomTo] = useState('');
  const [customError, setCustomError] = useState('');

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(search.trim()), 300);
    return () => clearTimeout(timer);
  }, [search]);

  useEffect(() => {
    if (dateFilter === 'custom') return;
    const now = new Date();
    if (dateFilter === 'created_last_24h') {
      setCreatedFrom(toIsoString(addHours(now, -24)));
      setCreatedTo(toIsoString(now));
      return;
    }
    if (dateFilter === 'created_last_7d') {
      setCreatedFrom(toIsoString(addDays(now, -7)));
      setCreatedTo(toIsoString(now));
      return;
    }
    if (dateFilter === 'created_last_month') {
      setCreatedFrom(toIsoString(addDays(now, -30)));
      setCreatedTo(toIsoString(now));
      return;
    }
    setCreatedFrom('');
    setCreatedTo('');
  }, [dateFilter]);

  useEffect(() => {
    setPage(0);
  }, [debouncedSearch, pageSize, sort, status, createdFrom, createdTo]);

  const loadLinks = useCallback(
    async (targetPage = 0) => {
      if (loadingRef.current || authLoading || !isAuthenticated) return;
      loadingRef.current = true;
      setLoading(true);
      setAlert('');
      try {
        const res = await Api.fetchMyShareLinks(sort, debouncedSearch, status, createdFrom, createdTo, targetPage, pageSize);
        const content = Array.isArray(res) ? res : res?.content || [];
        const meta = res?.page ?? res ?? {};
        const currentPage = typeof meta?.number === 'number' ? meta.number : targetPage;
        const total = typeof meta?.totalElements === 'number' ? meta.totalElements : content.length;
        const totalPagesValue = typeof meta?.totalPages === 'number' ? Math.max(1, meta.totalPages) : 1;
        setLinks(content);
        setPage(currentPage);
        setTotalPages(totalPagesValue);
        setTotalElements(total);
      } catch (err: unknown) {
        const apiErr = err instanceof ApiError ? err : null;
        setAlert(apiErr?.message || t('sharedLinks.error.loadFailed'));
      } finally {
        loadingRef.current = false;
        setLoading(false);
      }
    },
    [authLoading, createdFrom, createdTo, debouncedSearch, isAuthenticated, pageSize, sort, status, t],
  );

  useEffect(() => {
    if (authLoading || !isAuthenticated) return;
    loadLinks(page);
  }, [authLoading, isAuthenticated, loadLinks, page]);

  const handleCopy = async (token: string | undefined) => {
    const url = buildShareUrl(token);
    if (!url) return;
    try {
      await navigator.clipboard.writeText(url);
      pushToast(t('sharedLinks.toast.copied'), 'success');
    } catch {
      pushToast(t('sharedLinks.toast.copyFailed'), 'warning');
    }
  };

  const handleRevoke = async (id: string | number) => {
    try {
      await Api.revokeShareLink(id);
      pushToast(t('sharedLinks.toast.revoked'), 'success');
      loadLinks(0);
    } catch (err: unknown) {
      const apiErr = err instanceof ApiError ? err : null;
      pushToast(apiErr?.message || t('sharedLinks.toast.revokeFailed'), 'danger');
    }
  };

  const showEmpty = !loading && links.length === 0 && !alert;

  const paginationItems = useMemo(() => {
    if (totalPages <= 1) return [];
    const items: JSX.Element[] = [];
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

  const handleDateFilterChange = (value: string) => {
    if (value === 'custom') {
      setCustomModalOpen(true);
      setCustomError('');
      return;
    }
    setDateFilter(value);
  };

  const handleCustomSave = () => {
    const fromVal = toIsoString(customFrom);
    const toVal = toIsoString(customTo);
    if (!fromVal || !toVal) {
      setCustomError(t('sharedLinks.customRange.error.required'));
      return;
    }
    if (new Date(fromVal) > new Date(toVal)) {
      setCustomError(t('sharedLinks.customRange.error.order'));
      return;
    }
    setCustomError('');
    setDateFilter('custom');
    setCreatedFrom(fromVal);
    setCreatedTo(toVal);
    setCustomModalOpen(false);
    setPage(0);
  };

  return (
    <>
      <AppNavbar />
      <header className="py-4 mb-4 shadow-sm bg-body-tertiary">
        <Container className="d-flex flex-column flex-md-row align-items-md-center justify-content-between">
          <div>
            <h1 className="h4 mb-1 text-body">{t('sharedLinks.title')}</h1>
            <p className="mb-0 text-muted">{t('sharedLinks.subtitle')}</p>
          </div>
        </Container>
      </header>
      <main className="bg-body-tertiary flex-grow-1">
        <Container className="pb-5">
          <div className="d-flex justify-content-between align-items-center gap-3 mb-2 flex-wrap">
            <div className="d-flex align-items-center gap-2 ms-auto">
              {loading ? <Spinner size="sm" className="text-primary" /> : null}
              <div className="d-flex flex-column text-end">
                {links.length ? <span className="text-muted small">{t('pagination.summary.total', { count: totalElements })}</span> : null}
                {links.length ? (
                  <span className="text-muted small">
                    {t('pagination.summary.pageOf', { current: totalPages ? page + 1 : 0, total: totalPages })}
                  </span>
                ) : null}
              </div>
            </div>
          </div>
          <Card className="shadow-sm border-0 mb-3">
            <Card.Body>
              <div className="d-flex flex-wrap align-items-start gap-3">
                <div className="d-flex flex-column gap-2 flex-grow-1" style={{ minWidth: 320 }}>
                  <Form.Label className="small text-muted mb-0">{t('sharedLinks.filters.search.label')}</Form.Label>
                  <InputGroup size="sm" className="w-100">
                    <InputGroup.Text>
                      <FontAwesomeIcon icon={faMagnifyingGlass} />
                    </InputGroup.Text>
                    <Form.Control
                      type="text"
                      placeholder={t('sharedLinks.filters.search.placeholder')}
                      value={search}
                      onChange={(event) => setSearch(event.target.value)}
                    />
                    <Button
                      variant="outline-secondary"
                      onClick={() => setSearch('')}
                      aria-label={t('sharedLinks.filters.search.clearAria')}
                    >
                      &times;
                    </Button>
                  </InputGroup>
                </div>
                <div className="d-flex flex-column gap-2 flex-shrink-0" style={{ minWidth: 180 }}>
                  <Form.Label htmlFor="sharedLinksStatus" className="small text-muted mb-0">
                    {t('sharedLinks.filters.status.label')}
                  </Form.Label>
                  <Form.Select id="sharedLinksStatus" size="sm" value={status} onChange={(event) => setStatus(event.target.value)}>
                    <option value="all">{t('sharedLinks.filters.status.options.all')}</option>
                    <option value="active">{t('sharedLinks.filters.status.options.active')}</option>
                    <option value="expired">{t('sharedLinks.filters.status.options.expired')}</option>
                    <option value="revoked">{t('sharedLinks.filters.status.options.revoked')}</option>
                  </Form.Select>
                </div>
                <div className="d-flex flex-column gap-2 flex-shrink-0" style={{ minWidth: 180 }}>
                  <Form.Label htmlFor="sharedLinksDateFilter" className="small text-muted mb-0">
                    {t('sharedLinks.filters.createdDate.label')}
                  </Form.Label>
                  <Form.Select
                    id="sharedLinksDateFilter"
                    size="sm"
                    value={dateFilter === 'custom' ? 'custom' : dateFilter}
                    onChange={(event) => handleDateFilterChange(event.target.value)}
                  >
                    <option value="none">{t('sharedLinks.filters.createdDate.options.anyTime')}</option>
                    <option value="created_last_24h">{t('sharedLinks.filters.createdDate.options.last24h')}</option>
                    <option value="created_last_7d">{t('sharedLinks.filters.createdDate.options.last7d')}</option>
                    <option value="created_last_month">{t('sharedLinks.filters.createdDate.options.lastMonth')}</option>
                    <option value="custom">{t('sharedLinks.filters.createdDate.options.custom')}</option>
                  </Form.Select>
                  {dateFilter === 'custom' && createdFrom && createdTo ? (
                    <span className="text-muted small text-nowrap">
                      {formatDate(createdFrom)} - {formatDate(createdTo)}
                    </span>
                  ) : null}
                </div>
              </div>
            </Card.Body>
          </Card>

          <div className="d-flex flex-wrap align-items-center gap-3 mb-3">
            <div className="d-flex align-items-center gap-2">
              <Form.Label htmlFor="sharedLinksPageSize" className="small mb-0 text-muted text-nowrap">
                {t('pagination.pageSize.label')}
              </Form.Label>
              <Form.Select
                id="sharedLinksPageSize"
                size="sm"
                value={pageSize}
                onChange={(event) => setPageSize(Number(event.target.value))}
                style={{ width: 'auto' }}
              >
                <option value={5}>5</option>
                <option value={10}>10</option>
                <option value={25}>25</option>
              </Form.Select>
            </div>
            <div className="d-flex align-items-center gap-2">
              <Form.Label htmlFor="sharedLinksSort" className="small mb-0 text-muted text-nowrap">
                {t('pagination.sort.label')}
              </Form.Label>
              <Form.Select
                id="sharedLinksSort"
                size="sm"
                value={sort}
                onChange={(event) => setSort(event.target.value)}
                style={{ width: 'auto' }}
              >
                <option value="createdDate,desc">{t('sharedLinks.sort.options.createdNewest')}</option>
                <option value="createdDate,asc">{t('sharedLinks.sort.options.createdOldest')}</option>
                <option value="useCount,desc">{t('sharedLinks.sort.options.usedMost')}</option>
              </Form.Select>
            </div>
          </div>
          {alert ? <Alert variant="danger">{alert}</Alert> : null}
          <ListGroup>
            {showEmpty ? (
              <ListGroup.Item className="text-muted small d-flex align-items-center gap-2">
                <FontAwesomeIcon icon={faLinkSlash} />
                <span>{t('sharedLinks.empty')}</span>
              </ListGroup.Item>
            ) : null}
            {loading ? (
              <ListGroup.Item className="d-flex justify-content-center py-3">
                <Spinner animation="border" variant="primary" />
              </ListGroup.Item>
            ) : null}
            {links.map((link) => {
              const statusBadge = link.revoked
                ? { text: t('sharedLinks.status.revoked'), className: 'bg-secondary-subtle text-secondary' }
                : link.expired
                  ? { text: t('sharedLinks.status.expired'), className: 'bg-warning-subtle text-warning' }
                  : { text: t('sharedLinks.status.active'), className: 'bg-success-subtle text-success' };
              const linkUrl = buildShareUrl(link.token);
              return (
                <ListGroup.Item key={link.id} className="d-flex flex-column gap-2">
                  <div className="d-flex justify-content-between align-items-start flex-wrap gap-2">
                    <div className="d-flex align-items-center gap-2 flex-wrap">
                      <span className="fw-semibold text-primary">
                        {t('sharedLinks.link.label')} {link.token ? `${link.token.slice(0, 6)}…${link.token.slice(-4)}` : `#${link.id}`}
                      </span>
                      <Badge className={statusBadge.className}>{statusBadge.text}</Badge>
                      {link.oneTime ? <Badge className="bg-secondary-subtle text-secondary">{t('sharedLinks.oneTime')}</Badge> : null}
                    </div>
                    <div className="d-flex gap-2">
                      <Button
                        variant="outline-secondary"
                        size="sm"
                        onClick={() => handleCopy(link.token)}
                        disabled={!linkUrl || link.revoked}
                      >
                        <FontAwesomeIcon icon={faCopy} className="me-1" /> {t('sharedLinks.actions.copy')}
                      </Button>
                      <Button variant="outline-danger" size="sm" onClick={() => handleRevoke(link.id)} disabled={link.revoked}>
                        <FontAwesomeIcon icon={faTrash} className="me-1" /> {t('sharedLinks.actions.revoke')}
                      </Button>
                    </div>
                  </div>
                  <div className="text-muted small d-flex flex-column gap-1">
                    <span className="d-inline-flex align-items-center gap-1">
                      <FontAwesomeIcon icon={faNoteSticky} />
                      <span>{t('sharedLinks.fields.note')}</span>
                      <span>{link.noteTitle || '—'}</span>
                    </span>
                    <span className="d-inline-flex align-items-center gap-1">
                      <FontAwesomeIcon icon={faUser} />
                      <span>{t('sharedLinks.fields.owner')}</span>
                      <span>{link.noteOwner || '—'}</span>
                    </span>
                    <span className="d-inline-flex align-items-center gap-1">
                      <FontAwesomeIcon icon={faCalendar} />
                      <span>{t('sharedLinks.fields.created')}</span>
                      <span>{formatDate(link.createdDate) || '—'}</span>
                    </span>
                    <span className="d-inline-flex align-items-center gap-1">
                      <FontAwesomeIcon icon={faCalendarXmark} />
                      <span>{t('sharedLinks.fields.expires')}</span>
                      <span>{link.expiresAt ? formatDate(link.expiresAt) : t('sharedLinks.noExpiry')}</span>
                    </span>
                    <span className="d-inline-flex align-items-center gap-1">
                      <FontAwesomeIcon icon={faCalendarCheck} />
                      <span>{t('sharedLinks.fields.lastUsed')}</span>
                      <span>{link.lastUsedAt ? formatDate(link.lastUsedAt) : '—'}</span>
                    </span>
                    <span className="d-inline-flex align-items-center gap-1">
                      <FontAwesomeIcon icon={faChartColumn} />
                      <span>{t('sharedLinks.fields.used')}</span>
                      <span>{link.useCount || 0}</span>
                    </span>
                  </div>
                </ListGroup.Item>
              );
            })}
          </ListGroup>
          {totalPages > 1 && links.length ? (
            <div className="d-flex justify-content-center mt-3">
              <Pagination>{paginationItems}</Pagination>
            </div>
          ) : null}
        </Container>
      </main>
      <Footer />

      <Modal show={customModalOpen} onHide={() => setCustomModalOpen(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>{t('sharedLinks.customRange.title')}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form>
            <Form.Group className="mb-3">
              <Form.Label className="small mb-1">{t('sharedLinks.customRange.from')}</Form.Label>
              <Form.Control type="datetime-local" value={customFrom} onChange={(event) => setCustomFrom(event.target.value)} />
            </Form.Group>
            <Form.Group className="mb-2">
              <Form.Label className="small mb-1">{t('sharedLinks.customRange.to')}</Form.Label>
              <Form.Control type="datetime-local" value={customTo} onChange={(event) => setCustomTo(event.target.value)} />
            </Form.Group>
            <div className="text-muted small">{t('sharedLinks.customRange.helper')}</div>
            {customError ? <div className="invalid-feedback d-block">{customError}</div> : null}
          </Form>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-secondary" size="sm" onClick={() => setCustomModalOpen(false)}>
            {t('common.cancel')}
          </Button>
          <Button variant="primary" size="sm" onClick={handleCustomSave}>
            {t('common.apply')}
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  );
}

const getStaticProps = makeStaticProps(['common']);

export { getStaticPaths, getStaticProps };
