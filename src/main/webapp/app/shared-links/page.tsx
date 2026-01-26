'use client';

import { useCallback, useEffect, useMemo, useRef, useState, type JSX } from 'react';
import Container from 'react-bootstrap/Container';
import Form from 'react-bootstrap/Form';
import InputGroup from 'react-bootstrap/InputGroup';
import Button from 'react-bootstrap/Button';
import Alert from 'react-bootstrap/Alert';
import Badge from 'react-bootstrap/Badge';
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
import AppNavbar from '../components/AppNavbar';
import Footer from '../components/Footer';
import Api, { ApiError } from '../lib/api';
import { addDays, addHours, formatDate, toIsoString } from '../lib/format';
import { useToasts } from '../components/ToastProvider';
import useAuth from '../lib/useAuth';
import type { ShareLinkDTO } from '../types';

const defaultPageSize = 10;

function buildShareUrl(token: string | undefined) {
  const origin = ((globalThis as any).location as Location | undefined)?.origin;
  if (!origin || !token) return '';
  return `${origin}/share?share_token=${encodeURIComponent(token)}`;
}

export default function SharedLinksPage() {
  const { pushToast } = useToasts();
  const { loading: authLoading, isAuthenticated } = useAuth();
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
        setAlert(apiErr?.message || 'Could not load shared links.');
      } finally {
        loadingRef.current = false;
        setLoading(false);
      }
    },
    [authLoading, createdFrom, createdTo, debouncedSearch, isAuthenticated, pageSize, sort, status],
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
      pushToast('Share link copied', 'success');
    } catch {
      pushToast('Could not copy link. Copy manually.', 'warning');
    }
  };

  const handleRevoke = async (id: string | number) => {
    try {
      await Api.revokeShareLink(id);
      pushToast('Share link revoked', 'success');
      loadLinks(0);
    } catch (err: unknown) {
      const apiErr = err instanceof ApiError ? err : null;
      pushToast(apiErr?.message || 'Could not revoke link.', 'danger');
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
      setCustomError('Start and end dates are both required.');
      return;
    }
    if (new Date(fromVal) > new Date(toVal)) {
      setCustomError('Start date cannot be after end date.');
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
      <AppNavbar showHomeButton={true} showAuthDropdown={true} />
      <header className="py-4 mb-4 shadow-sm bg-body-tertiary">
        <Container className="d-flex flex-column flex-md-row align-items-md-center justify-content-between">
          <div>
            <h1 className="h4 mb-1 text-body">Shared Links</h1>
            <p className="mb-0 text-muted">See every link you’ve issued, check status, and revoke when needed.</p>
          </div>
        </Container>
      </header>
      <main className="bg-body-tertiary flex-grow-1">
        <Container className="pb-5">
          <div className="d-flex justify-content-between align-items-center gap-3 mb-2 flex-wrap">
            <div className="d-flex align-items-center gap-2 ms-auto">
              {loading ? <Spinner size="sm" className="text-primary" /> : null}
              <div className="d-flex flex-column text-end">
                {links.length ? <span className="text-muted small">Total: {totalElements}</span> : null}
                {links.length ? (
                  <span className="text-muted small">
                    Page {totalPages ? page + 1 : 0} of {totalPages}
                  </span>
                ) : null}
              </div>
            </div>
          </div>
          <div className="d-flex flex-column gap-2 mb-3">
            <div className="d-flex flex-wrap align-items-center gap-3">
              <div className="flex-grow-1" style={{ minWidth: 220, maxWidth: 360 }}>
                <InputGroup size="sm" className="w-100">
                  <InputGroup.Text>
                    <FontAwesomeIcon icon={faMagnifyingGlass} />
                  </InputGroup.Text>
                  <Form.Control
                    type="text"
                    placeholder="Search links or titles"
                    value={search}
                    onChange={event => setSearch(event.target.value)}
                  />
                  <Button variant="outline-secondary" onClick={() => setSearch('')} aria-label="Clear search">
                    &times;
                  </Button>
                </InputGroup>
              </div>
              <div className="d-flex flex-wrap align-items-center gap-3 ms-md-auto">
                <div className="d-flex align-items-center gap-2">
                  <Form.Label htmlFor="sharedLinksPageSize" className="small mb-0 text-muted">
                    Page size
                  </Form.Label>
                  <Form.Select
                    id="sharedLinksPageSize"
                    size="sm"
                    value={pageSize}
                    onChange={event => setPageSize(Number(event.target.value))}
                  >
                    <option value={5}>5</option>
                    <option value={10}>10</option>
                    <option value={25}>25</option>
                  </Form.Select>
                </div>
                <div className="d-flex align-items-center gap-2">
                  <Form.Label htmlFor="sharedLinksSort" className="small mb-0 text-muted">
                    Sort
                  </Form.Label>
                  <Form.Select id="sharedLinksSort" size="sm" value={sort} onChange={event => setSort(event.target.value)}>
                    <option value="createdDate,desc">Created (newest)</option>
                    <option value="createdDate,asc">Created (oldest)</option>
                    <option value="useCount,desc">Used (most)</option>
                  </Form.Select>
                </div>
              </div>
            </div>
            <div className="d-flex flex-wrap align-items-center gap-3 w-100">
              <div className="d-flex align-items-center gap-2">
                <Form.Label htmlFor="sharedLinksStatus" className="small mb-0 text-muted">
                  Status
                </Form.Label>
                <Form.Select id="sharedLinksStatus" size="sm" value={status} onChange={event => setStatus(event.target.value)}>
                  <option value="all">All</option>
                  <option value="active">Active</option>
                  <option value="expired">Expired</option>
                  <option value="revoked">Revoked</option>
                </Form.Select>
              </div>
              <div className="d-flex flex-wrap align-items-center gap-2">
                <Form.Label htmlFor="sharedLinksDateFilter" className="small mb-0 text-muted">
                  Created date
                </Form.Label>
                <Form.Select
                  id="sharedLinksDateFilter"
                  size="sm"
                  value={dateFilter === 'custom' ? 'custom' : dateFilter}
                  onChange={event => handleDateFilterChange(event.target.value)}
                >
                  <option value="none">Any time</option>
                  <option value="created_last_24h">Last 24h</option>
                  <option value="created_last_7d">Last 7 days</option>
                  <option value="created_last_month">Last month</option>
                  <option value="custom">Custom range</option>
                </Form.Select>
                {dateFilter === 'custom' && createdFrom && createdTo ? (
                  <span className="text-muted small">
                    {formatDate(createdFrom)} - {formatDate(createdTo)}
                  </span>
                ) : null}
              </div>
            </div>
          </div>
          {alert ? <Alert variant="danger">{alert}</Alert> : null}
          <ListGroup>
            {showEmpty ? (
              <ListGroup.Item className="text-muted small d-flex align-items-center gap-2">
                <FontAwesomeIcon icon={faLinkSlash} />
                <span>No shared links found. Create one to get started.</span>
              </ListGroup.Item>
            ) : null}
            {loading ? (
              <ListGroup.Item className="d-flex justify-content-center py-3">
                <Spinner animation="border" variant="primary" />
              </ListGroup.Item>
            ) : null}
            {links.map(link => {
              const statusBadge = link.revoked
                ? { text: 'Revoked', className: 'bg-secondary-subtle text-secondary' }
                : link.expired
                  ? { text: 'Expired', className: 'bg-warning-subtle text-warning' }
                  : { text: 'Active', className: 'bg-success-subtle text-success' };
              const linkUrl = buildShareUrl(link.token);
              return (
                <ListGroup.Item key={link.id} className="d-flex flex-column gap-2">
                  <div className="d-flex justify-content-between align-items-start flex-wrap gap-2">
                    <div className="d-flex align-items-center gap-2 flex-wrap">
                      <span className="fw-semibold text-primary">
                        Link {link.token ? `${link.token.slice(0, 6)}…${link.token.slice(-4)}` : `#${link.id}`}
                      </span>
                      <Badge className={statusBadge.className}>{statusBadge.text}</Badge>
                      {link.oneTime ? <Badge className="bg-secondary-subtle text-secondary">One-time</Badge> : null}
                    </div>
                    <div className="d-flex gap-2">
                      <Button
                        variant="outline-secondary"
                        size="sm"
                        onClick={() => handleCopy(link.token)}
                        disabled={!linkUrl || link.revoked}
                      >
                        <FontAwesomeIcon icon={faCopy} className="me-1" /> Copy
                      </Button>
                      <Button variant="outline-danger" size="sm" onClick={() => handleRevoke(link.id)} disabled={link.revoked}>
                        <FontAwesomeIcon icon={faTrash} className="me-1" /> Revoke
                      </Button>
                    </div>
                  </div>
                  <div className="text-muted small d-flex flex-column gap-1">
                    <span className="d-inline-flex align-items-center gap-1">
                      <FontAwesomeIcon icon={faNoteSticky} />
                      <span>Note:</span>
                      <span>{link.noteTitle || '—'}</span>
                    </span>
                    <span className="d-inline-flex align-items-center gap-1">
                      <FontAwesomeIcon icon={faUser} />
                      <span>Owner:</span>
                      <span>{link.noteOwner || '—'}</span>
                    </span>
                    <span className="d-inline-flex align-items-center gap-1">
                      <FontAwesomeIcon icon={faCalendar} />
                      <span>Created:</span>
                      <span>{formatDate(link.createdDate) || '—'}</span>
                    </span>
                    <span className="d-inline-flex align-items-center gap-1">
                      <FontAwesomeIcon icon={faCalendarXmark} />
                      <span>Expires:</span>
                      <span>{link.expiresAt ? formatDate(link.expiresAt) : 'No expiry'}</span>
                    </span>
                    <span className="d-inline-flex align-items-center gap-1">
                      <FontAwesomeIcon icon={faCalendarCheck} />
                      <span>Last used:</span>
                      <span>{link.lastUsedAt ? formatDate(link.lastUsedAt) : '—'}</span>
                    </span>
                    <span className="d-inline-flex align-items-center gap-1">
                      <FontAwesomeIcon icon={faChartColumn} />
                      <span>Used:</span>
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
          <Modal.Title>Custom date range</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form>
            <Form.Group className="mb-3">
              <Form.Label className="small mb-1">From</Form.Label>
              <Form.Control type="datetime-local" value={customFrom} onChange={event => setCustomFrom(event.target.value)} />
            </Form.Group>
            <Form.Group className="mb-2">
              <Form.Label className="small mb-1">To</Form.Label>
              <Form.Control type="datetime-local" value={customTo} onChange={event => setCustomTo(event.target.value)} />
            </Form.Group>
            <div className="text-muted small">Both dates are required. Start date must not be after end date.</div>
            {customError ? <div className="invalid-feedback d-block">{customError}</div> : null}
          </Form>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-secondary" size="sm" onClick={() => setCustomModalOpen(false)}>
            Cancel
          </Button>
          <Button variant="primary" size="sm" onClick={handleCustomSave}>
            Apply
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  );
}
