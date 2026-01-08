import Api from './api.js';
import Theme from './theme.js';
import State from './state.js';
import Helpers from './helpers.js';

const {escapeHtml, formatDate, showToast, debounce} = Helpers;

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
const statusSelect = document.getElementById('sharedLinksStatus');
const dateFilterSelect = document.getElementById('sharedLinksDateFilter');
const customRange = document.getElementById('sharedLinksCustomRange');
const createdFromInput = document.getElementById('sharedLinksCreatedFrom');
const createdToInput = document.getElementById('sharedLinksCreatedTo');
const customDateModalEl = document.getElementById('customDateModal');
const customDateSave = document.getElementById('customDateSave');
const customDateCancel = document.getElementById('customDateCancel');
const customDateError = document.getElementById('customDateError');
const bootstrapModal = globalThis.bootstrap?.Modal;
const customDateModal = customDateModalEl && bootstrapModal ? new bootstrapModal(customDateModalEl) : null;
let customModalOpen = false;
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
const searchInput = document.getElementById('sharedLinksSearch');
const searchClear = document.getElementById('sharedLinksSearchClear');

totalLabel?.classList.add('d-none');
pageInfo?.classList.add('d-none');

let page = 0;
let totalPages = 1;
let loading = false;
let pageSize = 10;
let sort = 'createdDate,desc';
let search = '';
let status = 'all';
let isAdmin = false;
let dateFilter = 'none';
let createdFrom = '';
let createdTo = '';
let lastDateFilter = 'none';

function getErrorMessage(error, fallback = 'Request failed') {
    if (error?.body?.detail) return error.body.detail;
    if (error?.body?.title) return error.body.title;
    if (error?.message) return error.message;
    return fallback;
}

async function handleApi(promise, options = {}) {
    const {fallback = 'Request failed', onError, onFinally, silent} = options;
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
    Theme.init({button: themeToggle, icon: themeToggleIcon, label: themeToggleLabel});
    initAuth().then((admin) => {
        isAdmin = admin;
        scopeGroup?.classList.add('d-none');
        if (sharedLinksTitle) sharedLinksTitle.textContent = admin ? 'Shared Links' : 'My Shared Links';
        if (sharedLinksSubtitle) sharedLinksSubtitle.textContent = 'See every link you’ve issued, check status, and revoke when needed.';
        if (pageSizeSelect) {
            const val = Number.parseInt(pageSizeSelect.value, 10);
            pageSize = Number.isNaN(val) ? pageSize : val;
        }
        if (sortSelect) {
            sort = sortSelect.value || sort;
        }
        if (statusSelect) {
            status = statusSelect.value || status;
        }
        if (dateFilterSelect) {
            dateFilter = dateFilterSelect.value || dateFilter;
            lastDateFilter = dateFilter;
        }
        applyDatePreset(dateFilter);
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
        const target = Number.parseInt(link.dataset.page, 10);
        if (Number.isNaN(target) || target === page || target < 0 || target >= totalPages || loading) return;
        loadLinks(target);
    });
    if (searchInput) {
        const debounced = debounce((val) => {
            search = (val || '').trim();
            page = 0;
            loadLinks(0);
        }, 300);
        searchInput.addEventListener('input', (e) => debounced(e.target.value || ''));
    }
    searchClear?.addEventListener('click', () => {
        if (!searchInput) return;
        searchInput.value = '';
        search = '';
        page = 0;
        loadLinks(0);
    });
    pageSizeSelect?.addEventListener('change', () => {
        const val = Number.parseInt(pageSizeSelect.value, 10);
        pageSize = Number.isNaN(val) ? 10 : val;
        page = 0;
        loadLinks(0);
    });
    sortSelect?.addEventListener('change', () => {
        sort = sortSelect.value || 'createdDate,desc';
        page = 0;
        loadLinks(0);
    });
    statusSelect?.addEventListener('change', () => {
        status = statusSelect.value || 'all';
        page = 0;
        loadLinks(0);
    });
    dateFilterSelect?.addEventListener('change', () => {
        const newVal = dateFilterSelect.value || 'none';
        if (newVal === 'custom') {
            dateFilterSelect.value = lastDateFilter;
            openCustomDateModal();
            return;
        }
        dateFilter = newVal;
        lastDateFilter = newVal;
        applyDatePreset(newVal);
        page = 0;
        loadLinks(0);
    });
    customDateSave?.addEventListener('click', () => {
        const {valid, fromVal, toVal} = validateCustomInputs(true);
        if (!valid) return;
        createdFrom = fromVal || '';
        createdTo = toVal || '';
        dateFilter = 'custom';
        lastDateFilter = 'custom';
        if (dateFilterSelect) dateFilterSelect.value = 'custom';
        customDateModal?.hide();
        page = 0;
        loadLinks(0);
    });
    customDateCancel?.addEventListener('click', () => {
        if (dateFilterSelect) {
            dateFilterSelect.value = lastDateFilter;
        }
        hideCustomDateError();
    });
    const liveValidate = () => validateCustomInputs(dateFilter === 'custom' || customModalOpen);
    createdFromInput?.addEventListener('input', liveValidate);
    createdToInput?.addEventListener('input', liveValidate);
    if (customDateModalEl) {
        customDateModalEl.addEventListener('shown.bs.modal', () => {
            customModalOpen = true;
            hideCustomDateError();
        });
        customDateModalEl.addEventListener('hidden.bs.modal', () => {
            customModalOpen = false;
            hideCustomDateError();
        });
    }
    signOutBtn?.addEventListener('click', async () => {
        try {
            await Api.logout();
        } finally {
            State.clearToken();
            globalThis.location.replace('/login.html');
        }
    });
}

function hideAlert() {
    alertEl?.classList.add('d-none');
}

function toIsoString(value) {
    if (!value) return '';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    return date.toISOString();
}

function addDays(date, days) {
    const d = new Date(date);
    d.setDate(d.getDate() + days);
    return d;
}

function addHours(date, hours) {
    const d = new Date(date);
    d.setHours(d.getHours() + hours);
    return d;
}

function openCustomDateModal() {
    if (customDateModal) {
        hideCustomDateError();
        if (createdFromInput) createdFromInput.classList.remove('is-invalid');
        if (createdToInput) createdToInput.classList.remove('is-invalid');
        if (customDateError) customDateError.classList.add('d-none');
        customDateModal.show();
    } else if (customRange) {
        customRange.classList.remove('d-none');
    }
}

function showCustomDateError(msg) {
    if (customDateError) {
        customDateError.textContent = msg;
        customDateError.classList.remove('d-none');
    }
    if (customModalOpen || dateFilter === 'custom') {
        createdFromInput?.classList.add('is-invalid');
        createdToInput?.classList.add('is-invalid');
    }
}

function hideCustomDateError() {
    if (customDateError) {
        customDateError.classList.add('d-none');
        customDateError.textContent = '';
    }
    createdFromInput?.classList.remove('is-invalid');
    createdToInput?.classList.remove('is-invalid');
}

function validateCustomInputs(requireAny) {
    const fromVal = toIsoString(createdFromInput?.value);
    const toVal = toIsoString(createdToInput?.value);
    const mustRequire = requireAny || customModalOpen;
    if (mustRequire) {
        if (!fromVal || !toVal) {
            showCustomDateError('Start and end dates are both required.');
            return {valid: false, fromVal, toVal};
        }
        if (new Date(fromVal) > new Date(toVal)) {
            showCustomDateError('Start date cannot be after end date.');
            return {valid: false, fromVal, toVal};
        }
    } else if (!fromVal && !toVal) {
        hideCustomDateError();
        return {valid: true, fromVal, toVal};
    } else if (fromVal && toVal && new Date(fromVal) > new Date(toVal)) {
        showCustomDateError('Start date cannot be after end date.');
        return {valid: false, fromVal, toVal};
    }
    hideCustomDateError();
    return {valid: true, fromVal, toVal};
}

function applyDatePreset(val) {
    const now = new Date();
    createdFrom = '';
    createdTo = '';
    if (createdFromInput) createdFromInput.value = '';
    if (createdToInput) createdToInput.value = '';
    switch (val) {
        case 'created_last_24h':
            createdFrom = toIsoString(addHours(now, -24));
            createdTo = toIsoString(now);
            break;
        case 'created_last_7d':
            createdFrom = toIsoString(addDays(now, -7));
            createdTo = toIsoString(now);
            break;
        case 'created_last_month':
            createdFrom = toIsoString(addDays(now, -30));
            createdTo = toIsoString(now);
            break;
        default:
            break;
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
        console.warn('Failed to load current user for shared links', e);
        updateAuthUi('');
        globalThis.location.replace('/login.html');
        return false;
    }
}

function updateAuthUi(username) {
    const signedIn = !!username;
    authBtn?.classList.toggle('d-none', !signedIn);
    authBtn?.classList.toggle('dropdown-toggle', signedIn);
    if (signedIn) {
        if (authBtn) authBtn.dataset.bsToggle = 'dropdown';
        if (authBtnLabel) authBtnLabel.textContent = username;
    } else {
        if (authBtn) delete authBtn.dataset.bsToggle;
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
    let status = '<span class="badge bg-success-subtle text-success">Active</span>';
    if (link.revoked) {
        status = '<span class="badge bg-secondary-subtle text-secondary">Revoked</span>';
    } else if (link.expired) {
        status = '<span class="badge bg-warning-subtle text-warning">Expired</span>';
    }
    const noteTitle = link.noteTitle || '—';
    const noteOwner = link.noteOwner || '—';
    const tokenShort = link.token ? `${escapeHtml(link.token.substring(0, 6))}…${escapeHtml(link.token.slice(-4))}` : `#${link.id}`;
    const linkUrl = link.token ? `${globalThis.location.origin}/share/${encodeURIComponent(link.token)}` : '';
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
    const res = await handleApi(
        Api.fetchMyShareLinks(sort, search, status, createdFrom, createdTo, targetPage, pageSize),
        {
            fallback: 'Could not load shared links.',
            onFinally: () => setLoading(false)
        });
    if (!res) return;
    hideAlert();

    const {content, meta, totalElements} = normalizeLinksResponse(res, targetPage);
    renderLinksList(content);
    renderLinksTotals(totalElements, content.length > 0);
    renderLinksPagination(meta, content.length > 0);
}

function normalizeLinksResponse(res, targetPage) {
    const content = Array.isArray(res) ? res : (res.content ?? []);
    const meta = res.page ?? res;
    page = typeof meta?.number === 'number' ? meta.number : targetPage;
    const derivedTotalPages = typeof meta?.totalPages === 'number'
        ? Math.max(1, meta.totalPages)
        : Math.max(1, page + 1);
    totalPages = derivedTotalPages;
    const totalElements = typeof meta?.totalElements === 'number' ? meta.totalElements : content.length;
    return {content, meta, totalElements};
}

function renderLinksList(content) {
    if (listEl) {
        listEl.innerHTML = '';
        if (emptyEl) listEl.appendChild(emptyEl);
        if (loadingRow) listEl.appendChild(loadingRow);
    }
    const hasItems = content.length > 0;
    emptyEl?.classList.toggle('d-none', hasItems);
    loadingRow?.classList.add('d-none');
    content.forEach(link => {
        listEl?.insertAdjacentHTML('beforeend', buildRow(link));
    });
}

function renderLinksTotals(totalElements, hasItems) {
    if (!totalLabel) return;
    totalLabel.textContent = `Total: ${totalElements}`;
    totalLabel.classList.toggle('d-none', !hasItems);
}

function renderLinksPagination(meta, hasItems) {
    if (pagination) {
        pagination.innerHTML = hasItems ? buildPaginationItems(page, totalPages) : '';
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

function buildPaginationItems(currentPage, totalPagesCount) {
    const items = [];
    items.push(`<li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
            <a class="page-link" href="#" data-page="${currentPage - 1}" aria-label="Previous">&laquo;</a>
        </li>`);
    for (let i = 0; i < totalPagesCount; i++) {
        items.push(`<li class="page-item ${i === currentPage ? 'active' : ''}">
                <a class="page-link" href="#" data-page="${i}">${i + 1}</a>
            </li>`);
    }
    items.push(`<li class="page-item ${currentPage >= totalPagesCount - 1 ? 'disabled' : ''}">
            <a class="page-link" href="#" data-page="${currentPage + 1}" aria-label="Next">&raquo;</a>
        </li>`);
    return items.join('');
}

async function handleListClick(event) {
    const btn = event.target.closest('button[data-action]');
    if (!btn) return;
    const action = btn.dataset.action;
    if (action === 'copy') {
        const token = btn.dataset.token;
        if (!token) return;
        const url = `${globalThis.location.origin}/share/${encodeURIComponent(token)}`;
        try {
            await navigator.clipboard.writeText(url);
            inlineCopied(btn);
        } catch (err) {
            console.warn('Copy to clipboard failed', err);
            showToast('Could not copy link. Copy manually.', 'warning');
        }
        return;
    }
    if (action === 'revoke') {
        const id = btn.dataset.id;
        if (!id) return;
        btn.disabled = true;
        const res = await handleApi(Api.revokeShareLink(id), {
            fallback: 'Could not revoke link.',
            onFinally: () => {
                btn.disabled = false;
            },
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
