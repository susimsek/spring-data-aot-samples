import Helpers from './helpers.js';
import State from './state.js';
import Api from './api.js';
import Render from './render.js';
import Ui from './ui.js';
import Validation from './validation.js';
import Diff from './diff.js';
import Theme from './theme.js';

const {state, currentAuditor, clearToken, currentUsername, setCurrentUser, isAdmin} = State;
const {escapeHtml, formatDate, showToast, debounce} = Helpers;
const {renderTags, revisionTypeBadge} = Render;
const {toggleSizeMessages, toggleInlineMessages} = Validation;
const {diffLines, diffLinesDetailed} = Diff;

// Use state from module
const BULK_LIMIT = 100;
const TAG_LIMIT = 5;
const FILTER_TAG_LIMIT = 5;
const noteGrid = document.getElementById('noteGrid');
const totalLabel = document.getElementById('totalLabel');
const pageInfo = document.getElementById('pageInfo');
const pageSize = document.getElementById('pageSize');
const sortSelect = document.getElementById('sortSelect');
const pagination = document.getElementById('pagination');
const pager = document.getElementById('pager');
const auditorInput = document.getElementById('auditorInput');
const addNoteBtn = document.getElementById('addNoteBtn');
const activeViewBtn = document.getElementById('activeViewTab');
const trashViewBtn = document.getElementById('trashViewTab');
const noteModal = bootstrap.Modal.getOrCreateInstance(document.getElementById('noteModal'));
const deleteModal = bootstrap.Modal.getOrCreateInstance(document.getElementById('deleteModal'));
const emptyTrashModal = bootstrap.Modal.getOrCreateInstance(document.getElementById('emptyTrashModal'));
const deleteForeverModal = bootstrap.Modal.getOrCreateInstance(document.getElementById('deleteForeverModal'));
const bulkModalEl = document.getElementById('bulkModal');
const bulkModal = bootstrap.Modal.getOrCreateInstance(bulkModalEl);
const noteForm = document.getElementById('noteForm');
const titleInput = document.getElementById('title');
const contentInput = document.getElementById('content');
const pinnedInput = document.getElementById('pinned');
const colorInput = document.getElementById('color');
const tagsInput = document.getElementById('tagsInput');
const tagsContainer = document.getElementById('tagsContainer');
const tagsListEl = document.getElementById('tagsList');
const tagsLimitMsg = document.getElementById('tagsLimitMessage');
const tagSuggestions = document.getElementById('tagSuggestions');
const filterTagsInput = document.getElementById('filterTagsInput');
const filterTagsContainer = document.getElementById('filterTagsContainer');
const filterTagsList = document.getElementById('filterTagsList');
const filterTagsError = document.getElementById('filterTagsError');
const filterTagsSuggestions = document.getElementById('filterTagsSuggestions');
const filterColorInput = document.getElementById('filterColor');
const clearColorFilterBtn = document.getElementById('clearColorFilter');
const filterPinnedSelect = document.getElementById('filterPinned');
const resetFiltersBtn = document.getElementById('resetFilters');
const applyFiltersBtn = document.getElementById('applyFilters');
const authBtn = document.getElementById('authBtn');
const authBtnLabel = document.getElementById('authBtnLabel');
const authUserLabel = document.getElementById('authUserLabel');
const authMenu = document.getElementById('authMenu');
const navbarSharedLinks = document.getElementById('navbarSharedLinks');
const signOutBtn = document.getElementById('signOutBtn');
const signOutDivider = document.getElementById('signOutDivider');
const TAG_PATTERN = /^[A-Za-z0-9_-]{1,30}$/;
const TAG_FORMAT_MESSAGE = 'Tags must be 1-30 characters using letters, digits, hyphen, or underscore.';
let currentTags = new Set();
let tagsDirty = false;
const inlineTagsState = new Map();
const inlineTagsDirty = new Map();
const submitLabel = document.getElementById('noteSubmitLabel');
const saveBtn = document.querySelector('#noteForm button[type="submit"]');
const saveSpinner = document.getElementById('noteSubmitSpinner');
const formAlert = document.getElementById('formAlert');
const toastContainer = document.getElementById('toastContainer');
const searchInput = document.getElementById('searchInput');
const searchClear = document.getElementById('searchClear');
const emptyTrashBtn = document.getElementById('emptyTrashBtn');
const emptyTrashSpinner = document.getElementById('emptyTrashSpinner');
const emptyTrashLabel = document.getElementById('emptyTrashLabel');
const confirmEmptyTrashBtn = document.getElementById('confirmEmptyTrashBtn');
const emptyTrashConfirmSpinner = document.getElementById('emptyTrashConfirmSpinner');
const emptyTrashConfirmLabel = document.getElementById('emptyTrashConfirmLabel');
const confirmDeleteForeverBtn = document.getElementById('confirmDeleteForeverBtn');
const deleteForeverSpinner = document.getElementById('deleteForeverSpinner');
const deleteForeverLabel = document.getElementById('deleteForeverLabel');
const selectAllCheckbox = document.getElementById('selectAllCheckbox');
const bulkDeleteBtn = document.getElementById('bulkDeleteBtn');
const bulkRestoreBtn = document.getElementById('bulkRestoreBtn');
const bulkDeleteForeverBtn = document.getElementById('bulkDeleteForeverBtn');
const bulkModalMessage = document.getElementById('bulkModalMessage');
const confirmBulkBtn = document.getElementById('confirmBulkBtn');
const bulkSpinner = document.getElementById('bulkSpinner');
const bulkConfirmLabel = document.getElementById('bulkConfirmLabel');
const selectedCount = document.getElementById('selectedCount');
const bulkRow = document.getElementById('bulkRow');
const controlsRow = document.getElementById('controlsRow');
const revisionModalEl = document.getElementById('revisionModal');
const revisionModal = revisionModalEl ? bootstrap.Modal.getOrCreateInstance(revisionModalEl) : null;
const revisionModalBody = document.getElementById('revisionModalBody');
const revisionList = document.getElementById('revisionList');
const revisionSpinner = document.getElementById('revisionSpinner');
const revisionError = document.getElementById('revisionError');
const revisionModalTitle = document.getElementById('revisionModalTitle');
const ownerModalEl = document.getElementById('ownerModal');
const ownerModal = ownerModalEl ? bootstrap.Modal.getOrCreateInstance(ownerModalEl) : null;
const ownerForm = document.getElementById('ownerForm');
const ownerInput = document.getElementById('ownerInput');
const ownerNoteTitleText = document.getElementById('ownerNoteTitleText');
const ownerAlert = document.getElementById('ownerAlert');
const ownerSuggestions = document.getElementById('ownerSuggestions');
const ownerSubmit = document.getElementById('ownerSubmit');
const ownerSubmitSpinner = document.getElementById('ownerSubmitSpinner');
const ownerCurrentLabel = document.getElementById('ownerCurrentLabel');
const ownerLoadMoreBtn = document.getElementById('ownerLoadMore');
const OWNER_SEARCH_PAGE_SIZE = 5;
let ownerSearchPage = 0;
let ownerSearchHasMore = false;
let ownerSearchQuery = '';
let ownerSearchLoading = false;
const themeToggle = document.getElementById('themeToggle');
const themeToggleIcon = document.getElementById('themeToggleIcon');
const themeToggleLabel = document.getElementById('themeToggleLabel');
let revisionCache = [];
let revisionPageSize = 5;
let revisionPage = 0;
let revisionHasMore = false;
let isLoadingRevisions = false;
let revisionNoteId = null;
let revisionTotal = 0;
const shareModalEl = document.getElementById('shareModal');
let shareModal = null;
const shareNoteTitle = document.getElementById('shareNoteTitle');
const shareForm = document.getElementById('shareForm');
const shareExpirySelect = document.getElementById('shareExpirySelect');
const shareExpiresAt = document.getElementById('shareExpiresAt');
const shareOneTime = document.getElementById('shareOneTime');
const shareAlert = document.getElementById('shareAlert');
const shareSubmitBtn = document.getElementById('shareSubmitBtn');
const shareSubmitSpinner = document.getElementById('shareSubmitSpinner');
const shareSubmitLabel = document.getElementById('shareSubmitLabel');
const shareResult = document.getElementById('shareResult');
const shareLink = document.getElementById('shareLink');
const copyShareLinkBtn = document.getElementById('copyShareLink');
const sharePermissionBadge = document.getElementById('sharePermissionBadge');
const shareExpiryLabel = document.getElementById('shareExpiryLabel');
const shareOneTimeBadge = document.getElementById('shareOneTimeBadge');
const shareLinksList = document.getElementById('shareLinksList');
const shareLinksEmpty = document.getElementById('shareLinksEmpty');
const shareLinksSpinner = document.getElementById('shareLinksSpinner');
const refreshShareLinksBtn = document.getElementById('refreshShareLinks');
const shareLinksLoadMoreBtn = document.getElementById('shareLinksLoadMore');
const shareLinksLoadMoreSpinner = document.getElementById('shareLinksLoadMoreSpinner');
const shareLinksLoadMoreLabel = document.getElementById('shareLinksLoadMoreLabel');
const shareLinksSection = document.getElementById('shareLinksSection');
const copyShareLabelDefault = 'Copy';
const copyShareLabelCopied = 'Copied';
let shareNoteId = null;
let shareLinksCache = [];
let shareLinksPage = 0;
let shareLinksHasMore = false;
let shareLinksLoading = false;
const SHARE_LINKS_PAGE_SIZE = 3;
const shareLinksHeading = document.getElementById('shareLinksHeading');

function resetRevisionState() {
    revisionCache = [];
    revisionPage = 0;
    revisionHasMore = false;
    revisionTotal = 0;
    isLoadingRevisions = false;
    revisionNoteId = null;
}

function resetShareModal() {
    if (shareAlert) shareAlert.classList.add('d-none');
    if (shareForm) {
        shareForm.reset();
        shareForm.classList.remove('d-none');
    }
    if (shareExpirySelect) shareExpirySelect.value = '24h';
    if (shareExpiresAt) shareExpiresAt.value = '';
    if (shareExpiresAt) shareExpiresAt.classList.add('d-none');
    if (shareOneTime) shareOneTime.checked = false;
    if (shareResult) shareResult.classList.add('d-none');
    if (shareLink) shareLink.value = '';
    shareLinksCache = [];
    if (shareLinksList) shareLinksList.innerHTML = '';
    if (shareLinksEmpty) shareLinksEmpty.classList.remove('d-none');
    if (shareLinksSpinner) shareLinksSpinner.classList.add('d-none');
    if (refreshShareLinksBtn) refreshShareLinksBtn.disabled = false;
    if (shareLinksLoadMoreBtn) {
        shareLinksLoadMoreBtn.classList.add('d-none');
        shareLinksLoadMoreBtn.disabled = false;
    }
    if (shareLinksLoadMoreSpinner) {
        shareLinksLoadMoreSpinner.classList.add('d-none');
    }
    shareLinksPage = 0;
    shareLinksHasMore = false;
    shareLinksLoading = false;
    shareNoteId = null;
    if (shareLinksSection) {
        shareLinksSection.classList.add('d-none');
    }
}

function setShareLoading(loading) {
    if (!shareSubmitBtn) return;
    shareSubmitBtn.disabled = loading;
    if (shareSubmitSpinner) {
        shareSubmitSpinner.classList.toggle('d-none', !loading);
    }
    if (shareSubmitLabel) {
        shareSubmitLabel.textContent = loading ? 'Creating...' : 'Create link';
    }
}

function setShareLinksLoading(loading) {
    shareLinksLoading = loading;
    if (shareLinksSpinner) {
        shareLinksSpinner.classList.toggle('d-none', !loading);
    }
    if (refreshShareLinksBtn) {
        refreshShareLinksBtn.disabled = loading;
    }
    if (!loading && shareLinksLoadMoreBtn) {
        shareLinksLoadMoreBtn.disabled = false;
    }
    if (shareLinksLoadMoreSpinner) {
        shareLinksLoadMoreSpinner.classList.add('d-none');
    }
}

function buildShareLinkCard(link) {
    const status = link.revoked
        ? '<span class="badge bg-secondary-subtle text-secondary border">Revoked</span>'
        : link.expired
            ? '<span class="badge bg-warning-subtle text-warning border">Expired</span>'
            : '<span class="badge bg-success-subtle text-success border">Active</span>';
    const expiresLabel = link.expiresAt
        ? escapeHtml(formatDate(link.expiresAt) || '')
        : 'No expiry';
    const createdLabel = escapeHtml(formatDate(link.createdDate) || '');
    const lastUsedLabel = link.lastUsedAt ? escapeHtml(formatDate(link.lastUsedAt) || '') : 'Not used yet';
    const tokenDisplay = link.token
        ? `${escapeHtml(link.token.substring(0, 6))}…${escapeHtml(link.token.slice(-4))}`
        : `#${link.id}`;
    const oneTimeBadge = link.oneTime
        ? '<span class="badge bg-secondary-subtle text-secondary border">One-time</span>'
        : '';
    const linkUrl = link.token ? `${window.location.origin}/share/${encodeURIComponent(link.token)}` : '';
    const copyDisabled = !linkUrl || link.revoked;
    const revokeDisabled = link.revoked;

    return `
        <div class="list-group-item py-3" id="share-item-${link.id}">
            <div class="d-flex flex-column flex-md-row justify-content-between gap-3">
                <div class="flex-grow-1">
                    <div class="d-flex align-items-center gap-2 flex-wrap mb-1">
                        <span class="badge bg-primary-subtle text-primary border border-primary-subtle">${tokenDisplay}</span>
                        ${status}
                        ${oneTimeBadge}
                    </div>
                    <div class="fw-semibold">${escapeHtml(link.noteTitle || '')}</div>
                    <div class="text-muted small d-flex gap-3 flex-wrap mt-1">
                        <span><i class="fa-solid fa-user me-1"></i>${escapeHtml(link.noteOwner || '—')}</span>
                        <span><i class="fa-solid fa-chart-column me-1"></i>Used ${link.useCount || 0}</span>
                    </div>
                    <div class="text-muted small d-flex gap-3 flex-wrap mt-1">
                        <span><i class="fa-solid fa-clock-rotate-left me-1"></i>Last used: ${lastUsedLabel || '—'}</span>
                    </div>
                    <div class="text-muted small d-flex gap-3 flex-wrap mt-1">
                        <span><i class="fa-regular fa-calendar me-1"></i>Created: ${createdLabel || '—'}</span>
                    </div>
                    <div class="text-muted small d-flex gap-3 flex-wrap mt-1">
                        <span><i class="fa-regular fa-calendar-xmark me-1"></i>Expires: ${expiresLabel}</span>
                    </div>
                </div>
                <div class="d-flex flex-column align-items-end gap-2">
                    <button class="btn btn-sm btn-outline-secondary d-inline-flex align-items-center gap-2"
                            data-share-action="copy" data-token="${escapeHtml(link.token || '')}" ${copyDisabled ? 'disabled' : ''}>
                        <i class="fa-solid fa-copy"></i> Copy
                    </button>
                    <button class="btn btn-sm btn-outline-danger d-inline-flex align-items-center gap-2"
                            data-share-action="revoke" data-id="${link.id}" ${revokeDisabled ? 'disabled' : ''}>
                        <i class="fa-solid fa-trash"></i> Revoke
                    </button>
                </div>
            </div>
        </div>
        `;
}

function renderShareLinks(links, append = false) {
    if (!shareLinksList) return;
    const data = Array.isArray(links) ? links : [];
    if (!append) {
        shareLinksList.innerHTML = '';
    }
    if (shareLinksEmpty) {
        const existing = shareLinksList.children.length;
        shareLinksEmpty.classList.toggle('d-none', existing + data.length > 0);
    }
    data.forEach(link => {
        shareLinksList.insertAdjacentHTML('beforeend', buildShareLinkCard(link));
    });
}

async function loadShareLinks(append = false) {
    if (!shareNoteId || shareLinksLoading) return;
    const page = append ? shareLinksPage + 1 : 0;
    setShareLinksLoading(true);
    toggleLoadMoreLoading(append, true);
    const res = await handleApi(Api.fetchShareLinks(shareNoteId, page, SHARE_LINKS_PAGE_SIZE), {
        fallback: 'Could not load share links',
        silent: true,
        onFinally: () => setShareLinksLoading(false)
    });
    toggleLoadMoreLoading(append, false);
    if (!res) return;
    const {content, meta} = normalizeShareLinksResponse(res);
    shareLinksPage = page;
    shareLinksHasMore = computeHasMore(meta, content.length, shareLinksPage);
    updateShareLinksCache(content, append);
    renderShareLinks(content, append);
    updateLoadMoreControls();
}

function toggleLoadMoreLoading(append, loading) {
    if (!append || !shareLinksLoadMoreSpinner) return;
    shareLinksLoadMoreSpinner.classList.toggle('d-none', !loading);
    if (shareLinksLoadMoreLabel) {
        shareLinksLoadMoreLabel.textContent = loading ? 'Loading...' : 'Load more';
    }
}

function normalizeShareLinksResponse(res) {
    return {
        content: Array.isArray(res) ? res : (res.content ?? []),
        meta: res.page ?? res
    };
}

function computeHasMore(meta, contentLength, currentPage) {
    if (meta && typeof meta.totalPages === 'number') {
        return currentPage + 1 < meta.totalPages;
    }
    return contentLength === SHARE_LINKS_PAGE_SIZE;
}

function updateShareLinksCache(content, append) {
    shareLinksCache = append ? [...shareLinksCache, ...content] : content;
}

function updateLoadMoreControls() {
    if (!shareLinksLoadMoreBtn) return;
    shareLinksLoadMoreBtn.classList.toggle('d-none', !shareLinksHasMore);
    shareLinksLoadMoreBtn.disabled = !shareLinksHasMore;
    if (shareLinksLoadMoreLabel) shareLinksLoadMoreLabel.textContent = 'Load more';
}

async function handleShareLinksClick(event) {
    const btn = event.target.closest('[data-share-action]');
    if (!btn) return;
    const action = btn.dataset.shareAction;
    if (action === 'copy') {
        const token = btn.dataset.token;
        if (!token) return;
        const link = `${window.location.origin}/share/${encodeURIComponent(token)}`;
        try {
            await navigator.clipboard.writeText(link);
            showInlineCopied(btn);
        } catch (err) {
            fallbackCopyText(link);
            showInlineCopied(btn);
        }
        return;
    }
    if (action === 'revoke') {
        const id = btn.dataset.id;
        if (!id) return;
        btn.disabled = true;
        const success = await handleApi(Api.revokeShareLink(id), {
            fallback: 'Could not revoke link',
            onFinally: () => {
                btn.disabled = false;
            }
        });
        if (success !== null) {
            showToast('Share link revoked', 'success');
            loadShareLinks();
        }
    }
}

const noteCache = new Map();
const defaultSort = 'createdDate,desc';
state.sort = defaultSort;
state.selected = new Set();
let deleteForeverId = null;
let ownerNoteId = null;

if (addNoteBtn) {
    addNoteBtn.disabled = state.view === 'trash';
}

Theme.init({button: themeToggle, icon: themeToggleIcon, label: themeToggleLabel});

function redirectToLogin() {
    const path = window.location.pathname || '';
    if (path.includes('/login')) {
        return;
    }
    window.location.replace('/login.html');
}

function setColorFilterActive(active) {
    if (filterColorInput) {
        filterColorInput.dataset.active = active ? 'true' : 'false';
    }
}

function getErrorMessage(error, fallback = 'Request failed') {
    if (error?.body?.detail) return error.body.detail;
    if (error?.body?.title) return error.body.title;
    if (error?.message) return error.message;
    return fallback;
}

function extractViolations(error) {
    return error?.body?.violations || [];
}

function isAuthenticated() {
    return !!state.currentUser;
}

function updateAuthUi() {
    const username = currentUsername();
    const signedIn = isAuthenticated();
    toggleSignOutControls(signedIn);
    updateAuthButton(signedIn, username);
    updateUserLabel(signedIn, username);
    toggleElement(authMenu, signedIn);
    toggleElement(signOutDivider, signedIn);
    toggleElement(navbarSharedLinks, signedIn);
}

function toggleSignOutControls(signedIn) {
    toggleElement(signOutBtn, signedIn);
}

function updateAuthButton(signedIn, username) {
    if (!authBtn) return;
    authBtn.classList.toggle('d-none', !signedIn);
    authBtn.classList.toggle('dropdown-toggle', signedIn);
    if (signedIn) {
            authBtn.dataset.bsToggle = 'dropdown';
        if (authBtnLabel) {
            authBtnLabel.textContent = username || 'User';
        }
        return;
    }
            delete authBtn.dataset.bsToggle;
    if (authBtnLabel) {
        authBtnLabel.textContent = '';
    }
}

function updateUserLabel(signedIn, username) {
    if (!authUserLabel) return;
    authUserLabel.classList.toggle('d-none', !signedIn);
    authUserLabel.textContent = signedIn ? `Signed in as ${username || ''}` : '';
}

function toggleElement(el, show) {
    if (el) {
        el.classList.toggle('d-none', !show);
    }
}

function handleLogout(event) {
    event?.preventDefault();
    handleApi(Api.logout(), {silent: true})
        .finally(() => {
            clearToken();
            updateAuthUi();
            showToast('Signed out', 'info');
            window.location.href = '/login.html';
        });
}

async function bootstrapAuth() {
    const username = currentUsername();
    if (!username) {
        clearToken();
        updateAuthUi();
        redirectToLogin();
        return false;
    }
    updateAuthUi();
    return true;
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

function clearOwnerModal() {
    ownerAlert?.classList.add('d-none');
    if (ownerSuggestions) ownerSuggestions.innerHTML = '';
    if (ownerInput) {
        ownerInput.value = '';
        ownerInput.classList.remove('is-invalid');
    }
    if (ownerSubmit) ownerSubmit.disabled = true;
    ownerSubmitSpinner?.classList.add('d-none');
    ownerSearchPage = 0;
    ownerSearchHasMore = false;
    ownerSearchQuery = '';
    ownerSearchLoading = false;
    if (ownerLoadMoreBtn) {
        ownerLoadMoreBtn.classList.add('d-none');
        ownerLoadMoreBtn.disabled = false;
    }
}

function openOwnerModal(noteId) {
    if (!ownerModal || !isAdmin?.()) return;
    ownerNoteId = noteId;
    const note = noteCache.get(noteId);
    if (ownerSuggestions) ownerSuggestions.dataset.currentOwner = note?.owner || '';
    if (ownerNoteTitleText) {
        ownerNoteTitleText.textContent = note?.title
            ? `Change owner for "${note.title}"`
            : 'Change owner';
    }
    if (ownerCurrentLabel) {
        const hasOwner = !!note?.owner;
        ownerCurrentLabel.textContent = hasOwner ? `Current owner: ${note.owner}` : '';
        ownerCurrentLabel.classList.toggle('d-none', !hasOwner);
    }
    clearOwnerModal();
    ownerModal.show();
    ownerInput?.focus();
    if (ownerInput?.value) {
        loadOwnerSuggestions(ownerInput.value);
    }
}

function renderOwnerSuggestions(users, append = false) {
    if (!ownerSuggestions) return;
    const rows = Array.isArray(users) ? users : [];
    const currentOwner = ownerSuggestions?.dataset.currentOwner || '';
    const selectedValue = ownerInput?.value?.trim() || '';
    if (!append) {
        ownerSuggestions.innerHTML = '';
    }
    if (rows.length === 0 && !append) {
        ownerSuggestions.innerHTML = '<div class="text-muted small px-2 py-1">Sonuç yok</div>';
        return;
    }
    ownerSuggestions.insertAdjacentHTML('beforeend', rows.map(user => {
        const username = user.username || '';
        const initial = username ? escapeHtml(username.charAt(0).toUpperCase()) : '?';
        const status = user.enabled === false
            ? '<span class="badge text-bg-secondary text-uppercase small">Disabled</span>'
            : '<span class="badge text-bg-success text-uppercase small">Active</span>';
        const isCurrent = currentOwner && currentOwner === username;
        const isSelected = selectedValue && selectedValue === username;
        return `
                <button type="button" class="list-group-item list-group-item-action d-flex justify-content-between align-items-center owner-item ${isSelected ? 'active' : ''}" data-username="${escapeHtml(username)}">
                    <div class="d-flex align-items-center gap-2">
                        <span class="badge rounded-circle text-bg-secondary fw-semibold" style="width: 28px; height: 28px; display: inline-flex; align-items: center; justify-content: center;">${initial}</span>
                        <span class="fw-semibold">${escapeHtml(username)}</span>
                    </div>
                    <div class="d-flex align-items-center gap-2">
                        ${isCurrent ? '<span class="badge text-bg-secondary text-uppercase small">Current</span>' : ''}
                        ${isSelected && !isCurrent ? '<i class="fa-solid fa-check text-success"></i>' : ''}
                        ${status}
                    </div>
                </button>
            `;
    }).join(''));
}

const debouncedOwnerSearch = debounce((query) => loadOwnerSuggestions(query, false), 250);

async function loadOwnerSuggestions(query, append = false) {
    if (!ownerModal || !isAdmin?.()) return;
    if (ownerSearchLoading) return;
    ownerSearchLoading = true;
    if (append && ownerLoadMoreBtn) {
        ownerLoadMoreBtn.querySelector('[data-owner-load-more-spinner="true"]')?.classList.remove('d-none');
        ownerLoadMoreBtn.disabled = true;
    }
    const page = append ? ownerSearchPage + 1 : 0;
    const result = await handleApi(Api.searchUsers(query, page, OWNER_SEARCH_PAGE_SIZE), {
        fallback: 'Could not search users',
        silent: true
    });
    const content = result?.content ?? result ?? [];
    const meta = result?.page ?? result;
    ownerSearchPage = page;
    const totalPages = meta?.totalPages ?? 1;
    ownerSearchHasMore = ownerSearchPage + 1 < totalPages;
    renderOwnerSuggestions(content, append);
    if (ownerLoadMoreBtn) {
        ownerLoadMoreBtn.classList.toggle('d-none', !ownerSearchHasMore);
        ownerLoadMoreBtn.disabled = !ownerSearchHasMore;
        ownerLoadMoreBtn.querySelector('[data-owner-load-more-spinner="true"]')?.classList.add('d-none');
    }
    ownerSearchLoading = false;
}

function handleOwnerInput() {
    if (!ownerInput) return;
    ownerInput.classList.remove('is-invalid');
    ownerAlert?.classList.add('d-none');
    const val = ownerInput.value?.trim() || '';
    if (ownerSuggestions) ownerSuggestions.dataset.selectedOwner = val;
    ownerSearchQuery = val;
    ownerSearchPage = 0;
    ownerSearchHasMore = false;
    if (ownerSubmit) {
        ownerSubmit.disabled = val.length === 0;
    }
    if (val.length >= 2) {
        debouncedOwnerSearch(val);
    } else {
        if (ownerSuggestions) {
            ownerSuggestions.innerHTML = '';
        }
        if (ownerLoadMoreBtn) {
            ownerLoadMoreBtn.classList.add('d-none');
        }
    }
}

function openShareModal(noteId) {
    const modalInstance = shareModal || (shareModalEl && window.bootstrap?.Modal ? window.bootstrap.Modal.getOrCreateInstance(shareModalEl) : null);
    const useFallback = !modalInstance;
    const id = Number(noteId);
    const note = noteCache.get(id);
    if (!note) {
        showToast('Note not loaded yet, try again.', 'warning');
        return;
    }
    resetShareModal();
    shareNoteId = id;
    if (shareNoteTitle) {
        shareNoteTitle.textContent = note.title
            ? `Sharing "${escapeHtml(note.title)}"`
            : 'Sharing note';
    }
    if (useFallback) {
        showShareModalFallback();
    } else {
        modalInstance.show();
    }
}

function openShareLinksOnly(noteId) {
    const modalInstance = shareModal || (shareModalEl && window.bootstrap?.Modal ? window.bootstrap.Modal.getOrCreateInstance(shareModalEl) : null);
    const useFallback = !modalInstance;
    const id = Number(noteId);
    const note = noteCache.get(id);
    if (!note) {
        showToast('Note not loaded yet, try again.', 'warning');
        return;
    }
    resetShareModal();
    shareNoteId = id;
    if (shareNoteTitle) shareNoteTitle.textContent = '';
    if (shareLinksHeading) {
        shareLinksHeading.textContent = note.title
            ? `Links for "${escapeHtml(note.title)}"`
            : 'Links';
    }
    if (shareForm) shareForm.classList.add('d-none');
    shareLinksSection?.classList.remove('d-none');
    if (useFallback) {
        showShareModalFallback();
    } else {
        modalInstance.show();
    }
    loadShareLinks();
}

function validateShareForm() {
    return !!shareForm;
}

async function submitShare(event) {
    event.preventDefault();
    if (!shareModalEl || !shareNoteId) return;
    if (!validateShareForm()) return;
    shareAlert?.classList.add('d-none');
    setShareLoading(true);
    const expiresAtValue = resolveShareExpiry();
    const noExpiry = shareExpirySelect?.value === 'never';
    if (expiresAtValue === undefined) {
        if (shareAlert) {
            shareAlert.textContent = 'Invalid expiry date.';
            shareAlert.classList.remove('d-none');
        }
        setShareLoading(false);
        return;
    }
    const payload = {
        expiresAt: expiresAtValue,
        noExpiry,
        oneTime: !!shareOneTime?.checked
    };
    const result = await handleApi(Api.createShareLink(shareNoteId, payload), {
        fallback: 'Could not create share link',
        silent: true
    });
    if (!result) {
        setShareLoading(false);
        return;
    }
    const link = `${window.location.origin}/share/${encodeURIComponent(result.token)}`;
    if (shareLink) shareLink.value = link;
    if (sharePermissionBadge) sharePermissionBadge.textContent = `${result.permission || 'READ'} access`;
    if (shareExpiryLabel) {
        if (result.expiresAt) {
            shareExpiryLabel.textContent = `Expires ${formatDate(result.expiresAt)}`;
        } else if (noExpiry) {
            shareExpiryLabel.textContent = 'No expiry';
        } else {
            shareExpiryLabel.textContent = 'Expires in 24h by default';
        }
    }
    if (shareOneTimeBadge) {
        shareOneTimeBadge.classList.toggle('d-none', !result.oneTime);
    }
    shareResult?.classList.remove('d-none');
    setShareLoading(false);
    showToast('Share link created. Copy and send it.', 'success');
    loadShareLinks();
}

async function copyShareLink() {
    if (!shareLink || !shareLink.value) return;
    try {
        await navigator.clipboard.writeText(shareLink.value);
        showCopiedFeedback();
    } catch (err) {
        showToast('Could not copy link. Copy manually.', 'warning');
    }
}

function showCopiedFeedback() {
    if (!copyShareLinkBtn) return;
    const originalHtml = copyShareLinkBtn.innerHTML;
    copyShareLinkBtn.innerHTML = `<i class="fa-solid fa-check"></i> Copied`;
    setTimeout(() => {
        copyShareLinkBtn.innerHTML = originalHtml || `<i class="fa-solid fa-copy"></i> Copy`;
    }, 1500);
}

function showInlineCopied(btn) {
    if (!btn) return;
    const original = btn.innerHTML;
    btn.innerHTML = `<i class="fa-solid fa-check"></i> Copied`;
    btn.classList.add('disabled');
    setTimeout(() => {
        btn.innerHTML = original;
        btn.classList.remove('disabled');
    }, 1200);
}

function fallbackCopyText(text) {
    try {
        const tempInput = document.createElement('textarea');
        tempInput.value = text;
        tempInput.readOnly = true;
        tempInput.style.position = 'absolute';
        tempInput.style.left = '-9999px';
        document.body.appendChild(tempInput);
        tempInput.select();
        document.execCommand('copy');
        document.body.removeChild(tempInput);
    } catch (e) {
        // ignore
    }
}

function handleShareExpiryChange() {
    if (!shareExpirySelect || !shareExpiresAt) return;
    const val = shareExpirySelect.value;
    if (val === 'custom') {
        shareExpiresAt.classList.remove('d-none');
        setCustomExpiryBounds();
    } else {
        shareExpiresAt.classList.add('d-none');
        shareExpiresAt.value = '';
        if (val === 'never' && shareExpiresAt) {
            shareExpiresAt.removeAttribute('min');
            shareExpiresAt.removeAttribute('max');
        }
    }
}

function resolveShareExpiry() {
    const val = shareExpirySelect ? shareExpirySelect.value : '24h';
    if (val === 'custom') {
        const raw = shareExpiresAt?.value?.trim();
        if (!raw) {
            return undefined;
        }
        const parsed = new Date(raw);
        if (Number.isNaN(parsed.getTime())) {
            return undefined;
        }
        return parsed.toISOString();
    }
    if (val === 'never') {
        return null;
    }
    const hours = Number.parseInt(val, 10);
    if (Number.isNaN(hours)) {
        return null;
    }
    const expires = new Date(Date.now() + hours * 60 * 60 * 1000);
    return expires.toISOString();
}

function setCustomExpiryBounds() {
    if (!shareExpiresAt) return;
    const now = new Date();
    const min = new Date(now.getTime() + 5 * 60 * 1000); // at least 5 minutes from now
    const max = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000); // max 30 days
    const toLocal = (date) => {
        const pad = (n) => String(n).padStart(2, '0');
        const yyyy = date.getFullYear();
        const MM = pad(date.getMonth() + 1);
        const dd = pad(date.getDate());
        const hh = pad(date.getHours());
        const mm = pad(date.getMinutes());
        return `${yyyy}-${MM}-${dd}T${hh}:${mm}`;
    };
    shareExpiresAt.setAttribute('min', toLocal(min));
    shareExpiresAt.setAttribute('max', toLocal(max));
    shareExpiresAt.value = toLocal(min);
}

async function submitOwnerChange(event) {
    event?.preventDefault();
    if (!ownerInput || !ownerNoteId) return;
    const owner = (ownerInput.value || '').trim();
    if (!owner) {
        ownerInput.classList.add('is-invalid');
        return;
    }
    if (ownerSubmit) ownerSubmit.disabled = true;
    ownerSubmitSpinner?.classList.remove('d-none');
    const result = await handleApi(Api.changeOwner(ownerNoteId, {owner}), {
        fallback: 'Failed to change owner',
        onFinally: () => {
            ownerSubmitSpinner?.classList.add('d-none');
            if (ownerSubmit) ownerSubmit.disabled = false;
        }
    });
    if (result) {
        ownerModal?.hide();
        ownerNoteId = null;
        clearOwnerModal();
        showToast('Owner updated', 'success');
        loadNotes();
    }
}


function switchView(view) {
    if (state.view === view) return;
    state.view = view;
    state.page = 0;
    clearSelection();
    if (activeViewBtn && trashViewBtn) {
        activeViewBtn.classList.toggle('active', view === 'active');
        activeViewBtn.setAttribute('aria-selected', view === 'active');
        trashViewBtn.classList.toggle('active', view === 'trash');
        trashViewBtn.setAttribute('aria-selected', view === 'trash');
    }
    if (addNoteBtn) {
        addNoteBtn.disabled = view === 'trash';
    }
    pager.hidden = true;
    pagination.innerHTML = '';
    updateEmptyTrashButton();
    loadNotes();
}


function setLoading() {
    totalLabel.textContent = state.view === 'trash' ? 'Total (trash): …' : 'Total: …';
    totalLabel.classList.add('invisible');
    if (pageInfo) pageInfo.hidden = true;
    updateEmptyTrashButton();
    if (emptyTrashBtn) {
        emptyTrashBtn.disabled = true;
    }
    totalLabel.hidden = true;
    pager.hidden = true;
    pagination.innerHTML = '';
    noteGrid.innerHTML = '<div class="col-12 text-center py-3"><div class="spinner-border text-primary" role="status"><span class="visually-hidden">Loading...</span></div></div>';
}

function renderNotes(data) {
    noteCache.clear();
    const {notes, meta} = normalizeNotes(data);
    const showOwner = isAdmin?.() || false;
    if (!updateTotalsAndCheckEmpty(meta, notes.length)) {
        renderEmptyNotes();
        return;
    }
    const totalPages = meta?.totalPages ?? 1;
    const current = meta?.number ?? 0;

    bulkRow?.classList.remove('d-none');
    controlsRow?.classList.remove('d-none');
    const fragments = [];
    notes.forEach(note => {
        noteCache.set(note.id, note);
        const metaInfo = buildNoteMeta(note);
        const card = state.view === 'trash'
            ? buildTrashCard(note, metaInfo, showOwner)
            : buildActiveNoteCard(note, metaInfo, showOwner);
        fragments.push(card);
    });
    noteGrid.innerHTML = fragments.join('');

    pager.hidden = totalPages < 1 || notes.length === 0;
    notes.forEach(note => resetInlineTags(note.id, note.tags || []));
    const items = [];
    items.push(`<li class="page-item ${current === 0 ? 'disabled' : ''}">
            <a class="page-link" href="#" data-page="${current - 1}" aria-label="Previous">&laquo;</a>
        </li>`);
    for (let i = 0; i < totalPages; i++) {
        items.push(`<li class="page-item ${i === current ? 'active' : ''}">
                <a class="page-link" href="#" data-page="${i}">${i + 1}</a>
            </li>`);
    }
    items.push(`<li class="page-item ${current >= totalPages - 1 ? 'disabled' : ''}">
            <a class="page-link" href="#" data-page="${current + 1}" aria-label="Next">&raquo;</a>
        </li>`);
    pagination.innerHTML = items.join('');
    bindSelectionCheckboxes();
}

function normalizeNotes(data) {
    const notes = data?.content || [];
    const meta = data?.page ?? data ?? {};
    state.total = meta.totalElements ?? notes.length;
    state.page = meta.number ?? 0;
    state.totalPages = meta.totalPages ?? 1;
    return {notes, meta};
}

function updateTotalsAndCheckEmpty(meta, count) {
    totalLabel.hidden = count === 0;
    if (count > 0) {
        totalLabel.textContent = `Total: ${state.total}`;
        totalLabel.classList.remove('invisible');
    }
    updateEmptyTrashButton();
    if (pageInfo) {
        pageInfo.textContent =
                `Page ${(meta?.number ?? 0) + 1} of ${Math.max(meta?.totalPages ?? 1, 1)}`;
        pageInfo.hidden = count === 0;
    }
    return count > 0;
}

function renderEmptyNotes() {
    const emptyMsg = state.query
        ? 'No notes match your search.'
        : (state.view === 'trash' ? 'Trash is empty.' : 'No notes found. Create a new one to get started.');
    const emptyIcon = state.query
        ? 'fa-circle-info'
        : (state.view === 'trash' ? 'fa-trash-can' : 'fa-note-sticky');
    totalLabel.hidden = true;
    noteGrid.innerHTML = `
            <div class="col-12">
                <div class="list-group w-100">
                    <div class="list-group-item text-muted small d-flex align-items-center gap-2">
                        <i class="fa-solid ${emptyIcon}"></i>
                        <span>${emptyMsg}</span>
                    </div>
                </div>
            </div>`;
    bulkRow?.classList.add('d-none');
    controlsRow?.classList.add('d-none');
    clearSelection();
    pager.hidden = true;
    pagination.innerHTML = '';
    if (pageInfo) {
        pageInfo.hidden = true;
    }
}

function buildNoteMeta(note) {
    const createdText = formatDate(note.createdDate);
    const modifiedText = note.lastModifiedDate ? formatDate(note.lastModifiedDate) : createdText;
    const deletedText = note.deletedDate ? formatDate(note.deletedDate) : '';
    return {
        creator: note.createdBy ?? '',
        owner: note.owner ?? '',
        updater: note.lastModifiedBy ?? '',
        createdText,
        modifiedText,
        deletedBy: note.deletedBy ?? '',
        deletedText
    };
}

function buildTrashCard(note, metaInfo, showOwner) {
    const {creator, owner, updater, createdText, modifiedText, deletedBy, deletedText} = metaInfo;
    return `
        <div class="col-12 col-md-6 col-xl-6">
            <div class="card h-100 border-0 shadow-sm" id="note-${note.id}">
                <div class="card-body d-flex flex-column gap-2">
                    <div class="d-flex justify-content-between align-items-start">
                        <div class="form-check me-2 mt-1">
                            <input class="form-check-input selection-checkbox" type="checkbox" data-note-id="${note.id}" ${state.selected.has(note.id) ? 'checked' : ''}>
                        </div>
                        <div class="flex-grow-1">
                            <div class="d-flex align-items-center gap-2">
                                <div class="fw-bold text-primary mb-0">${escapeHtml(note.title)}</div>
                                ${note.pinned ? '<i class="fa-solid fa-thumbtack text-warning" title="Pinned"></i>' : ''}
                                ${note.color ? `<span class="badge rounded-pill bg-body-secondary border text-body" title="Color" style="border-color:${escapeHtml(note.color)};color:${escapeHtml(note.color)}"><i class="fa-solid fa-circle" style="color:${escapeHtml(note.color)}"></i></span>` : ''}
                            </div>
                            <div class="text-muted small">${escapeHtml(note.content)}</div>
                            ${renderTags(note)}
                        </div>
                        <div class="d-flex flex-wrap gap-1 justify-content-end">
                            <button class="btn btn-success btn-sm" data-action="restore" data-id="${note.id}" title="Restore">
                                <i class="fa-solid fa-rotate-left"></i>
                            </button>
                            <button class="btn btn-outline-secondary btn-sm" data-action="copy" data-id="${note.id}" title="Copy content">
                                <i class="fa-solid fa-copy"></i>
                            </button>
                            ${showOwner ? `<button class="btn btn-outline-secondary btn-sm" data-action="change-owner" data-id="${note.id}" title="Change owner">
                                <i class="fa-solid fa-user-gear"></i>
                            </button>` : ''}
                            <button class="btn btn-outline-info btn-sm" data-action="revisions" data-id="${note.id}" title="Revision history">
                                <i class="fa-solid fa-clock-rotate-left"></i>
                            </button>
                            <button class="btn btn-outline-danger btn-sm" data-action="delete-forever" data-id="${note.id}" title="Delete permanently">
                                <i class="fa-solid fa-trash"></i>
                            </button>
                        </div>
                    </div>
                    <div class="d-flex flex-column gap-1 text-muted small">
                        ${showOwner ? `<span><i class="fa-solid fa-user-shield me-1"></i>Owner: ${escapeHtml(owner)}</span>` : ''}
                        <span><i class="fa-solid fa-user me-1"></i>Created by: ${escapeHtml(creator)}</span>
                        ${updater ? `<span><i class="fa-solid fa-user-pen me-1"></i>Updated by: ${escapeHtml(updater)}</span>` : ''}
                    </div>
                    <div class="d-flex flex-column text-muted small gap-1">
                        ${buildDateRow('Created', createdText)}
                        ${buildDateRow('Updated', modifiedText)}
                    </div>
                    <div class="d-flex gap-3 text-muted small">
                        <span><i class="fa-solid fa-ban me-1"></i>Deleted by: ${escapeHtml(deletedBy || '—')}</span>
                    </div>
                    ${deletedText ? buildDeletedRow(deletedText) : ''}
                </div>
            </div>
        </div>
    `;
}

function buildActiveNoteCard(note, metaInfo, showOwner) {
    const {creator, owner, updater, createdText, modifiedText} = metaInfo;
    return `
        <div class="col-12 col-md-6 col-xl-6">
            <div class="card h-100 border-0 shadow-sm" id="note-${note.id}">
                <div class="card-body d-flex flex-column gap-2">
                    <div class="d-flex justify-content-between align-items-start view-mode">
                        <div class="form-check me-2 mt-1">
                            <input class="form-check-input selection-checkbox" type="checkbox" data-note-id="${note.id}" ${state.selected.has(note.id) ? 'checked' : ''}>
                        </div>
                        <div class="flex-grow-1">
                            <div class="d-flex align-items-center gap-2">
                                <div class="fw-bold text-primary mb-0">${escapeHtml(note.title)}</div>
                                ${note.pinned ? '<i class="fa-solid fa-thumbtack text-warning" title="Pinned"></i>' : ''}
                                ${note.color ? `<span class="badge rounded-pill bg-body-secondary border text-body" title="Color" style="border-color:${escapeHtml(note.color)};color:${escapeHtml(note.color)}"><i class="fa-solid fa-circle" style="color:${escapeHtml(note.color)}"></i></span>` : ''}
                            </div>
                            <div class="text-muted small">${escapeHtml(note.content)}</div>
                            ${renderTags(note)}
                        </div>
                        ${buildActiveActions(note, showOwner)}
                    </div>
                    ${buildInlineEditor(note)}
                    <div class="d-flex flex-column gap-1 text-muted small">
                        ${showOwner ? `<span><i class="fa-solid fa-user-shield me-1"></i>Owner: ${escapeHtml(owner)}</span>` : ''}
                        <span><i class="fa-solid fa-user me-1"></i>Created by: ${escapeHtml(creator)}</span>
                        ${updater ? `<span><i class="fa-solid fa-user-pen me-1"></i>Updated by: ${escapeHtml(updater)}</span>` : ''}
                    </div>
                    <div class="d-flex flex-column text-muted small gap-1">
                        ${buildDateRow('Created', createdText)}
                        ${buildDateRow('Updated', modifiedText)}
                    </div>
                </div>
            </div>
        </div>
    `;
}

function buildDateRow(label, text) {
    const [datePart = text, timePart = ''] = text.split(' ');
    return `
        <div class="d-flex align-items-center gap-2 flex-wrap">
            <i class="fa-regular fa-calendar me-1"></i>
            <span>${label}:</span>
            <span class="text-nowrap">${escapeHtml(datePart)}</span>
            <span class="d-inline-flex align-items-center gap-1 text-nowrap"><i class="fa-regular fa-clock"></i>${escapeHtml(timePart)}</span>
        </div>
    `;
}

function buildDeletedRow(deletedText) {
    const [datePart = deletedText, timePart = ''] = deletedText.split(' ');
    return `
        <div class="d-flex text-muted small align-items-center gap-2 flex-wrap mt-1">
            <i class="fa-regular fa-calendar me-1"></i>
            <span>Deleted:</span>
            <span class="text-nowrap">${escapeHtml(datePart)}</span>
            <span class="d-inline-flex align-items-center gap-1 text-nowrap"><i class="fa-regular fa-clock"></i>${escapeHtml(timePart)}</span>
        </div>
    `;
}

function buildActiveActions(note, showOwner) {
    return `
        <div class="d-grid gap-1" style="grid-template-columns: repeat(3, 32px); justify-content: end; justify-items: end;">
            <button class="btn btn-outline-warning btn-sm" style="width: 32px; height: 32px;" data-action="toggle-pin" data-id="${note.id}" title="${note.pinned ? 'Unpin' : 'Pin'}">
                <i class="fa-solid fa-thumbtack ${note.pinned ? '' : 'opacity-50'}"></i>
            </button>
            <button class="btn btn-outline-primary btn-sm" style="width: 32px; height: 32px;" data-action="edit-modal" data-id="${note.id}" title="Edit in modal">
                <i class="fa-solid fa-pen-to-square"></i>
            </button>
            <button class="btn btn-outline-secondary btn-sm" style="width: 32px; height: 32px;" data-action="inline-edit" data-id="${note.id}" title="Inline edit">
                <i class="fa-solid fa-pen"></i>
            </button>
            <button class="btn btn-outline-secondary btn-sm" style="width: 32px; height: 32px;" data-action="copy" data-id="${note.id}" title="Copy content">
                <i class="fa-solid fa-copy"></i>
            </button>
            <button class="btn btn-outline-secondary btn-sm" style="width: 32px; height: 32px;" data-action="share-links" data-id="${note.id}" title="Existing links">
                <i class="fa-solid fa-link"></i>
            </button>
            <button class="btn btn-outline-secondary btn-sm" style="width: 32px; height: 32px;" data-action="share" data-id="${note.id}" title="Create share link">
                <i class="fa-solid fa-share-from-square"></i>
            </button>
            ${showOwner ? `<button class="btn btn-outline-secondary btn-sm" style="width: 32px; height: 32px;" data-action="change-owner" data-id="${note.id}" title="Change owner">
                <i class="fa-solid fa-user-gear"></i>
            </button>` : ''}
            <button class="btn btn-outline-info btn-sm" style="width: 32px; height: 32px;" data-action="revisions" data-id="${note.id}" title="Revision history">
                <i class="fa-solid fa-clock-rotate-left"></i>
            </button>
            <button class="btn btn-outline-danger btn-sm" style="width: 32px; height: 32px;" data-action="delete" data-id="${note.id}" title="Delete">
                <i class="fa-solid fa-trash"></i>
            </button>
        </div>
    `;
}

function buildInlineEditor(note) {
    return `
        <div class="edit-mode d-none">
            <div class="mb-2">
                <input class="form-control form-control-sm" type="text" placeholder="Title"
                       minlength="3" maxlength="255" required value="${escapeHtml(note.title)}"
                       data-inline-title="${note.id}">
                <div class="invalid-feedback d-none" data-inline-title-required="${note.id}">This field is required.</div>
                <div class="invalid-feedback d-none" data-inline-title-size="${note.id}">Size must be between 3 and 255 characters.</div>
            </div>
            <div class="mb-2">
                <textarea class="form-control form-control-sm" rows="3" placeholder="Content"
                          minlength="10" maxlength="1024" required
                          data-inline-content="${note.id}">${escapeHtml(note.content)}</textarea>
                <div class="invalid-feedback d-none" data-inline-content-required="${note.id}">This field is required.</div>
                <div class="invalid-feedback d-none" data-inline-content-size="${note.id}">Size must be between 10 and 1024 characters.</div>
            </div>
            <div class="mb-2">
                <label class="form-label small mb-1" for="inlineColor-${note.id}">Color</label>
                <input class="form-control form-control-color" type="color" id="inlineColor-${note.id}" data-inline-color="${note.id}" value="${escapeHtml(note.color || '#2563eb')}">
            </div>
            <div class="mb-2">
                <label class="form-label small mb-1" for="inlineTagsInput-${note.id}">Tags</label>
                <div class="form-control p-2 pe-5" data-inline-tags-container="${note.id}">
                    <div class="d-flex flex-wrap gap-2 mb-2" data-inline-tags-list="${note.id}"></div>
                    <input class="form-control form-control-sm border-0 shadow-none p-0" type="text" id="inlineTagsInput-${note.id}" data-inline-tags-input="${note.id}" placeholder="Type and press Enter or comma" list="tagSuggestions">
                </div>
                <div class="invalid-feedback d-none" data-inline-tags-error="${note.id}"></div>
            </div>
            <div class="form-check form-switch mb-3">
                <input class="form-check-input" type="checkbox" id="inlinePinned-${note.id}" data-inline-pinned="${note.id}" ${note.pinned ? 'checked' : ''}>
                <label class="form-check-label text-body" for="inlinePinned-${note.id}">Pin this note</label>
            </div>
            <div class="d-flex justify-content-end gap-2">
                <button class="btn btn-outline-secondary btn-sm d-inline-flex align-items-center gap-1" data-action="inline-cancel" data-id="${note.id}">
                    <i class="fa-solid fa-xmark"></i> Cancel
                </button>
                <button class="btn btn-primary btn-sm d-inline-flex align-items-center gap-1" data-action="inline-save" data-id="${note.id}">
                    <i class="fa-solid fa-save"></i> Save
                </button>
            </div>
        </div>
    `;
}

function bindSelectionCheckboxes() {
    document.querySelectorAll('.selection-checkbox').forEach(cb => {
        cb.addEventListener('change', (e) => {
            const id = Number.parseInt(e.target.dataset.noteId, 10);
            toggleSelection(id, e.target.checked);
        });
    });
    syncSelectAllCheckbox();
    updateBulkButtons();
    syncCheckboxStates();
}

function revisionLocalNumber(globalIndex, provided) {
    if (provided != null) {
        return provided;
    }
    if (typeof revisionTotal === 'number' && revisionTotal > 0) {
        return revisionTotal - globalIndex;
    }
    return globalIndex + 1;
}

function revisionVersionLabel(revision) {
    const v = revision?.note?.version;
    return (typeof v === 'number') ? (v + 1) : null;
}

function renderRevisionItem(rev, noteId, index, localNumber) {
    const note = rev.note || {};
    const tags = note.tags && note.tags.length
        ? `<div class="d-flex flex-wrap gap-1 mt-1">${note.tags.map(t => `<span class="badge bg-secondary-subtle text-secondary">${escapeHtml(tagLabel(t))}</span>`).join('')}</div>`
        : '';
    const pinnedBadge = note.pinned
        ? '<span class="badge bg-warning-subtle text-warning border border-warning-subtle">Pinned</span>'
        : '<span class="badge bg-body-secondary text-secondary border">Unpinned</span>';
    const colorDot = note.color ? `<span class="badge bg-body-secondary border text-body" title="Color" style="border-color:${escapeHtml(note.color)};color:${escapeHtml(note.color)}"><i class="fa-solid fa-circle" style="color:${escapeHtml(note.color)}"></i></span>` : '';
    const revTypeClass = revisionTypeBadge(rev.revisionType);
    const revTypeLabel = `<span class="badge ${revTypeClass} text-uppercase">${escapeHtml(rev.revisionType || 'N/A')}</span>`;
    const versionFromNote = revisionVersionLabel(rev);
    const displayLocal = revisionLocalNumber(index, localNumber ?? versionFromNote);
    return `
        <div class="list-group-item">
            <div class="d-flex flex-column flex-md-row justify-content-between gap-3">
                <div class="flex-grow-1">
                    <div class="d-flex align-items-center gap-2 flex-wrap mb-1">
                        <span class="badge bg-primary-subtle text-primary border border-primary-subtle">v${escapeHtml(displayLocal)}</span>
                        ${revTypeLabel}
                        <span class="text-muted small"><i class="fa-regular fa-clock me-1"></i>${escapeHtml(formatDate(rev.revisionDate) || '—')}</span>
                        <span class="text-muted small"><i class="fa-solid fa-user me-1"></i>${escapeHtml(rev.auditor || 'unknown')}</span>
                        ${colorDot}
                        ${pinnedBadge}
                    </div>
                    <div class="fw-semibold">${escapeHtml(note.title || '(no title)')}</div>
                    <div class="text-muted small">${escapeHtml(note.content || '')}</div>
                    ${tags}
                </div>
                <div class="d-flex flex-column align-items-end gap-2">
                    <button class="btn btn-sm btn-outline-secondary d-inline-flex align-items-center gap-2"
                            data-action="revision-diff"
                            data-rev-index="${index}">
                        <i class="fa-solid fa-code-compare"></i> Diff
                    </button>
                    <button class="btn btn-sm btn-outline-primary d-inline-flex align-items-center gap-2"
                            data-revision-restore="${noteId}-${rev.revision}"
                            data-action="revision-restore"
                            data-note-id="${noteId}"
                            data-rev-id="${rev.revision}">
                        <i class="fa-solid fa-rotate-left"></i> Restore
                    </button>
                </div>
            </div>
            <div class="mt-2 d-none" data-diff-block="${rev.revision}"></div>
        </div>`;
}

function showRevisionError(message) {
    if (!revisionError) return;
    revisionError.textContent = message;
    revisionError.classList.remove('d-none');
}

function clearRevisionError() {
    if (!revisionError) return;
    revisionError.textContent = '';
    revisionError.classList.add('d-none');
}

function renderColorDiff(oldColor, newColor, additionsOnly = false) {
    if (oldColor === newColor) {
        return '<span class="text-muted small">No color changes.</span>';
    }
    const oldDot = oldColor ? `<span class="badge bg-body-secondary border text-body" style="border-color:${escapeHtml(oldColor)};color:${escapeHtml(oldColor)}"><i class="fa-solid fa-circle" style="color:${escapeHtml(oldColor)}"></i></span>` : '<span class="text-muted small">none</span>';
    const newDot = newColor ? `<span class="badge bg-body-secondary border text-body" style="border-color:${escapeHtml(newColor)};color:${escapeHtml(newColor)}"><i class="fa-solid fa-circle" style="color:${escapeHtml(newColor)}"></i></span>` : '<span class="text-muted small">none</span>';
    if (additionsOnly) {
        return `<div class="d-flex flex-wrap gap-2">
                        <span class="badge bg-success-subtle text-success border border-success-subtle d-inline-flex align-items-center gap-1">+ ${newDot}</span>
                    </div>`;
    }
    return `<div class="d-flex flex-wrap gap-2">
                    <span class="badge bg-danger-subtle text-danger border border-danger-subtle d-inline-flex align-items-center gap-1">− ${oldDot}</span>
                    <span class="badge bg-success-subtle text-success border border-success-subtle d-inline-flex align-items-center gap-1">+ ${newDot}</span>
                </div>`;
}

function renderPinnedDiff(oldPinned, newPinned, additionsOnly = false) {
    if (!!oldPinned === !!newPinned) {
        return '<span class="text-muted small">No pin changes.</span>';
    }
    const badge = (val) => val
        ? '<span class="badge bg-warning-subtle text-warning border border-warning-subtle">Pinned</span>'
        : '<span class="badge bg-body-secondary text-secondary border">Unpinned</span>';
    if (additionsOnly) {
        return `<div class="d-flex flex-wrap gap-2">
                        <span class="badge bg-success-subtle text-success border border-success-subtle d-inline-flex align-items-center gap-1">+ ${badge(!!newPinned)}</span>
                    </div>`;
    }
    return `<div class="d-flex flex-wrap gap-2">
                    <span class="badge bg-danger-subtle text-danger border border-danger-subtle d-inline-flex align-items-center gap-1">− ${badge(!!oldPinned)}</span>
                    <span class="badge bg-success-subtle text-success border border-success-subtle d-inline-flex align-items-center gap-1">+ ${badge(!!newPinned)}</span>
                </div>`;
}

function renderTagsDiff(oldTags, newTags, additionsOnly = false) {
    const normalize = (list) => (list || [])
        .map(tagLabel)
        .filter(t => t && t.trim().length > 0);
    const oldSet = new Set(normalize(oldTags));
    const newSet = new Set(normalize(newTags));
    const added = [...newSet].filter(t => !oldSet.has(t));
    const removed = [...oldSet].filter(t => !newSet.has(t));

    const renderBadges = (list, cls, prefix) => list.map(t =>
        `<span class="badge ${cls} d-inline-flex align-items-center gap-1">${prefix ? prefix + ' ' : ''}${escapeHtml(t)}</span>`
    ).join('');

    const badges = additionsOnly
        ? renderBadges(added, 'bg-success-subtle text-success border border-success-subtle', '+')
        : [
            renderBadges(removed, 'bg-danger-subtle text-danger border border-danger-subtle', '−'),
            renderBadges(added, 'bg-success-subtle text-success border border-success-subtle', '+')
        ].filter(Boolean).join(' ');

    if (!added.length && !removed.length) {
        return '<span class="text-muted small">No tag changes.</span>';
    }
    return badges;
}

function tagLabel(tag) {
    if (tag == null) return '';
    if (typeof tag === 'string') return tag;
    if (typeof tag === 'object') {
        return tag.name ?? tag.label ?? '';
    }
    return String(tag);
}

function diffWords(oldText, newText) {
    const a = (oldText || '').split(/\s+/);
    const b = (newText || '').split(/\s+/);
    const m = a.length;
    const n = b.length;
    const lcs = Array.from({length: m + 1}, () => new Array(n + 1).fill(0));
    for (let i = m - 1; i >= 0; i--) {
        for (let j = n - 1; j >= 0; j--) {
            if (a[i] === b[j]) {
                lcs[i][j] = 1 + lcs[i + 1][j + 1];
            } else {
                lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }
    }
    const ops = [];
    let i = 0;
    let j = 0;
    while (i < m && j < n) {
        if (a[i] === b[j]) {
            ops.push({type: 'eq', value: a[i]});
            i++;
            j++;
        } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
            ops.push({type: 'del', value: a[i]});
            i++;
        } else {
            ops.push({type: 'add', value: b[j]});
            j++;
        }
    }
    while (i < m) ops.push({type: 'del', value: a[i++]});
    while (j < n) ops.push({type: 'add', value: b[j++]});
    return ops;
}

function renderInlineDiff(oldText, newText, additionsOnly = false) {
    const ops = diffWords(oldText, newText);
    const hasChange = ops.some(op => op.type !== 'eq');
    if (!hasChange) {
        return '<span class="text-muted small">No changes.</span>';
    }
    // Merge consecutive ops of the same type (word-level)
    const segments = [];
    ops.forEach(op => {
        if (additionsOnly && op.type === 'del') {
            return;
        }
        const last = segments[segments.length - 1];
        if (last && last.type === op.type) {
            last.value += ' ' + op.value;
        } else {
            segments.push({type: op.type, value: op.value});
        }
    });
    const spans = segments.map(op => {
        const val = escapeHtml(op.value);
        if (op.type === 'eq') {
            return val;
        }
        if (op.type === 'add') {
            return `<span class="bg-success-subtle text-success border border-success-subtle rounded px-2">${val}</span>`;
        }
        if (op.type === 'del') {
            return additionsOnly ? '' : `<span class="bg-danger-subtle text-danger border border-danger-subtle rounded px-2 text-decoration-line-through">${val}</span>`;
        }
        return val;
    }).join(' ');
    return spans || '<span class="text-muted small">No changes.</span>';
}

function showRevisionDiff(index) {
    if (!revisionCache || revisionCache.length === 0) return;
    const current = revisionCache[index];
    if (!current) return;
    const prev = revisionCache[index + 1] || null;
    const currentLocal = revisionLocalNumber(index, revisionVersionLabel(current));
    const prevLocal = prev ? revisionLocalNumber(index + 1, revisionVersionLabel(prev)) : null;
    const initialOnly = !prev; // first revision, show only additions
    const currentNote = current.note || {};
    const prevNote = prev?.note || {};
    const titleDiff = renderInlineDiff(prevNote.title || '', currentNote.title || '', initialOnly);
    const contentDiff = renderInlineDiff(prevNote.content || '', currentNote.content || '', initialOnly);
    const colorDiff = renderColorDiff(prevNote.color, currentNote.color, initialOnly);
    const pinnedDiff = renderPinnedDiff(prevNote.pinned, currentNote.pinned, initialOnly);
    const tagsDiff = renderTagsDiff(prevNote.tags || [], currentNote.tags || [], initialOnly);
    const container = revisionList?.querySelector(`[data-diff-block="${current.revision}"]`);
    if (!container) return;
    container.innerHTML = `
            <div class="card border-secondary-subtle">
                <div class="card-body p-3">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <span class="text-muted small">Diff vs ${prev ? (`v${prevLocal}`) : 'current'}</span>
                        <button class="btn btn-sm btn-link text-decoration-none" data-action="hide-diff" data-rev-id="${current.revision}">Close</button>
                    </div>
                    <div class="mb-3">
                        <div class="fw-semibold mb-2">Title</div>
                        ${titleDiff}
                    </div>
                    <div class="mb-3">
                        <div class="fw-semibold mb-2">Content</div>
                        ${contentDiff}
                    </div>
                    <hr class="my-3">
                    <div class="mb-2">
                        <div class="fw-semibold mb-1">Color</div>
                        ${colorDiff}
                    </div>
                    <div class="mb-2">
                        <div class="fw-semibold mb-1">Pinned</div>
                        ${pinnedDiff}
                    </div>
                    <div>
                        <div class="fw-semibold mb-1">Tags</div>
                        ${tagsDiff}
                    </div>
                </div>
            </div>`;
    container.classList.remove('d-none');
}

function renderRevisionItems(items, noteId, startIndex, append = false) {
    if (!revisionList) return;
    if (!append) {
        revisionList.innerHTML = '';
    }
    if ((!items || !items.length) && !append) {
        revisionList.innerHTML = '<div class="list-group-item text-muted">No revisions yet.</div>';
        return;
    }
    const html = (items || []).map((rev, idx) => {
        const globalIndex = startIndex + idx;
        const localNumber = revisionLocalNumber(globalIndex, revisionVersionLabel(rev));
        return renderRevisionItem(rev, noteId, globalIndex, localNumber);
    }).join('');
    revisionList.insertAdjacentHTML('beforeend', html);
    const loadMoreRow = revisionList.querySelector('[data-action="revision-load-more"]')?.parentElement;
    if (loadMoreRow) {
        loadMoreRow.remove();
    }
    if (revisionHasMore) {
        revisionList.insertAdjacentHTML('beforeend', `
                <div class="list-group-item text-center d-flex justify-content-center align-items-center gap-2">
                    <span class="spinner-border spinner-border-sm d-none" data-revision-load-spinner="true" role="status" aria-hidden="true"></span>
                    <button class="btn btn-outline-secondary btn-sm" data-action="revision-load-more" data-note-id="${noteId}">
                        Load more
                    </button>
                </div>`);
    }
}

function toggleRevisionLoadSpinner(show) {
    const row = revisionList?.querySelector('[data-action="revision-load-more"]')?.closest('.list-group-item');
    if (!row) return;
    const spinner = row.querySelector('[data-revision-load-spinner="true"]');
    const button = row.querySelector('button');
    if (show) {
        spinner?.classList.remove('d-none');
        if (button) button.disabled = true;
    } else {
        spinner?.classList.add('d-none');
        if (button) button.disabled = false;
    }
}

async function loadRevisionPage(noteId, append = false) {
    if (shouldSkipRevisionLoad(noteId, append)) return;
    prepareRevisionList(append);
    const activeNoteId = revisionNoteId;
    const startedAt = Date.now();
    toggleRevisionIndicators(append, true);
    try {
        await fetchAndRenderRevisions(noteId, append, activeNoteId);
    } catch (e) {
        showRevisionError(getErrorMessage(e, 'Failed to load revisions'));
    } finally {
        finalizeRevisionLoad(append, activeNoteId, startedAt);
    }
}

function shouldSkipRevisionLoad(noteId, append) {
    return !revisionList || !revisionNoteId || revisionNoteId !== noteId || isLoadingRevisions || (!revisionHasMore && append);
}

function prepareRevisionList(append) {
    if (!append) {
        revisionList.innerHTML = '';
    }
    clearRevisionError();
    isLoadingRevisions = true;
}

async function fetchAndRenderRevisions(noteId, append, activeNoteId) {
    const pageData = await Api.fetchRevisions(noteId, revisionPage, revisionPageSize);
    const content = pageData?.content ?? pageData ?? [];
    const meta = pageData?.page ?? pageData;
    const startIndex = revisionCache.length;
    revisionCache = revisionCache.concat(content);
    updateRevisionTotals(meta, content.length, append);
    const totalPages = meta?.totalPages;
    revisionHasMore = typeof totalPages === 'number'
        ? revisionPage + 1 < totalPages
        : Boolean(content.length);
    if (revisionNoteId === activeNoteId) {
        renderRevisionItems(content, noteId, startIndex, append);
        revisionPage += 1;
    }
}

function updateRevisionTotals(meta, newCount, append) {
    const totalElements = meta?.totalElements;
    if (typeof totalElements === 'number') {
        revisionTotal = totalElements;
        return;
    }
    if (!append) {
        revisionTotal = newCount;
        return;
    }
    revisionTotal = Math.max(revisionTotal, revisionCache.length);
}

function toggleRevisionIndicators(append, show) {
    if (append) {
        toggleRevisionLoadSpinner(show);
    } else if (show) {
        revisionSpinner?.classList.remove('d-none');
    } else {
        revisionSpinner?.classList.add('d-none');
    }
}

function finalizeRevisionLoad(append, activeNoteId, startedAt) {
    if (revisionNoteId !== activeNoteId) {
        return;
    }
    if (append) {
        const elapsed = Date.now() - startedAt;
        const hide = () => toggleRevisionLoadSpinner(false);
        if (elapsed < 120) {
            setTimeout(hide, 120 - elapsed);
        } else {
            hide();
        }
    } else {
        toggleRevisionIndicators(false, false);
    }
    isLoadingRevisions = false;
}

async function openRevisionModal(noteId) {
    if (!revisionModal) return;
    revisionNoteId = noteId;
    const cached = noteCache.get(noteId);
    if (revisionModalTitle) {
        const versionRaw = (cached?.version ?? cached?.note?.version);
        const versionLabel = typeof versionRaw === 'number' ? versionRaw + 1 : versionRaw;
        const titleText = cached?.title
            ? `${cached.title} · ${versionLabel ? `v${versionLabel}` : `#${noteId}`}`
            : `Revisions · ${versionLabel ? `v${versionLabel}` : `#${noteId}`}`;
        revisionModalTitle.textContent = titleText;
    }
    clearRevisionError();
    if (revisionList) revisionList.innerHTML = '';
    revisionSpinner?.classList.remove('d-none');
    revisionModal.show();
    revisionPage = 0;
    revisionHasMore = false;
    revisionTotal = 0;
    revisionCache = [];
    if (revisionModalBody) {
        revisionModalBody.scrollTop = 0;
    }
    await loadRevisionPage(noteId, false);
}

const revisionScrollContainer = revisionModalBody || revisionList;
if (revisionScrollContainer) {
    revisionScrollContainer.style.maxHeight = '40rem'; // allow ~5 cards fully visible
    revisionScrollContainer.style.minHeight = '30rem';
    revisionScrollContainer.style.overflowY = 'auto';
}
if (revisionModalEl) {
    revisionModalEl.addEventListener('hidden.bs.modal', () => {
        resetRevisionState();
        if (revisionList) {
            revisionList.innerHTML = '';
        }
    });
}

function updateEmptyTrashButton() {
    if (!emptyTrashBtn) return;
    const isTrash = state.view === 'trash';
    emptyTrashBtn.hidden = !isTrash;
    emptyTrashBtn.classList.toggle('d-none', !isTrash);
    if (isTrash) {
        const hasItems = state.total > 0;
        emptyTrashBtn.hidden = !hasItems;
        emptyTrashBtn.classList.toggle('d-none', !hasItems);
        emptyTrashBtn.disabled = !hasItems;
        emptyTrashSpinner?.classList.add('d-none');
        if (emptyTrashLabel) {
            emptyTrashLabel.textContent = 'Empty Trash';
        }
    }
}

function clearSelection() {
    state.selected = new Set();
    if (selectAllCheckbox) {
        selectAllCheckbox.checked = false;
        selectAllCheckbox.indeterminate = false;
    }
    syncCheckboxStates();
    updateBulkButtons();
}

function toggleSelection(id, checked) {
    if (!id) return;
    if (checked) {
        if (state.selected.size >= BULK_LIMIT) {
            showToast(`You can select up to ${BULK_LIMIT} notes for bulk actions.`, 'warning');
            syncCheckboxStates();
            return;
        }
        state.selected.add(id);
    } else {
        state.selected.delete(id);
    }
    syncSelectAllCheckbox();
    updateBulkButtons();
}

function syncSelectAllCheckbox() {
    if (!selectAllCheckbox) return;
    const currentIds = Array.from(noteCache.keys());
    const selectedOnPage = currentIds.filter(id => state.selected.has(id));
    if (selectedOnPage.length === 0) {
        selectAllCheckbox.checked = false;
        selectAllCheckbox.indeterminate = false;
    } else if (selectedOnPage.length === currentIds.length) {
        selectAllCheckbox.checked = true;
        selectAllCheckbox.indeterminate = false;
    } else {
        selectAllCheckbox.checked = false;
        selectAllCheckbox.indeterminate = true;
    }
}

function syncCheckboxStates() {
    document.querySelectorAll('.selection-checkbox').forEach(cb => {
        const id = Number.parseInt(cb.dataset.noteId, 10);
        cb.checked = state.selected.has(id);
    });
}

function updateBulkButtons() {
    const count = state.selected.size;
    const isTrash = state.view === 'trash';
    if (selectedCount) {
        selectedCount.textContent = `Selected: ${count}`;
        selectedCount.classList.toggle('d-none', count === 0);
    }
    if (bulkDeleteBtn) {
        bulkDeleteBtn.classList.toggle('d-none', isTrash);
        bulkDeleteBtn.disabled = count === 0 || isTrash;
    }
    if (bulkRestoreBtn) {
        bulkRestoreBtn.classList.toggle('d-none', !isTrash);
        bulkRestoreBtn.disabled = count === 0;
    }
    if (bulkDeleteForeverBtn) {
        bulkDeleteForeverBtn.classList.toggle('d-none', !isTrash);
        bulkDeleteForeverBtn.disabled = count === 0;
    }
}

function openBulkModal(action) {
    if (!bulkModal || !bulkModalEl) return;
    if (!state.selected.size) return;
    if (state.selected.size > BULK_LIMIT) {
        showToast(`You can select up to ${BULK_LIMIT} notes for bulk actions.`, 'warning');
        return;
    }
    bulkModalEl.dataset.action = action;
    const count = state.selected.size;
    if (bulkModalMessage) {
        const messages = {
            DELETE_SOFT: `Delete ${count} selected note(s)?`,
            RESTORE: `Restore ${count} selected note(s)?`,
            DELETE_FOREVER: `Permanently delete ${count} selected note(s)?`
        };
        bulkModalMessage.textContent = messages[action] || 'Apply to selected items?';
    }
    if (bulkConfirmLabel) {
        const labels = {
            DELETE_SOFT: 'Delete',
            RESTORE: 'Restore',
            DELETE_FOREVER: 'Delete'
        };
        bulkConfirmLabel.textContent = labels[action] || 'Confirm';
    }
    bulkModal.show();
}

async function performBulkAction() {
    if (!bulkModal || !bulkModalEl) return;
    const action = bulkModalEl.dataset.action;
    if (!action) return;
    const ids = Array.from(state.selected);
    if (!ids.length) return;
    if (ids.length > BULK_LIMIT) {
        showToast(`You can select up to ${BULK_LIMIT} notes for bulk actions.`, 'warning');
        return;
    }
    confirmBulkBtn.disabled = true;
    bulkSpinner?.classList.remove('d-none');
    const original = bulkConfirmLabel?.textContent;
    if (bulkConfirmLabel) {
        bulkConfirmLabel.textContent = 'Processing...';
    }
    const result = await handleApi(Api.bulkAction({action, ids}), {
        fallback: 'Bulk action failed'
    });
    confirmBulkBtn.disabled = false;
    bulkSpinner?.classList.add('d-none');
    if (bulkConfirmLabel) {
        bulkConfirmLabel.textContent = original || 'Confirm';
    }
    if (!result) return;
    bulkModal.hide();
    clearSelection();
    await loadNotes();
    const processed = result.processedCount ?? ids.length;
    const failed = result.failedIds ?? [];
    const successMsg = action === 'RESTORE' ? 'Notes restored' : 'Notes deleted';
    if (failed.length) {
        showToast(`${successMsg}. Processed: ${processed}. Failed: ${failed.length} (${failed.join(', ')}).`, 'warning');
    } else {
        showToast(successMsg, 'success');
    }
}

async function loadNotes() {
    setLoading();
    try {
        const data = await Api.fetchNotes({
            view: state.view,
            page: state.page,
            size: state.size,
            sort: state.sort || defaultSort,
            query: state.query,
            tags: Array.from(state.filterTags || []),
            color: state.filterColor || null,
            pinned: state.filterPinned
        });
        const meta = data?.page ?? data;
        if (meta && typeof meta.totalPages === 'number') {
            const targetPage = Math.max(Math.min(state.page, Math.max(meta.totalPages - 1, 0)), 0);
            if (targetPage !== state.page) {
                state.page = targetPage;
                return loadNotes();
            }
        }
        renderNotes(data);
    } catch (e) {
        const message = getErrorMessage(e, 'Failed to load');
        noteGrid.innerHTML = `<div class="col-12 text-center text-danger py-3">${escapeHtml(message)}</div>`;
        if (pageInfo) {
            pageInfo.hidden = true;
        }
        totalLabel.classList.add('invisible');
        pager.hidden = true;
        updateEmptyTrashButton();
    }
}

function clearValidation() {
    noteForm.classList.remove('was-validated');
    titleInput.classList.remove('is-valid', 'is-invalid');
    contentInput.classList.remove('is-valid', 'is-invalid');
    pinnedInput?.classList.remove('is-valid', 'is-invalid');
    tagsDirty = false;
    tagsContainer?.classList.remove('is-invalid', 'is-valid');
    tagsInput?.classList.remove('is-valid', 'is-invalid');
    tagsLimitMsg?.classList.add('d-none');
    markTagsValid();
}

function isTagValid(tag) {
    return TAG_PATTERN.test(tag.trim());
}

function markTagsValid() {
    if (!tagsContainer) return;
    const hasError = tagsContainer.classList.contains('is-invalid');
    if (hasError) {
        tagsContainer.classList.remove('is-valid');
        return;
    }
    const interacted = noteForm.classList.contains('was-validated') || tagsDirty;
    if (!interacted || currentTags.size === 0) {
        tagsContainer.classList.remove('is-valid', 'is-invalid');
        return;
    }
    const valid = currentTags.size <= TAG_LIMIT;
    tagsContainer.classList.toggle('is-valid', valid);
    if (!valid) {
        tagsContainer.classList.remove('is-valid');
    }
}

function showTagError(message) {
    if (!tagsLimitMsg || !tagsContainer) return;
    tagsLimitMsg.textContent = message;
    tagsLimitMsg.classList.remove('d-none');
    tagsLimitMsg.classList.add('d-block');
    tagsContainer?.classList.add('is-invalid');
    tagsContainer?.classList.remove('is-valid');
}

function clearTagError() {
    if (tagsLimitMsg) {
        tagsLimitMsg.classList.add('d-none');
        tagsLimitMsg.classList.remove('d-block');
    }
    tagsContainer?.classList.remove('is-invalid');
    markTagsValid();
}

function renderTagsChips() {
    if (!tagsListEl) return;
    tagsListEl.innerHTML = '';
    currentTags.forEach(tag => {
        const pill = document.createElement('span');
        pill.className = 'badge rounded-pill bg-body-secondary border text-body-secondary d-inline-flex align-items-center gap-1 px-2 py-1 small';
        pill.innerHTML = `${escapeHtml(tag)} <button type="button" class="btn btn-sm btn-link p-0 text-secondary remove-tag" aria-label="Remove tag" data-tag="${escapeHtml(tag)}"><i class="fa-solid fa-xmark"></i></button>`;
        tagsListEl.appendChild(pill);
    });
    updateTagsLimitMessage();
    markTagsValid();
}

function updateTagsLimitMessage(showWarning = false) {
    if (!tagsLimitMsg) return;
    clearTagError();
}

function addTagsFromInput() {
    if (!tagsInput) return;
    const raw = tagsInput.value || '';
    if (!raw.trim()) return;
    const parts = raw.split(',').map(part => part.trim()).filter(Boolean);
    const invalid = parts.find(part => !isTagValid(part));
    if (invalid) {
        showTagError(TAG_FORMAT_MESSAGE);
        return;
    }
    clearTagError();
    let limitHit = false;
    parts.forEach(cleaned => {
        if (currentTags.size < TAG_LIMIT) {
            currentTags.add(cleaned);
            tagsDirty = true;
        } else {
            limitHit = true;
        }
    });
    tagsInput.value = '';
    renderTagsChips();
    markTagsValid();
    if (limitHit) {
        showToast(`You can add up to ${TAG_LIMIT} tags.`, 'warning');
        clearTagError();
    }
}

function renderFilterTags() {
    if (!filterTagsList) return;
    filterTagsList.innerHTML = '';
    state.filterTags.forEach(tag => {
        const pill = document.createElement('span');
        pill.className = 'badge rounded-pill bg-body-secondary border text-body-secondary d-inline-flex align-items-center gap-1 px-2 py-1 small';
        pill.innerHTML = `${escapeHtml(tag)} <button type="button" class="btn btn-sm btn-link p-0 text-secondary" data-filter-tag-remove="${escapeHtml(tag)}" aria-label="Remove tag"><i class="fa-solid fa-xmark"></i></button>`;
        filterTagsList.appendChild(pill);
    });
}

function showFilterTagError(message) {
    filterTagsContainer?.classList.add('is-invalid');
    if (filterTagsError) {
        filterTagsError.textContent = message;
        filterTagsError.classList.remove('d-none');
    }
}

function clearFilterTagError() {
    filterTagsContainer?.classList.remove('is-invalid');
    if (filterTagsError) {
        filterTagsError.textContent = '';
        filterTagsError.classList.add('d-none');
    }
}

function addFilterTagsFromInput() {
    if (!filterTagsInput) return;
    const raw = filterTagsInput.value || '';
    if (!raw.trim()) return;
    const parts = raw.split(',').map(p => p.trim()).filter(Boolean);
    const invalid = parts.find(part => !TAG_PATTERN.test(part));
    if (invalid) {
        showFilterTagError(TAG_FORMAT_MESSAGE);
        return;
    }
    let limitHit = false;
    parts.forEach(tag => {
        if (state.filterTags.size < FILTER_TAG_LIMIT) {
            state.filterTags.add(tag);
        } else {
            limitHit = true;
        }
    });
    if (limitHit) {
        showToast(`You can filter by up to ${FILTER_TAG_LIMIT} tags.`, 'warning');
    }
    filterTagsInput.value = '';
    renderFilterTags();
    clearFilterTagError();
}

const loadTagSuggestions = debounce(async (query) => {
    try {
        const tags = await Api.fetchTags(query);
        if (!filterTagsSuggestions) return;
        filterTagsSuggestions.innerHTML = '';
        (tags || []).forEach(tag => {
            const opt = document.createElement('option');
            opt.value = tag;
            filterTagsSuggestions.appendChild(opt);
        });
    } catch (_) {
        // ignore suggestions errors
    }
}, 200);

const loadNoteTagSuggestions = debounce(async (query) => {
    if (!tagSuggestions) return;
    if (!query || query.length < 2) {
        tagSuggestions.innerHTML = '';
        return;
    }
    try {
        const tags = await Api.fetchTags(query);
        tagSuggestions.innerHTML = '';
        (tags || []).forEach(tag => {
            const opt = document.createElement('option');
            opt.value = tag;
            tagSuggestions.appendChild(opt);
        });
    } catch (_) {
        tagSuggestions.innerHTML = '';
    }
}, 200);

function markInlineTagsValid(id) {
    const container = document.querySelector(`[data-inline-tags-container="${id}"]`);
    if (!container) return;
    const dirty = inlineTagsDirty.get(id);
    const tags = inlineTagsState.get(id) || new Set();
    const interacted = dirty || container.classList.contains('was-validated');

    if (container.classList.contains('is-invalid')) {
        container.classList.remove('is-valid');
        return;
    }
    if (!interacted || tags.size === 0) {
        container.classList.remove('is-valid', 'is-invalid');
        return;
    }
    const valid = tags.size <= TAG_LIMIT;
    container.classList.toggle('is-valid', valid);
    if (!valid) {
        container.classList.remove('is-valid');
    }
}

function showInlineTagError(id, message) {
    const container = document.querySelector(`[data-inline-tags-container="${id}"]`);
    const errorEl = document.querySelector(`[data-inline-tags-error="${id}"]`);
    container?.classList.add('is-invalid');
    container?.classList.remove('is-valid');
    if (message) {
        if (errorEl) {
            errorEl.textContent = message;
            errorEl.classList.remove('d-none');
        }
    }
}

function clearInlineTagError(id) {
    const container = document.querySelector(`[data-inline-tags-container="${id}"]`);
    const errorEl = document.querySelector(`[data-inline-tags-error="${id}"]`);
    container?.classList.remove('is-invalid');
    if (errorEl) {
        errorEl.textContent = '';
        errorEl.classList.add('d-none');
    }
    markInlineTagsValid(id);
}

function resetInlineTagValidation(id) {
    const container = document.querySelector(`[data-inline-tags-container="${id}"]`);
    const input = document.querySelector(`[data-inline-tags-input="${id}"]`);
    container?.classList.remove('is-valid', 'is-invalid');
    input?.classList.remove('is-valid', 'is-invalid');
    inlineTagsDirty.set(id, false);
}

function renderInlineTags(id) {
    const listEl = document.querySelector(`[data-inline-tags-list="${id}"]`);
    if (!listEl) return;
    listEl.innerHTML = '';
    const tagSet = inlineTagsState.get(id) || new Set();
    tagSet.forEach(tag => {
        const pill = document.createElement('span');
        pill.className = 'badge rounded-pill bg-body-secondary border text-body-secondary d-inline-flex align-items-center gap-1 px-2 py-1';
        pill.innerHTML = `${escapeHtml(tag)} <button type="button" class="btn btn-sm btn-link p-0 text-secondary" data-inline-tag-remove="${id}" data-tag="${escapeHtml(tag)}"><i class="fa-solid fa-xmark"></i></button>`;
        listEl.appendChild(pill);
    });
    markInlineTagsValid(id);
}

function addInlineTagsFromInput(id) {
    const input = document.querySelector(`[data-inline-tags-input="${id}"]`);
    if (!input) return;
    const raw = input.value || '';
    if (!raw.trim()) return;
    const tagSet = inlineTagsState.get(id) || new Set();
    const parts = raw.split(',').map(p => p.trim()).filter(Boolean);
    const invalid = parts.find(part => !isTagValid(part));
    if (invalid) {
        showInlineTagError(id, TAG_FORMAT_MESSAGE);
        return;
    }
    clearInlineTagError(id);
    let limitHit = false;
    parts.forEach(cleaned => {
        if (tagSet.size < TAG_LIMIT) {
            tagSet.add(cleaned);
            inlineTagsDirty.set(id, true);
        } else {
            limitHit = true;
        }
    });
    inlineTagsState.set(id, tagSet);
    input.value = '';
    if (limitHit) {
        showToast(`You can add up to ${TAG_LIMIT} tags.`, 'warning');
        clearInlineTagError(id);
    }
    renderInlineTags(id);
}

function bindInlineTagInputs(id) {
    const input = document.querySelector(`[data-inline-tags-input="${id}"]`);
    const listEl = document.querySelector(`[data-inline-tags-list="${id}"]`);
    if (input) {
        input.addEventListener('keydown', (e) => {
            if (['Enter', ','].includes(e.key)) {
                e.preventDefault();
                addInlineTagsFromInput(id);
            }
        });
        input.addEventListener('input', () => {
            inlineTagsDirty.set(id, true);
            const value = input.value || '';
            if (value && !/^[A-Za-z0-9_\\-]*$/.test(value.replaceAll(',', ''))) {
                showInlineTagError(id, TAG_FORMAT_MESSAGE);
            } else {
                clearInlineTagError(id);
            }
            const trimmed = value.trim();
            if (trimmed.length >= 2) {
                loadNoteTagSuggestions(trimmed);
            } else if (tagSuggestions) {
                tagSuggestions.innerHTML = '';
            }
        });
        input.addEventListener('blur', () => addInlineTagsFromInput(id));
    }
    if (listEl) {
        listEl.addEventListener('click', (e) => {
            const target = e.target.closest('[data-inline-tag-remove]');
            if (!target) return;
            const tag = target.dataset.tag;
            const set = inlineTagsState.get(id);
            if (set && tag) {
                set.delete(tag);
                renderInlineTags(id);
                inlineTagsDirty.set(id, true);
                clearInlineTagError(id);
            }
        });
    }
}

function resetInlineTags(id, tags) {
    inlineTagsState.set(id, new Set(tags || []));
    resetInlineTagValidation(id);
    renderInlineTags(id);
    bindInlineTagInputs(id);
}

function resetFilterControls() {
    state.filterTags = new Set();
    renderFilterTags();
    clearFilterTagError();
    state.filterColor = '';
    if (filterColorInput) {
        filterColorInput.value = '#2563eb';
    }
    setColorFilterActive(false);
    state.filterPinned = null;
    if (filterPinnedSelect) {
        filterPinnedSelect.value = '';
    }
}

function applyFilterStateFromUi() {
    state.filterColor = (filterColorInput && filterColorInput.dataset.active === 'true')
        ? (filterColorInput.value?.trim() || '')
        : '';
    const pinnedVal = filterPinnedSelect?.value;
    state.filterPinned = pinnedVal === '' ? null : pinnedVal === 'true';
}

function openCreate() {
    state.mode = 'create';
    state.editId = null;
    submitLabel.textContent = 'Save';
    formAlert.classList.add('d-none');
    noteForm.reset();
    if (pinnedInput) pinnedInput.checked = false;
    if (colorInput) colorInput.value = '#2563eb';
    currentTags = new Set();
    tagsDirty = false;
    renderTagsChips();
    clearValidation();
    noteModal.show();
}

function openEdit(id) {
    const note = noteCache.get(id);
    if (!note) return;
    state.mode = 'edit';
    state.editId = id;
    submitLabel.textContent = 'Update';
    formAlert.classList.add('d-none');
    clearValidation();
    titleInput.value = note.title ?? '';
    contentInput.value = note.content ?? '';
    if (pinnedInput) pinnedInput.checked = !!note.pinned;
    if (colorInput) colorInput.value = note.color || '#2563eb';
    currentTags = new Set(note.tags || []);
    tagsDirty = false;
    renderTagsChips();
    noteModal.show();
}

function startInlineEdit(id) {
    const card = document.getElementById(`note-${id}`);
    if (!card) return;
    const cached = noteCache.get(id);
    resetInlineTags(id, cached?.tags || []);
    card.querySelector('.view-mode')?.classList.add('d-none');
    card.querySelector('.edit-mode')?.classList.remove('d-none');
    const titleEl = card.querySelector(`[data-inline-title="${id}"]`);
    const contentEl = card.querySelector(`[data-inline-content="${id}"]`);
    const titleReq = card.querySelector(`[data-inline-title-required="${id}"]`);
    const titleSize = card.querySelector(`[data-inline-title-size="${id}"]`);
    const contentReq = card.querySelector(`[data-inline-content-required="${id}"]`);
    const contentSize = card.querySelector(`[data-inline-content-size="${id}"]`);
    [titleEl, contentEl].forEach(el => {
        if (!el) return;
        el.addEventListener('input', () => {
            if (el === titleEl) {
                toggleInlineMessages(el, titleReq, titleSize, true);
            } else {
                toggleInlineMessages(el, contentReq, contentSize, true);
            }
        }, {once: false});
    });
    toggleInlineMessages(titleEl, titleReq, titleSize, false);
    toggleInlineMessages(contentEl, contentReq, contentSize, false);
}

function cancelInlineEdit(id) {
    const card = document.getElementById(`note-${id}`);
    if (!card) return;
    card.querySelector('.edit-mode')?.classList.add('d-none');
    card.querySelector('.view-mode')?.classList.remove('d-none');
    const original = noteCache.get(id);
    resetInlineTags(id, original?.tags || []);
}

async function saveInlineEdit(id) {
    const card = document.getElementById(`note-${id}`);
    if (!card) return;
    const titleEl = card.querySelector(`[data-inline-title="${id}"]`);
    const contentEl = card.querySelector(`[data-inline-content="${id}"]`);
    const titleReq = card.querySelector(`[data-inline-title-required="${id}"]`);
    const titleSize = card.querySelector(`[data-inline-title-size="${id}"]`);
    const contentReq = card.querySelector(`[data-inline-content-required="${id}"]`);
    const contentSize = card.querySelector(`[data-inline-content-size="${id}"]`);
    const pinnedEl = card.querySelector(`[data-inline-pinned="${id}"]`);
    const colorEl = card.querySelector(`[data-inline-color="${id}"]`);
    const title = titleEl?.value.trim() ?? '';
    const content = contentEl?.value.trim() ?? '';
    const pinned = !!pinnedEl?.checked;
    const color = colorEl?.value?.trim() || null;
    const tags = Array.from(inlineTagsState.get(id) || []);
    const invalidTitle = !title || title.length < 3 || title.length > 255;
    const invalidContent = !content || content.length < 10 || content.length > 1024;
    if (invalidTitle || invalidContent) {
        toggleInlineMessages(titleEl, titleReq, titleSize, true);
        toggleInlineMessages(contentEl, contentReq, contentSize, true);
        return;
    }
    toggleInlineMessages(titleEl, titleReq, titleSize, true);
    toggleInlineMessages(contentEl, contentReq, contentSize, true);
    const res = await handleApi(
        Api.patchNote(id, {title, content, pinned, color, tags}),
        {fallback: 'Update failed'}
    );
    if (!res) return;
    cancelInlineEdit(id);
    await loadNotes();
    showToast('Note updated');
}

function openDelete(id) {
    state.deleteId = id;
    deleteModal.show();
}

async function restoreNote(id) {
    if (!id) return;
    const res = await handleApi(Api.restore(id), {fallback: 'Restore failed'});
    if (!res) return;
    await loadNotes();
    showToast('Note restored');
}

async function restoreRevision(noteId, revisionId) {
    if (!noteId || !revisionId) return;
    const btn = document.querySelector(`[data-revision-restore="${noteId}-${revisionId}"]`);
    const original = btn?.innerHTML;
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Restoring...';
    }
    clearRevisionError();
    const res = await handleApi(
        Api.restoreRevision(noteId, revisionId),
        {
            fallback: 'Restore failed',
            onError: (_, msg) => {
                showRevisionError(msg);
                showToast(msg, 'danger');
            },
            silent: true
        }
    );
    if (btn) {
        btn.disabled = false;
        btn.innerHTML = original ?? 'Restore';
    }
    if (!res) return;
    revisionModal?.hide();
    await loadNotes();
    showToast(`Restored to revision #${revisionId}`, 'success');
}

function openDeleteForever(id) {
    deleteForeverId = id;
    deleteForeverModal?.show();
}

async function deleteForever() {
    if (!deleteForeverId) return;
    const originalText = deleteForeverLabel?.textContent;
    confirmDeleteForeverBtn.disabled = true;
    deleteForeverSpinner?.classList.remove('d-none');
    if (deleteForeverLabel) {
        deleteForeverLabel.textContent = 'Deleting...';
    }
    const res = await handleApi(
        Api.deletePermanent(deleteForeverId),
        {fallback: 'Permanent delete failed'}
    );
    confirmDeleteForeverBtn.disabled = false;
    deleteForeverSpinner?.classList.add('d-none');
    if (deleteForeverLabel) {
        deleteForeverLabel.textContent = originalText || 'Delete';
    }
    const id = deleteForeverId;
    deleteForeverId = null;
    if (!res) return;
    deleteForeverModal.hide();
    await loadNotes();
    showToast('Note permanently deleted', 'danger', null, 'Permanent delete');
}

async function togglePin(id) {
    const note = noteCache.get(id);
    if (!note) return;
    const res = await handleApi(
        Api.patchNote(id, {pinned: !note.pinned}),
        {fallback: 'Pin update failed'}
    );
    if (!res) return;
    await loadNotes();
    showToast(note.pinned ? 'Note unpinned' : 'Note pinned', 'success');
}

async function copyNote(id) {
    const note = noteCache.get(id);
    if (!note) return;
    const textToCopy = `${note.title}\n\n${note.content}`;
    try {
        await navigator.clipboard.writeText(textToCopy);
        showToast('Content copied to clipboard', 'success');
    } catch (e) {
        showToast('Copy failed', 'danger');
    }
}

function openEmptyTrashModal() {
    if (!emptyTrashModal) return;
    emptyTrashModal.show();
}

async function emptyTrash() {
    if (!emptyTrashBtn) return;
    const originalText = emptyTrashConfirmLabel?.textContent;
    confirmEmptyTrashBtn.disabled = true;
    emptyTrashConfirmSpinner?.classList.remove('d-none');
    if (emptyTrashConfirmLabel) {
        emptyTrashConfirmLabel.textContent = 'Emptying...';
    }
    const res = await handleApi(Api.emptyTrash(), {fallback: 'Failed to empty trash'});
    confirmEmptyTrashBtn.disabled = false;
    emptyTrashConfirmSpinner?.classList.add('d-none');
    if (emptyTrashConfirmLabel) {
        emptyTrashConfirmLabel.textContent = originalText || 'Empty Trash';
    }
    updateEmptyTrashButton();
    if (!res) return;
    emptyTrashModal.hide();
    showToast('Trash emptied', 'success');
    await loadNotes();
}

async function saveNote(e) {
    e.preventDefault();
    formAlert.classList.add('d-none');
    const title = titleInput.value.trim();
    const content = contentInput.value.trim();
    const color = colorInput?.value?.trim() || null;
    const tags = Array.from(currentTags);
    const payload = {title, content, pinned: !!pinnedInput?.checked, color, tags};

    if (currentTags.size > TAG_LIMIT) {
        showToast(`You can add up to ${TAG_LIMIT} tags.`, 'warning');
        return;
    }
    toggleSizeMessages(titleInput);
    toggleSizeMessages(contentInput);
    titleInput.classList.toggle('is-invalid', !titleInput.checkValidity());
    contentInput.classList.toggle('is-invalid', !contentInput.checkValidity());
    titleInput.classList.remove('is-valid');
    contentInput.classList.remove('is-valid');
    pinnedInput?.classList.remove('is-valid', 'is-invalid');

    if (!noteForm.checkValidity()) {
        return;
    }

    const prevText = submitLabel.textContent;
    const prevDisabled = saveBtn?.disabled;
    if (saveBtn) {
        saveBtn.disabled = true;
        submitLabel.textContent = state.mode === 'edit' ? 'Updating...' : 'Saving...';
        saveSpinner?.classList.remove('d-none');
    }

    const isEdit = state.mode === 'edit' && state.editId;
    if (!isEdit) {
        state.page = 0; // new note: jump to first page
    }
    const url = isEdit ? `/api/notes/${state.editId}` : '/api/notes';
    const method = isEdit ? 'PUT' : 'POST';

    const res = await handleApi(
        isEdit
            ? Api.updateNote(state.editId, payload)
            : Api.createNote(payload),
        {
            fallback: 'Request failed',
            onError: (e, msg) => {
                const violations = extractViolations(e);
                const details = violations.length
                    ? '<ul class="mb-0">' + violations.map(v => `<li>${escapeHtml(v.message || '')}</li>`).join('') + '</ul>'
                    : '';
                formAlert.innerHTML = `${escapeHtml(msg)}${details}`;
                formAlert.classList.remove('d-none');
            },
            silent: true
        }
    );
    if (saveBtn) {
        saveBtn.disabled = prevDisabled;
        submitLabel.textContent = prevText;
        saveSpinner?.classList.add('d-none');
    }
    if (!res) return;
    noteModal.hide();
    clearSelection();
    await loadNotes();
    showToast(isEdit ? 'Note updated' : 'Note created');
}

async function confirmDelete() {
    if (!state.deleteId) return;
    const deleteBtn = document.getElementById('confirmDeleteBtn');
    const deleteSpinner = document.getElementById('deleteSpinner');
    const deleteLabel = document.getElementById('deleteLabel');
    const originalText = deleteLabel?.textContent;
    try {
        if (deleteBtn) {
            deleteBtn.disabled = true;
            deleteLabel.textContent = 'Deleting...';
            deleteSpinner?.classList.remove('d-none');
        }
        const res = await handleApi(Api.softDelete(state.deleteId), {fallback: 'Delete failed'});
        const deletedId = state.deleteId;
        deleteModal.hide();
        if (res) {
            await loadNotes();
            showToast('Note deleted', 'danger', {
                label: 'Undo',
                handler: () => restoreNote(deletedId)
            });
        }
    } catch (e) {
        showToast(getErrorMessage(e, 'Delete failed'), 'danger');
    } finally {
        if (deleteBtn) {
            deleteBtn.disabled = false;
            deleteLabel.textContent = originalText ?? 'Delete';
            deleteSpinner?.classList.add('d-none');
        }
    }
}

function changePage(page) {
    if (page < 0 || page > state.totalPages - 1) return;
    state.page = page;
    loadNotes();
}

pageSize.addEventListener('change', () => {
    state.size = Number.parseInt(pageSize.value, 10) || 10;
    state.page = 0;
    loadNotes();
});

if (sortSelect) {
    sortSelect.addEventListener('change', () => {
        state.sort = sortSelect.value || defaultSort;
        state.page = 0;
        loadNotes();
    });
}

if (searchInput) {
    const triggerSearch = debounce(() => {
        state.query = searchInput.value.trim();
        state.page = 0;
        loadNotes();
    }, 300);
    searchInput.addEventListener('input', triggerSearch);
    searchInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            triggerSearch();
        }
    });
}

if (searchClear) {
    searchClear.addEventListener('click', () => {
        searchInput.value = '';
        state.query = '';
        state.page = 0;
        loadNotes();
    });
}

if (filterTagsInput) {
    filterTagsInput.addEventListener('keydown', (e) => {
        if (['Enter', ','].includes(e.key)) {
            e.preventDefault();
            addFilterTagsFromInput();
        }
    });
    filterTagsInput.addEventListener('input', () => {
        const raw = filterTagsInput.value || '';
        if (raw.includes(',')) {
            addFilterTagsFromInput();
            return;
        }
        if (raw.trim().length > 0 && !TAG_PATTERN.test(raw.trim())) {
            showFilterTagError(TAG_FORMAT_MESSAGE);
        } else {
            clearFilterTagError();
        }
        const trimmed = raw.trim();
        if (trimmed.length >= 2) {
            loadTagSuggestions(trimmed);
        } else {
            if (filterTagsSuggestions) {
                filterTagsSuggestions.innerHTML = '';
            }
        }
    });
    filterTagsInput.addEventListener('blur', addFilterTagsFromInput);
}

if (filterTagsList) {
    filterTagsList.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-filter-tag-remove]');
        if (!btn) return;
        const tag = btn.dataset.filterTagRemove;
        if (tag) {
            state.filterTags.delete(tag);
            renderFilterTags();
        }
    });
}

if (filterColorInput) {
    setColorFilterActive(false);
    filterColorInput.addEventListener('input', () => setColorFilterActive(true));
}
if (clearColorFilterBtn) {
    clearColorFilterBtn.addEventListener('click', () => {
        state.filterColor = '';
        if (filterColorInput) {
            filterColorInput.value = '#2563eb';
        }
        setColorFilterActive(false);
    });
}
if (filterPinnedSelect) {
    filterPinnedSelect.addEventListener('change', () => {
        // lazy update, actual apply when hitting Apply
    });
}
if (applyFiltersBtn) {
    applyFiltersBtn.addEventListener('click', () => {
        applyFilterStateFromUi();
        state.page = 0;
        loadNotes();
    });
}
if (resetFiltersBtn) {
    resetFiltersBtn.addEventListener('click', () => {
        resetFilterControls();
        state.page = 0;
        loadNotes();
    });
}

if (signOutBtn) {
    signOutBtn.addEventListener('click', handleLogout);
}

if (activeViewBtn) {
    activeViewBtn.addEventListener('click', () => {
        state.page = 0;
        switchView('active');
    });
}

if (trashViewBtn) {
    trashViewBtn.addEventListener('click', () => {
        state.page = 0;
        switchView('trash');
    });
}

if (emptyTrashBtn) {
    emptyTrashBtn.addEventListener('click', openEmptyTrashModal);
}
if (confirmEmptyTrashBtn) {
    confirmEmptyTrashBtn.addEventListener('click', emptyTrash);
}
if (confirmDeleteForeverBtn) {
    confirmDeleteForeverBtn.addEventListener('click', deleteForever);
}
if (selectAllCheckbox) {
    selectAllCheckbox.addEventListener('change', () => {
        const ids = Array.from(noteCache.keys());
        if (selectAllCheckbox.checked) {
            if (state.selected.size >= BULK_LIMIT) {
                showToast(`You can select up to ${BULK_LIMIT} notes for bulk actions.`, 'warning');
                selectAllCheckbox.checked = false;
            } else {
                for (const id of ids) {
                    if (state.selected.size >= BULK_LIMIT) {
                        showToast(`You can select up to ${BULK_LIMIT} notes for bulk actions.`, 'warning');
                        break;
                    }
                    state.selected.add(id);
                }
            }
        } else {
            ids.forEach(id => state.selected.delete(id));
        }
        syncCheckboxStates();
        syncSelectAllCheckbox();
        updateBulkButtons();
    });
}
if (bulkDeleteBtn) {
    bulkDeleteBtn.addEventListener('click', () => openBulkModal('DELETE_SOFT'));
}
if (bulkRestoreBtn) {
    bulkRestoreBtn.addEventListener('click', () => openBulkModal('RESTORE'));
}
if (bulkDeleteForeverBtn) {
    bulkDeleteForeverBtn.addEventListener('click', () => openBulkModal('DELETE_FOREVER'));
}
if (confirmBulkBtn) {
    confirmBulkBtn.addEventListener('click', performBulkAction);
}

addNoteBtn.addEventListener('click', openCreate);
noteForm.addEventListener('submit', saveNote);
if (tagsInput) {
    tagsInput.addEventListener('keydown', (e) => {
        if (['Enter', ','].includes(e.key)) {
            e.preventDefault();
            addTagsFromInput();
        }
    });
    tagsInput.addEventListener('input', () => {
        const raw = tagsInput.value || '';
        if (raw.includes(',')) {
            addTagsFromInput();
            return;
        }
        const trimmed = raw.trim();
        if (trimmed.length > 0 && !TAG_PATTERN.test(trimmed)) {
            showTagError(TAG_FORMAT_MESSAGE);
        } else {
            clearTagError();
        }
        if (trimmed.length >= 2) {
            loadNoteTagSuggestions(trimmed);
        } else if (tagSuggestions) {
            tagSuggestions.innerHTML = '';
        }
        if (trimmed.length > 0) {
            tagsDirty = true;
            markTagsValid();
        }
    });
    tagsInput.addEventListener('blur', addTagsFromInput);
}
if (tagsListEl) {
    tagsListEl.addEventListener('click', (e) => {
        const btn = e.target.closest('.remove-tag');
        if (!btn) return;
        const tag = btn.dataset.tag;
        currentTags.delete(tag);
        renderTagsChips();
        clearTagError();
        tagsDirty = true;
        markTagsValid();
    });
}
[titleInput, contentInput].forEach(input => {
    input.addEventListener('input', () => {
        const valid = input.checkValidity();
        input.classList.toggle('is-invalid', !valid);
        input.classList.toggle('is-valid', valid);
        toggleSizeMessages(input);
    });
});
document.getElementById('confirmDeleteBtn').addEventListener('click', confirmDelete);

if (pagination) {
    pagination.addEventListener('click', (e) => {
        const target = e.target.closest('[data-page]');
        if (!target) return;
        e.preventDefault();
        const page = Number.parseInt(target.dataset.page, 10);
        if (Number.isNaN(page)) return;
        changePage(page);
    });
}

if (ownerInput) {
    ownerInput.addEventListener('input', handleOwnerInput);
}
if (ownerSuggestions) {
    ownerSuggestions.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-username]');
        if (!btn || !ownerInput) return;
        ownerInput.value = btn.dataset.username || '';
        handleOwnerInput();
    });
    ownerSuggestions.addEventListener('scroll', () => {
        if (!ownerSearchHasMore || ownerSearchLoading) return;
        const threshold = 40;
        if (ownerSuggestions.scrollTop + ownerSuggestions.clientHeight >= ownerSuggestions.scrollHeight - threshold) {
            loadOwnerSuggestions(ownerSearchQuery, true);
        }
    });
}
if (ownerForm) {
    ownerForm.addEventListener('submit', submitOwnerChange);
}
if (ownerLoadMoreBtn) {
    ownerLoadMoreBtn.addEventListener('click', (e) => {
        e.preventDefault();
        if (!ownerSearchHasMore) return;
        loadOwnerSuggestions(ownerSearchQuery, true);
    });
}
if (ownerModalEl) {
    ownerModalEl.addEventListener('hidden.bs.modal', () => {
        ownerNoteId = null;
        clearOwnerModal();
    });
}
if (shareForm) {
    shareForm.addEventListener('submit', submitShare);
}
if (copyShareLinkBtn) {
    copyShareLinkBtn.addEventListener('click', copyShareLink);
}
if (shareModalEl) {
    if (window.bootstrap?.Modal) {
        shareModal = window.bootstrap.Modal.getOrCreateInstance(shareModalEl);
        shareModalEl.addEventListener('hidden.bs.modal', resetShareModal);
    } else {
        shareModalEl.addEventListener('hidden.bs.modal', resetShareModal);
    }
}
if (shareExpirySelect) {
    shareExpirySelect.addEventListener('change', handleShareExpiryChange);
}
if (refreshShareLinksBtn) {
    refreshShareLinksBtn.addEventListener('click', () => loadShareLinks(false));
}
if (shareLinksLoadMoreBtn) {
    shareLinksLoadMoreBtn.addEventListener('click', () => loadShareLinks(true));
}
if (shareLinksList) {
    shareLinksList.addEventListener('click', handleShareLinksClick);
}

Ui.bindNoteGridActions(noteGrid, {
    'restore': restoreNote,
    'copy': copyNote,
    'delete-forever': openDeleteForever,
    'toggle-pin': togglePin,
    'edit-modal': openEdit,
    'inline-edit': startInlineEdit,
    'delete': openDelete,
    'revisions': openRevisionModal,
    'share-links': openShareLinksOnly,
    'inline-cancel': cancelInlineEdit,
    'inline-save': saveInlineEdit,
    'change-owner': openOwnerModal,
    'share': openShareModal
});

Ui.bindRevisionActions(revisionList, (noteId, revId) => restoreRevision(noteId, revId));
if (revisionList) {
    revisionList.addEventListener('click', (e) => {
        const diffBtn = e.target.closest('[data-action="revision-diff"]');
        if (diffBtn) {
            const idx = Number.parseInt(diffBtn.dataset.revIndex, 10);
            showRevisionDiff(idx);
            return;
        }
        const hideBtn = e.target.closest('[data-action="hide-diff"]');
        if (hideBtn) {
            const revId = hideBtn.dataset.revId;
            const block = revisionList.querySelector(`[data-diff-block="${revId}"]`);
            if (block) block.classList.add('d-none');
            return;
        }
        const loadMore = e.target.closest('[data-action="revision-load-more"]');
        if (loadMore) {
            const noteId = Number.parseInt(loadMore.dataset.noteId, 10);
            const row = loadMore.closest('.list-group-item');
            if (row) {
                row.querySelector('[data-revision-load-spinner="true"]')?.classList.remove('d-none');
                const btnEl = row.querySelector('button');
                if (btnEl) btnEl.disabled = true;
            }
            loadRevisionPage(noteId, true).finally(() => {
                if (row) {
                    row.querySelector('[data-revision-load-spinner="true"]')?.classList.add('d-none');
                    const btnEl = row.querySelector('button');
                    if (btnEl) btnEl.disabled = false;
                }
            });

        }
    });
}

updateEmptyTrashButton();
resetFilterControls();
renderFilterTags();
(async () => {
    const authenticated = await bootstrapAuth();
    if (!authenticated) {
        return;
    }
    loadNotes();
})();

// Initialize auditor using shared State helper
// Auditor input removed; JWT user used for auditing
