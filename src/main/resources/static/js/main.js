import Helpers from '/js/helpers.js';
import State from '/js/state.js';
import Api from '/js/api.js';
import Render from '/js/render.js';
import Ui from '/js/ui.js';
import Validation from '/js/validation.js';
import Diff from '/js/diff.js';

const { state, currentAuditor, currentToken, saveToken, clearToken, currentUsername, setCurrentUser } = State;
const { escapeHtml, formatDate, showToast, debounce } = Helpers;
const { renderTags, revisionTypeBadge } = Render;
const { toggleSizeMessages, toggleInlineMessages } = Validation;
    const { diffLines, diffLinesDetailed } = Diff;

    // Use state from module
    const BULK_LIMIT = 100;
    const TAG_LIMIT = 5;
    const FILTER_TAG_LIMIT = 5;
    const THEME_KEY = 'theme';

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
    const signOutBtn = document.getElementById('signOutBtn');
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

    function resetRevisionState() {
        revisionCache = [];
        revisionPage = 0;
        revisionHasMore = false;
        revisionTotal = 0;
        isLoadingRevisions = false;
        revisionNoteId = null;
    }

    const noteCache = new Map();
    const defaultSort = 'createdDate,desc';
    state.sort = defaultSort;
    state.selected = new Set();
    let deleteForeverId = null;

    if (addNoteBtn) {
        addNoteBtn.disabled = state.view === 'trash';
    }

    initTheme();
    if (themeToggle) {
        themeToggle.addEventListener('click', toggleTheme);
    }

    function redirectToLogin() {
        window.location.href = '/login.html';
    }

    function systemPrefersDark() {
        return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    }

    function getStoredTheme() {
        try {
            return localStorage.getItem(THEME_KEY);
        } catch (e) {
            return null;
        }
    }

    function storeTheme(theme) {
        try {
            localStorage.setItem(THEME_KEY, theme);
        } catch (e) {
            // ignore
        }
    }

    function applyTheme(theme) {
        const next = theme === 'dark' ? 'dark' : 'light';
        document.documentElement.setAttribute('data-bs-theme', next);
        if (themeToggleIcon) {
            themeToggleIcon.classList.toggle('fa-moon', next === 'light');
            themeToggleIcon.classList.toggle('fa-sun', next === 'dark');
        }
        if (themeToggleLabel) {
            themeToggleLabel.textContent = next === 'dark' ? 'Light' : 'Dark';
        }
        if (themeToggle) {
            themeToggle.setAttribute('aria-pressed', next === 'dark');
            themeToggle.setAttribute('aria-label', `Switch to ${next === 'dark' ? 'light' : 'dark'} mode`);
        }
    }

    function setColorFilterActive(active) {
        if (filterColorInput) {
            filterColorInput.dataset.active = active ? 'true' : 'false';
        }
    }

    function initTheme() {
        const attrTheme = document.documentElement.getAttribute('data-bs-theme');
        const stored = getStoredTheme();
        const prefers = systemPrefersDark() ? 'dark' : 'light';
        const initial = attrTheme || stored || prefers;
        applyTheme(initial);
    }

    function toggleTheme() {
        const current = document.documentElement.getAttribute('data-bs-theme') === 'dark' ? 'dark' : 'light';
        const next = current === 'dark' ? 'light' : 'dark';
        applyTheme(next);
        storeTheme(next);
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
        return !!currentToken();
    }

    function updateAuthUi() {
        const username = currentUsername();
        const signedIn = isAuthenticated();
        if (authBtnLabel) {
            authBtnLabel.textContent = signedIn ? (username || 'Signed in') : 'Sign in';
        }
        if (authUserLabel) {
            authUserLabel.textContent = signedIn ? `Signed in as ${username || ''}` : 'Not signed in';
        }
        if (signOutBtn) {
            signOutBtn.classList.toggle('d-none', !signedIn);
        }
        if (authMenu) {
            authMenu.classList.toggle('d-none', !signedIn);
        }
        if (authBtn) {
            authBtn.classList.toggle('dropdown-toggle', signedIn);
            if (signedIn) {
                authBtn.setAttribute('data-bs-toggle', 'dropdown');
            } else {
                authBtn.removeAttribute('data-bs-toggle');
            }
        }
    }

    function handleUnauthorized(message = 'Authentication required') {
        clearToken();
        updateAuthUi();
        showToast(message, 'warning');
    }

    async function handleLoginSubmit(event) {
        event.preventDefault();
        if (!authForm) return;
        // Not used; login handled on separate page
    }

    function handleLogout(event) {
        event?.preventDefault();
        clearToken();
        updateAuthUi();
        showToast('Signed out', 'info');
        window.location.href = '/login.html';
    }

    async function bootstrapAuth() {
        updateAuthUi();
        if (!isAuthenticated()) {
            redirectToLogin();
            return;
        }
        const me = await handleApi(Api.currentUser(), {
            silent: true,
            onError: () => {
                clearToken();
                updateAuthUi();
                showToast('Session expired. Please sign in again.', 'warning');
                redirectToLogin();
            }
        });
        if (me?.username) {
            setCurrentUser(me);
            updateAuthUi();
        }
    }

    async function handleApi(promise, options = {}) {
        const { fallback = 'Request failed', onError, onFinally, silent } = options;
        try {
            return await promise;
        } catch (e) {
            if (e?.status === 401) {
                handleUnauthorized('Session expired or unauthorized');
                return null;
            }
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
        const notes = data?.content || [];
        const meta = data?.page ?? data;
        state.total = meta?.totalElements ?? notes.length;
        if (notes.length === 0) {
            totalLabel.hidden = true;
        } else {
            totalLabel.hidden = false;
            totalLabel.textContent = `Total: ${state.total}`;
            totalLabel.classList.remove('invisible');
        }
        const totalPages = meta?.totalPages ?? 1;
        const current = meta?.number ?? 0;
        state.page = current;
        state.totalPages = totalPages;
        updateEmptyTrashButton();
        if (pageInfo) {
            pageInfo.textContent = `Page ${current + 1} of ${Math.max(totalPages, 1)}`;
            pageInfo.hidden = false;
        }

        if (!notes.length) {
            const emptyMsg = state.query
                ? 'No notes match your search.'
                : (state.view === 'trash' ? 'Trash is empty.' : 'No notes found. Create a new one to get started.');
            const emptyIcon = state.query
                ? 'fa-magnifying-glass'
                : (state.view === 'trash' ? 'fa-trash-can' : 'fa-note-sticky');
            totalLabel.hidden = true;
            noteGrid.innerHTML = `
                <div class="col-12 text-center text-muted py-4 d-flex flex-column align-items-center gap-2">
                    <i class="fa-solid ${emptyIcon}" style="font-size: 2rem;"></i>
                    <div>${emptyMsg}</div>
                </div>`;
            bulkRow?.classList.add('d-none');
            controlsRow?.classList.add('d-none');
            clearSelection();
            pager.hidden = true;
            pagination.innerHTML = '';
            if (pageInfo) {
                pageInfo.hidden = true;
            }
            return;
        }

        bulkRow?.classList.remove('d-none');
        controlsRow?.classList.remove('d-none');
        const fragments = [];
        notes.forEach(note => {
            noteCache.set(note.id, note);
            const creator = note.createdBy ?? '';
            const updater = note.lastModifiedBy ?? '';
            const createdText = formatDate(note.createdDate);
            const modifiedText = note.lastModifiedDate ? formatDate(note.lastModifiedDate) : createdText;
            const deletedBy = note.deletedBy ?? '';
            const deletedText = note.deletedDate ? formatDate(note.deletedDate) : '';

            if (state.view === 'trash') {
                fragments.push(`
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
                                    <button class="btn btn-outline-info btn-sm" data-action="revisions" data-id="${note.id}" title="Revision history">
                                        <i class="fa-solid fa-clock-rotate-left"></i>
                                    </button>
                                    <button class="btn btn-outline-danger btn-sm" data-action="delete-forever" data-id="${note.id}" title="Delete permanently">
                                        <i class="fa-solid fa-trash"></i>
                                    </button>
                                </div>
                            </div>
                            <div class="d-flex flex-column gap-1 text-muted small">
                                <span><i class="fa-solid fa-user me-1"></i>Created by: ${escapeHtml(creator)}</span>
                                ${updater ? `<span><i class="fa-solid fa-user-pen me-1"></i>Updated by: ${escapeHtml(updater)}</span>` : ''}
                            </div>
                            <div class="d-flex flex-column text-muted small gap-1">
                                <div class="d-flex align-items-center gap-2 flex-wrap">
                                    <i class="fa-regular fa-calendar me-1"></i>
                                    <span>Created:</span>
                                    <span class="text-nowrap">${escapeHtml(createdText.split(' ')[0] ?? createdText)}</span>
                                    <span class="d-inline-flex align-items-center gap-1 text-nowrap"><i class="fa-regular fa-clock"></i>${escapeHtml(createdText.split(' ')[1] ?? '')}</span>
                                </div>
                                <div class="d-flex align-items-center gap-2 flex-wrap">
                                    <i class="fa-regular fa-calendar-check me-1"></i>
                                    <span>Updated:</span>
                                    <span class="text-nowrap">${escapeHtml(modifiedText.split(' ')[0] ?? modifiedText)}</span>
                                    <span class="d-inline-flex align-items-center gap-1 text-nowrap"><i class="fa-regular fa-clock"></i>${escapeHtml(modifiedText.split(' ')[1] ?? '')}</span>
                                </div>
                            </div>
                            <div class="d-flex gap-3 text-muted small">
                                <span><i class="fa-solid fa-ban me-1"></i>Deleted by: ${escapeHtml(deletedBy || '—')}</span>
                            </div>
                                ${deletedText ? `
                                <div class="d-flex text-muted small align-items-center gap-2 flex-wrap mt-1">
                                    <i class="fa-regular fa-calendar me-1"></i>
                                    <span>Deleted:</span>
                                    <span class="text-nowrap">${escapeHtml(deletedText.split(' ')[0] ?? deletedText)}</span>
                                    <span class="d-inline-flex align-items-center gap-1 text-nowrap"><i class="fa-regular fa-clock"></i>${escapeHtml(deletedText.split(' ')[1] ?? '')}</span>
                                </div>
                            ` : ''}
                        </div>
                    </div>
                </div>
            `);
            } else {
                fragments.push(`
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
                                <div class="d-flex flex-wrap gap-1 justify-content-end">
                                    <button class="btn btn-outline-warning btn-sm" data-action="toggle-pin" data-id="${note.id}" title="${note.pinned ? 'Unpin' : 'Pin'}">
                                        <i class="fa-solid fa-thumbtack ${note.pinned ? '' : 'opacity-50'}"></i>
                                    </button>
                                    <button class="btn btn-outline-primary btn-sm" data-action="edit-modal" data-id="${note.id}" title="Edit in modal">
                                        <i class="fa-solid fa-pen-to-square"></i>
                                    </button>
                                    <button class="btn btn-outline-secondary btn-sm" data-action="inline-edit" data-id="${note.id}" title="Inline edit">
                                        <i class="fa-solid fa-pen"></i>
                                    </button>
                                    <button class="btn btn-outline-secondary btn-sm" data-action="copy" data-id="${note.id}" title="Copy content">
                                        <i class="fa-solid fa-copy"></i>
                                    </button>
                                    <button class="btn btn-outline-info btn-sm" data-action="revisions" data-id="${note.id}" title="Revision history">
                                        <i class="fa-solid fa-clock-rotate-left"></i>
                                    </button>
                                    <button class="btn btn-outline-danger btn-sm" data-action="delete" data-id="${note.id}" title="Delete">
                                        <i class="fa-solid fa-trash"></i>
                                    </button>
                                </div>
                            </div>
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
                            <div class="d-flex flex-column gap-1 text-muted small">
                                <span><i class="fa-solid fa-user me-1"></i>Created by: ${escapeHtml(creator)}</span>
                                ${updater ? `<span><i class="fa-solid fa-user-pen me-1"></i>Updated by: ${escapeHtml(updater)}</span>` : ''}
                            </div>
                            <div class="d-flex flex-column text-muted small gap-1">
                                <div class="d-flex align-items-center gap-2 flex-wrap">
                                    <i class="fa-regular fa-calendar me-1"></i>
                                    <span>Created:</span>
                                    <span class="text-nowrap">${escapeHtml(createdText.split(' ')[0] ?? createdText)}</span>
                                    <span class="d-inline-flex align-items-center gap-1 text-nowrap"><i class="fa-regular fa-clock"></i>${escapeHtml(createdText.split(' ')[1] ?? '')}</span>
                                </div>
                                <div class="d-flex align-items-center gap-2 flex-wrap">
                                    <i class="fa-regular fa-calendar-check me-1"></i>
                                    <span>Updated:</span>
                                    <span class="text-nowrap">${escapeHtml(modifiedText.split(' ')[0] ?? modifiedText)}</span>
                                    <span class="d-inline-flex align-items-center gap-1 text-nowrap"><i class="fa-regular fa-clock"></i>${escapeHtml(modifiedText.split(' ')[1] ?? '')}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            `);
            }
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

    function bindSelectionCheckboxes() {
        document.querySelectorAll('.selection-checkbox').forEach(cb => {
            cb.addEventListener('change', (e) => {
                const id = parseInt(e.target.getAttribute('data-note-id'), 10);
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
        const displayLocal = revisionLocalNumber(index, localNumber ?? rev.versionNumber);
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

    function renderTagsDiff(oldTags, newTags) {
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

        const badges = [
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
        const lcs = Array.from({ length: m + 1 }, () => Array(n + 1).fill(0));
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
                ops.push({ type: 'eq', value: a[i] });
                i++; j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                ops.push({ type: 'del', value: a[i] });
                i++;
            } else {
                ops.push({ type: 'add', value: b[j] });
                j++;
            }
        }
        while (i < m) ops.push({ type: 'del', value: a[i++] });
        while (j < n) ops.push({ type: 'add', value: b[j++] });
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
                segments.push({ type: op.type, value: op.value });
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
        const currentLocal = revisionLocalNumber(index, current?.versionNumber);
        const prevLocal = prev ? revisionLocalNumber(index + 1, prev.versionNumber) : null;
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
            const localNumber = revisionLocalNumber(globalIndex, rev.versionNumber);
            return renderRevisionItem(rev, noteId, globalIndex, localNumber);
        }).join('');
        revisionList.insertAdjacentHTML('beforeend', html);
        const loadMoreRow = revisionList.querySelector('[data-action="revision-load-more"]')?.parentElement;
        if (loadMoreRow) {
            loadMoreRow.remove();
        }
        if (revisionHasMore) {
            revisionList.insertAdjacentHTML('beforeend', `
                <div class="list-group-item text-center">
                    <button class="btn btn-outline-secondary btn-sm" data-action="revision-load-more" data-note-id="${noteId}">
                        Load more
                    </button>
                </div>`);
        }
    }

    async function loadRevisionPage(noteId, append = false) {
        if (!revisionList || !revisionNoteId) return;
        if (revisionNoteId !== noteId) return;
        if (isLoadingRevisions || (!revisionHasMore && append)) return;
        if (!append) {
            revisionList.innerHTML = '';
        }
        clearRevisionError();
        isLoadingRevisions = true;
        const activeNoteId = revisionNoteId;
        revisionSpinner?.classList.remove('d-none');
        try {
            const pageData = await Api.fetchRevisions(noteId, revisionPage, revisionPageSize);
            const content = pageData?.content ?? pageData ?? [];
            const meta = pageData?.page ?? pageData;
            const totalElements = meta?.totalElements;
            const startIndex = revisionCache.length;
            revisionCache = revisionCache.concat(content);
            if (typeof totalElements === 'number') {
                revisionTotal = totalElements;
            } else if (!append) {
                revisionTotal = content.length;
            } else {
                revisionTotal = Math.max(revisionTotal, revisionCache.length);
            }
            const totalPages = meta?.totalPages;
            revisionHasMore = typeof totalPages === 'number'
                ? revisionPage + 1 < totalPages
                : Boolean(content.length);
            if (revisionNoteId === activeNoteId) {
                renderRevisionItems(content, noteId, startIndex, append);
                revisionPage += 1;
            }
        } catch (e) {
            showRevisionError(getErrorMessage(e, 'Failed to load revisions'));
        } finally {
            if (revisionNoteId === activeNoteId) {
                revisionSpinner?.classList.add('d-none');
                isLoadingRevisions = false;
            }
        }
    }

    async function openRevisionModal(noteId) {
        if (!revisionModal) return;
        revisionNoteId = noteId;
        const cached = noteCache.get(noteId);
        if (revisionModalTitle) {
            const title = cached?.title ? `${cached.title} · #${noteId}` : `Revisions · #${noteId}`;
            revisionModalTitle.textContent = title;
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
            const id = parseInt(cb.getAttribute('data-note-id'), 10);
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
        const result = await handleApi(Api.bulkAction({ action, ids }), {
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
                const tag = target.getAttribute('data-tag');
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
            }, { once: false });
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
            { fallback: 'Update failed' }
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
        const res = await handleApi(Api.restore(id), { fallback: 'Restore failed' });
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
            { fallback: 'Permanent delete failed' }
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
            Api.patchNote(id, { pinned: !note.pinned }),
            { fallback: 'Pin update failed' }
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
        const res = await handleApi(Api.emptyTrash(), { fallback: 'Failed to empty trash' });
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
        const res = await handleApi(Api.softDelete(state.deleteId), { fallback: 'Delete failed' });
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
        state.size = parseInt(pageSize.value, 10) || 10;
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
            const tag = btn.getAttribute('data-filter-tag-remove');
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
    if (authBtn) {
        authBtn.addEventListener('click', (e) => {
            if (!isAuthenticated()) {
                e.preventDefault();
                window.location.href = '/login.html';
            }
        });
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
            const tag = btn.getAttribute('data-tag');
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
            const page = parseInt(target.getAttribute('data-page'), 10);
            if (Number.isNaN(page)) return;
            changePage(page);
        });
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
        'inline-cancel': cancelInlineEdit,
        'inline-save': saveInlineEdit
    });

    Ui.bindRevisionActions(revisionList, (noteId, revId) => restoreRevision(noteId, revId));
    if (revisionList) {
        revisionList.addEventListener('click', (e) => {
            const diffBtn = e.target.closest('[data-action="revision-diff"]');
            if (diffBtn) {
                const idx = parseInt(diffBtn.getAttribute('data-rev-index'), 10);
                showRevisionDiff(idx);
                return;
            }
            const hideBtn = e.target.closest('[data-action="hide-diff"]');
            if (hideBtn) {
                const revId = hideBtn.getAttribute('data-rev-id');
                const block = revisionList.querySelector(`[data-diff-block="${revId}"]`);
                if (block) block.classList.add('d-none');
                return;
            }
            const loadMore = e.target.closest('[data-action="revision-load-more"]');
            if (loadMore) {
                const noteId = parseInt(loadMore.getAttribute('data-note-id'), 10);
                loadRevisionPage(noteId, true);
                return;
            }
        });
    }

    updateEmptyTrashButton();
    resetFilterControls();
    renderFilterTags();
    loadTagSuggestions('');
    bootstrapAuth();
    loadNotes();

    // Initialize auditor using shared State helper
    // Auditor input removed; JWT user used for auditing
