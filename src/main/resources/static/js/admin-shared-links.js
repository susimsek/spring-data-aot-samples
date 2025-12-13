import Api from '/js/api.js';
import Theme from '/js/theme.js';
import State from '/js/state.js';
import Helpers from '/js/helpers.js';

const { escapeHtml, formatDate, showToast } = Helpers;

const listEl = document.getElementById('sharedLinksList');
const emptyEl = document.getElementById('sharedLinksEmpty');
const alertEl = document.getElementById('sharedLinksAlert');
const loadMoreBtn = document.getElementById('sharedLinksLoadMore');
const loadMoreSpinner = document.getElementById('sharedLinksLoadMoreSpinner');
const loadMoreLabel = document.getElementById('sharedLinksLoadMoreLabel');
const refreshBtn = document.getElementById('sharedLinksRefresh');
const refreshSpinner = document.getElementById('sharedLinksRefreshSpinner');
const themeToggle = document.getElementById('themeToggle');
const themeToggleIcon = document.getElementById('themeToggleIcon');
const themeToggleLabel = document.getElementById('themeToggleLabel');
const authBtn = document.getElementById('authBtn');
const authBtnLabel = document.getElementById('authBtnLabel');
const authUserLabel = document.getElementById('authUserLabel');
const authMenu = document.getElementById('authMenu');
const signOutBtn = document.getElementById('signOutBtn');
const signOutDivider = document.getElementById('signOutDivider');

let page = 0;
let hasMore = false;
let loading = false;
const PAGE_SIZE = 10;

document.addEventListener('DOMContentLoaded', () => {
    Theme.init({ button: themeToggle, icon: themeToggleIcon, label: themeToggleLabel });
    initAuth().then((isAdmin) => {
        if (!isAdmin) {
            showError('This page is available for admins only.');
            return;
        }
        loadLinks();
    });
    bindEvents();
});

function bindEvents() {
    refreshBtn?.addEventListener('click', () => loadLinks(false));
    loadMoreBtn?.addEventListener('click', () => loadLinks(true));
    listEl?.addEventListener('click', handleListClick);
    signOutBtn?.addEventListener('click', async () => {
        try {
            await Api.logout();
        } finally {
            State.clearToken();
            window.location.replace('/login.html');
        }
    });
}

function showError(message) {
    if (alertEl) {
        alertEl.textContent = message;
        alertEl.classList.remove('d-none');
    }
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

function setLoading(isLoading, isAppend) {
    loading = isLoading;
    refreshSpinner?.classList.toggle('d-none', !(isLoading && !isAppend));
    loadMoreSpinner?.classList.toggle('d-none', !(isAppend && isLoading));
    if (loadMoreBtn) loadMoreBtn.disabled = isLoading;
    if (loadMoreLabel && isAppend) loadMoreLabel.textContent = isLoading ? 'Loading...' : 'Load more';
}

function buildRow(link) {
    const status = link.revoked
        ? '<span class="badge bg-secondary-subtle text-secondary">Revoked</span>'
        : link.expired
            ? '<span class="badge bg-warning-subtle text-warning">Expired</span>'
            : '<span class="badge bg-success-subtle text-success">Active</span>';
    const expiresLabel = link.expiresAt ? `Expires ${escapeHtml(formatDate(link.expiresAt) || '')}` : 'No expiry';
    const noteTitle = link.noteTitle || '—';
    const noteOwner = link.noteOwner || '—';
    const tokenShort = link.token ? `${escapeHtml(link.token.substring(0, 6))}…${escapeHtml(link.token.slice(-4))}` : `#${link.id}`;
    const linkUrl = link.token ? `${window.location.origin}/share/${encodeURIComponent(link.token)}` : '';
    const copyDisabled = !linkUrl || link.revoked;
    const revokeDisabled = link.revoked;
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
            <div class="text-muted small d-flex flex-wrap gap-3">
                <span><i class="fa-regular fa-note-sticky me-1"></i>${escapeHtml(noteTitle)}</span>
                <span><i class="fa-solid fa-user me-1"></i>${escapeHtml(noteOwner)}</span>
                <span><i class="fa-regular fa-calendar me-1"></i>${escapeHtml(formatDate(link.createdDate) || '')}</span>
                <span><i class="fa-regular fa-clock me-1"></i>${expiresLabel}</span>
                <span><i class="fa-solid fa-chart-simple me-1"></i>Used ${link.useCount || 0}</span>
            </div>
        </div>
    `;
}

async function loadLinks(append = false) {
    if (loading) return;
    const nextPage = append ? page + 1 : 0;
    setLoading(true, append);
    const res = await Api.fetchAllShareLinksAdmin(nextPage, PAGE_SIZE).catch(() => null);
    setLoading(false, append);
    if (!res) return;
    const content = Array.isArray(res) ? res : (res.content ?? []);
    const meta = res.page ?? res;
    page = nextPage;
    hasMore = !!meta && typeof meta.totalPages === 'number' ? page + 1 < meta.totalPages : content.length === PAGE_SIZE;
    if (!append && listEl) {
        listEl.innerHTML = '';
    }
    if (emptyEl) {
        const existing = listEl?.children.length || 0;
        emptyEl.classList.toggle('d-none', existing + content.length > 0);
    }
    content.forEach(link => {
        listEl?.insertAdjacentHTML('beforeend', buildRow(link));
    });
    if (loadMoreBtn) {
        loadMoreBtn.classList.toggle('d-none', !hasMore);
        loadMoreBtn.disabled = !hasMore;
    }
    if (loadMoreLabel) loadMoreLabel.textContent = 'Load more';
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
        const ok = await Api.revokeShareLink(id).catch(() => null);
        btn.disabled = false;
        if (ok !== null) {
            showToast('Share link revoked', 'success');
            page = 0;
            loadLinks(false);
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
