import Api from '/js/api.js';
import Theme from '/js/theme.js';
import State from '/js/state.js';
import Helpers from '/js/helpers.js';

const { escapeHtml, formatDate, showToast } = Helpers;

const listEl = document.getElementById('sharedLinksList');
const emptyEl = document.getElementById('sharedLinksEmpty');
const alertEl = document.getElementById('sharedLinksAlert');
const sharedLinksTitle = document.getElementById('sharedLinksTitle');
const sharedLinksSubtitle = document.getElementById('sharedLinksSubtitle');
const scopeGroup = document.getElementById('sharedLinksScope'); // kept for compatibility; hidden on load
const themeToggle = document.getElementById('themeToggle');
const themeToggleIcon = document.getElementById('themeToggleIcon');
const themeToggleLabel = document.getElementById('themeToggleLabel');
const pageSizeSelect = document.getElementById('sharedLinksPageSize');
const sortSelect = document.getElementById('sharedLinksSort');
const authBtn = document.getElementById('authBtn');
const authBtnLabel = document.getElementById('authBtnLabel');
const authUserLabel = document.getElementById('authUserLabel');
const authMenu = document.getElementById('authMenu');
const signOutBtn = document.getElementById('signOutBtn');
const signOutDivider = document.getElementById('signOutDivider');
const totalLabel = document.getElementById('sharedLinksTotal');
const pagination = document.getElementById('sharedLinksPagination');
const pager = document.getElementById('sharedLinksPager');
const pageInfo = document.getElementById('sharedLinksPageInfo');
const refreshSpinner = document.getElementById('sharedLinksRefreshSpinner');
const loadingRow = document.getElementById('sharedLinksLoading');

totalLabel?.classList.add('d-none');
pageInfo?.classList.add('d-none');

let page = 0;
let totalPages = 1;
let loading = false;
let pageSize = 10;
let sort = 'createdDate,desc';
let isAdmin = false;
const LOADING_ROWS = `
    <div class="list-group-item text-center py-4 border-0">
        <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">Loading...</span>
        </div>
    </div>`;

function getErrorMessage(error, fallback = 'Request failed') {
    if (error?.body?.detail) return error.body.detail;
    if (error?.body?.title) return error.body.title;
    if (error?.message) return error.message;
    return fallback;
}

async function handleApi(promise, options = {}) {
    const { fallback = 'Request failed', onError, onFinally, silent } = options;
    try {
        return await promise;
    } catch (e) {
        const message = getErrorMessage(e, fallback);
        if (onError) {
            onError(e, message);
        } else if (!silent) {
            showToast(message, 'danger', null, e?.body?.title || e?.title);
        }
        return null;
    } finally {
        onFinally?.();
    }
}

document.addEventListener('DOMContentLoaded', () => {
    Theme.init({ button: themeToggle, icon: themeToggleIcon, label: themeToggleLabel });
    initAuth().then((admin) => {
        isAdmin = admin;
        scopeGroup?.classList.add('d-none');
        if (sharedLinksTitle) sharedLinksTitle.textContent = admin ? 'Shared Links' : 'My Shared Links';
        if (sharedLinksSubtitle) sharedLinksSubtitle.textContent = admin
            ? 'See every link you’ve issued, check status, and revoke when needed.'
            : 'See every link you’ve issued, check status, and revoke when needed.';
        if (pageSizeSelect) {
            const val = parseInt(pageSizeSelect.value, 10);
            pageSize = Number.isNaN(val) ? pageSize : val;
        }
        if (sortSelect) {
            sort = sortSelect.value || sort;
        }
        loadLinks();
    });
    bindEvents();
});

function bindEvents() {
    listEl?.addEventListener('click', handleListClick);
    pagination?.addEventListener('click', (e) => {
        const link = e.target.closest('a[data-page]');
        if (!link) return;
        e.preventDefault();
        const target = parseInt(link.getAttribute('data-page'), 10);
        if (Number.isNaN(target) || target === page || target < 0 || target >= totalPages || loading) return;
        loadLinks(target);
    });
    pageSizeSelect?.addEventListener('change', () => {
        const val = parseInt(pageSizeSelect.value, 10);
        pageSize = Number.isNaN(val) ? 10 : val;
        page = 0;
        loadLinks(0);
    });
    sortSelect?.addEventListener('change', () => {
        sort = sortSelect.value || 'createdDate,desc';
        page = 0;
        loadLinks(0);
    });
    signOutBtn?.addEventListener('click', async () => {
        try {
            await Api.logout();
        } finally {
            State.clearToken();
            window.location.replace('/login.html');
        }
    });
}

function hideAlert() {
    alertEl?.classList.add('d-none');
}

async function initAuth() {
    const current = State.currentUser?.();
    if (current?.username) {
        State.setCurrentUser(current);
        updateAuthUi(current.username);
        return State.isAdmin?.();
    }
    try {
        const me = await Api.currentUser();
        State.setCurrentUser(me);
        updateAuthUi(me?.username);
        return State.isAdmin?.();
    } catch (e) {
        updateAuthUi('');
        window.location.replace('/login.html');
        return false;
    }
}

function updateAuthUi(username) {
    const signedIn = !!username;
    authBtn?.classList.toggle('d-none', !signedIn);
    authBtn?.classList.toggle('dropdown-toggle', signedIn);
    if (signedIn) {
        authBtn?.setAttribute('data-bs-toggle', 'dropdown');
        if (authBtnLabel) authBtnLabel.textContent = username;
    } else {
        authBtn?.removeAttribute('data-bs-toggle');
        if (authBtnLabel) authBtnLabel.textContent = '';
    }
    if (authUserLabel) {
        authUserLabel.classList.toggle('d-none', !signedIn);
        authUserLabel.textContent = signedIn ? `Signed in as ${username}` : '';
    }
    authMenu?.classList.toggle('d-none', !signedIn);
    signOutDivider?.classList.toggle('d-none', !signedIn);
    signOutBtn?.classList.toggle('d-none', !signedIn);
}

function setLoading(isLoading) {
    loading = isLoading;
    refreshSpinner?.classList.toggle('d-none', !isLoading);
    if (isLoading) {
        if (listEl && loadingRow && !listEl.contains(loadingRow)) {
            listEl.prepend(loadingRow);
        }
        loadingRow?.classList.remove('d-none');
    } else {
        loadingRow?.classList.add('d-none');
    }
}

function buildRow(link) {
    const status = link.revoked
        ? '<span class="badge bg-secondary-subtle text-secondary">Revoked</span>'
        : link.expired
            ? '<span class="badge bg-warning-subtle text-warning">Expired</span>'
            : '<span class="badge bg-success-subtle text-success">Active</span>';
    const noteTitle = link.noteTitle || '—';
    const noteOwner = link.noteOwner || '—';
    const tokenShort = link.token ? `${escapeHtml(link.token.substring(0, 6))}…${escapeHtml(link.token.slice(-4))}` : `#${link.id}`;
    const linkUrl = link.token ? `${window.location.origin}/share/${encodeURIComponent(link.token)}` : '';
    const copyDisabled = !linkUrl || link.revoked;
    const revokeDisabled = link.revoked;
    const expiresText = link.expiresAt ? escapeHtml(formatDate(link.expiresAt) || '') : 'No expiry';
    const createdText = escapeHtml(formatDate(link.createdDate) || '');
    const usedCount = link.useCount || 0;
    const lastUsedText = link.lastUsedAt ? escapeHtml(formatDate(link.lastUsedAt) || '') : '—';
    return `
        <div class="list-group-item d-flex flex-column gap-2" data-share-id="${link.id}">
            <div class="d-flex justify-content-between align-items-start flex-wrap gap-2">
                <div class="d-flex align-items-center gap-2 flex-wrap">
                    <span class="fw-semibold text-primary">Link ${tokenShort}</span>
                    ${status}
                    ${link.oneTime ? '<span class="badge bg-secondary-subtle text-secondary">One-time</span>' : ''}
                </div>
                <div class="d-flex gap-2">
                    <button class="btn btn-outline-secondary btn-sm" type="button" data-action="copy" data-token="${escapeHtml(link.token || '')}" ${copyDisabled ? 'disabled' : ''}>
                        <i class="fa-solid fa-copy"></i> Copy
                    </button>
                    <button class="btn btn-outline-danger btn-sm" type="button" data-action="revoke" data-id="${link.id}" ${revokeDisabled ? 'disabled' : ''}>
                        <i class="fa-solid fa-trash"></i> Revoke
                    </button>
                </div>
            </div>
            <div class="text-muted small d-flex flex-column gap-1">
                <span class="d-inline-flex align-items-center gap-1">
                    <i class="fa-regular fa-note-sticky"></i>
                    <span>Note:</span>
                    <span>${escapeHtml(noteTitle)}</span>
                </span>
                <span class="d-inline-flex align-items-center gap-1">
                    <i class="fa-solid fa-user"></i>
                    <span>Owner:</span>
                    <span>${escapeHtml(noteOwner)}</span>
                </span>
                <span class="d-inline-flex align-items-center gap-1">
                    <i class="fa-regular fa-calendar"></i>
                    <span>Created:</span>
                    <span>${createdText}</span>
                </span>
                <span class="d-inline-flex align-items-center gap-1">
                    <i class="fa-regular fa-calendar-xmark"></i>
                    <span>Expires:</span>
                    <span>${expiresText}</span>
                </span>
                <span class="d-inline-flex align-items-center gap-1">
                    <i class="fa-regular fa-calendar-check"></i>
                    <span>Last used:</span>
                    <span>${lastUsedText}</span>
                </span>
                <span class="d-inline-flex align-items-center gap-1">
                    <i class="fa-solid fa-chart-column"></i>
                    <span>Used:</span>
                    <span>${usedCount}</span>
                </span>
            </div>
        </div>
    `;
}

async function loadLinks(targetPage = 0) {
    if (loading) return;
    setLoading(true);
    totalLabel?.classList.add('d-none');
    pageInfo?.classList.add('d-none');
    const res = await handleApi(Api.fetchMyShareLinks(targetPage, pageSize, sort), {
        fallback: 'Could not load shared links.',
        onFinally: () => setLoading(false)
    });
    if (!res) return;
    hideAlert?.();

    const content = Array.isArray(res) ? res : (res.content ?? []);
    const meta = res.page ?? res;
    page = typeof meta?.number === 'number' ? meta.number : targetPage;
    const derivedTotalPages = typeof meta?.totalPages === 'number' ? Math.max(1, meta.totalPages) : Math.max(1, page + 1);
    totalPages = derivedTotalPages;
    const totalElements = typeof meta?.totalElements === 'number' ? meta.totalElements : content.length;

    if (listEl) {
        listEl.innerHTML = LOADING_ROWS;
    }
    const hasItems = content.length > 0;
    emptyEl?.classList.toggle('d-none', hasItems);
    if (listEl) {
        listEl.innerHTML = '';
        if (emptyEl) {
            listEl.appendChild(emptyEl);
        }
    }
    content.forEach(link => {
        listEl?.insertAdjacentHTML('beforeend', buildRow(link));
    });

    if (totalLabel) {
        totalLabel.textContent = `Total: ${totalElements}`;
        totalLabel.classList.toggle('d-none', !hasItems);
    }
    if (pagination) {
        if (!hasItems) {
            pagination.innerHTML = '';
        } else {
            const items = [];
            items.push(`<li class="page-item ${page === 0 ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="${page - 1}" aria-label="Previous">&laquo;</a>
            </li>`);
            for (let i = 0; i < totalPages; i++) {
                items.push(`<li class="page-item ${i === page ? 'active' : ''}">
                    <a class="page-link" href="#" data-page="${i}">${i + 1}</a>
                </li>`);
            }
            items.push(`<li class="page-item ${page >= totalPages - 1 ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="${page + 1}" aria-label="Next">&raquo;</a>
            </li>`);
            pagination.innerHTML = items.join('');
        }
    }
    if (pageInfo) {
        const humanPage = totalPages > 0 ? page + 1 : 0;
        pageInfo.textContent = hasItems ? `Page ${humanPage} of ${totalPages}` : '';
        pageInfo.classList.toggle('d-none', !hasItems);
    }
    if (pager) {
        pager.hidden = totalPages < 1 || !hasItems;
    }
}

async function handleListClick(event) {
    const btn = event.target.closest('button[data-action]');
    if (!btn) return;
    const action = btn.getAttribute('data-action');
    if (action === 'copy') {
        const token = btn.getAttribute('data-token');
        if (!token) return;
        const url = `${window.location.origin}/share/${encodeURIComponent(token)}`;
        try {
            await navigator.clipboard.writeText(url);
            inlineCopied(btn);
        } catch (err) {
            showToast('Could not copy link. Copy manually.', 'warning');
        }
        return;
    }
    if (action === 'revoke') {
        const id = btn.getAttribute('data-id');
        if (!id) return;
        btn.disabled = true;
        const res = await handleApi(Api.revokeShareLink(id), {
            fallback: 'Could not revoke link.',
            onFinally: () => { btn.disabled = false; },
            silent: false
        });
        if (res !== null) {
            showToast('Share link revoked', 'success');
            page = 0;
            await loadLinks(0);
        }
    }
}

function inlineCopied(btn) {
    if (!btn) return;
    const original = btn.innerHTML;
    btn.innerHTML = '<i class="fa-solid fa-check"></i> Copied';
    btn.disabled = true;
    setTimeout(() => {
        btn.innerHTML = original;
        btn.disabled = false;
    }, 1200);
}
