import Api from './api.js';
import Theme from './theme.js';
import State from './state.js';

const shareToken = extractTokenFromPath() || new URLSearchParams(globalThis.location.search).get('share_token');
let noteId = null;
const alertBox = document.getElementById('alert');
const card = document.getElementById('card');
const spinner = document.getElementById('shareSpinner');
const titleEl = document.getElementById('title');
const contentEl = document.getElementById('content');
const tagsEl = document.getElementById('tags');
const colorDot = document.getElementById('colorDot');
const ownerText = document.getElementById('ownerText');
const createdByText = document.getElementById('createdByText');
const updatedByText = document.getElementById('updatedByText');
const createdDateEl = document.getElementById('createdDate');
const updatedDateEl = document.getElementById('updatedDate');
const createdTimeEl = document.getElementById('createdTime');
const updatedTimeEl = document.getElementById('updatedTime');
const pinnedIcon = document.getElementById('pinnedIcon');
const themeToggle = document.getElementById('themeToggle');
const themeToggleIcon = document.getElementById('themeToggleIcon');
const themeToggleLabel = document.getElementById('themeToggleLabel');
const authBtn = document.getElementById('authBtn');
const authBtnLabel = document.getElementById('authBtnLabel');
const authUserLabel = document.getElementById('authUserLabel');
const authMenu = document.getElementById('authMenu');
const signOutBtn = document.getElementById('signOutBtn');
const signOutDivider = document.getElementById('signOutDivider');
const homeLink = document.getElementById('homeLink');
const sharedLinksNav = document.getElementById('sharedLinksNav');

function showError(msg) {
    if (spinner) spinner.classList.add('d-none');
    if (alertBox) {
        alertBox.textContent = msg || 'Could not load note.';
        alertBox.classList.remove('d-none');
    }
    if (card) {
        card.classList.add('d-none');
    }
}

async function loadNote() {
    showLoadingState();
    if (!shareToken) return showError('Invalid share link.');
    try {
        const note = await Api.fetchNoteWithShareTokenViaShareApi(shareToken);
        renderNoteContent(note);
        showNoteCard();
    } catch (err) {
        handleNoteError(err);
    }
}

function showLoadingState() {
    spinner?.classList.remove('d-none');
    card?.classList.add('d-none');
}

function renderNoteContent(note) {
    noteId = note.id;
    updateTextContent(titleEl, note.title || 'Untitled');
    updateTextContent(contentEl, note.content || '');
    updateTextContent(ownerText, `Owner: ${note.owner || '—'}`);
    updateTextContent(createdByText, `Created by: ${note.createdBy || '—'}`);
    updateTextContent(updatedByText, `Updated by: ${note.lastModifiedBy || '—'}`);
    const createdParts = splitDateTime(note.createdDate);
    const updatedParts = splitDateTime(note.lastModifiedDate);
    updateTextContent(createdDateEl, createdParts.date);
    updateTextContent(createdTimeEl, createdParts.time);
    updateTextContent(updatedDateEl, updatedParts.date);
    updateTextContent(updatedTimeEl, updatedParts.time);
    updateColorDot(note.color);
    togglePinnedIcon(note.pinned);
    renderTagsList(Array.isArray(note.tags) ? note.tags : []);
}

function updateTextContent(el, value) {
    if (el) el.textContent = value;
}

function updateColorDot(color) {
    if (!colorDot) return;
    if (color) {
        colorDot.innerHTML = `<span class="badge rounded-pill bg-body-secondary border text-body" title="Color" style="border-color:${color};color:${color}"><i class="fa-solid fa-circle" style="color:${color}"></i></span>`;
    } else {
        colorDot.textContent = '';
    }
}

function togglePinnedIcon(pinned) {
    pinnedIcon?.classList.toggle('d-none', !pinned);
}

function renderTagsList(tags) {
    if (!tagsEl) return;
    tagsEl.innerHTML = tags.map(t => `<span class="badge bg-secondary-subtle text-secondary">${t}</span>`).join('');
}

function showNoteCard() {
    card?.classList.remove('d-none');
    alertBox?.classList.add('d-none');
    spinner?.classList.add('d-none');
}

function handleNoteError(err) {
    if (err?.status === 401 || err?.status === 403) {
        showError('This share link is no longer available. Please request a new link.');
        return;
    }
    showError(err?.message || 'Could not load note.');
}

function extractTokenFromPath() {
    const path = globalThis.location.pathname || '';
    const parts = path.split('/').filter(Boolean);
    if (parts.length >= 2 && parts[0] === 'share') {
        return parts[1];
    }
    return null;
}

document.addEventListener('DOMContentLoaded', () => {
    Theme.init({
        button: themeToggle,
        icon: themeToggleIcon,
        label: themeToggleLabel
    });
    initCurrentUser();
    bindSignOut();
    loadNote();
});

async function initCurrentUser() {
    const userFromState = State?.currentUser?.();
    if (userFromState?.username) {
        updateAuthUi(userFromState.username, true);
        return;
    }
    updateAuthUi('', false);
}

function bindSignOut() {
    if (!signOutBtn) return;
    signOutBtn.addEventListener('click', async () => {
        try {
            await Api.logout();
            globalThis.location.reload();
        } catch (err) {
            console.warn('Logout failed', err);
            globalThis.location.reload();
        }
    });
}

function updateAuthUi(username, signedIn) {
    updateAuthButton(username, signedIn);
    updateAuthLabel(username, signedIn);
    toggleAuthElement(authMenu, signedIn);
    toggleAuthElement(signOutDivider, signedIn);
    toggleAuthElement(signOutBtn, signedIn);
    toggleAuthElement(homeLink, signedIn);
    toggleAuthElement(sharedLinksNav, signedIn);
}

function updateAuthButton(username, signedIn) {
    if (!authBtn) return;
    authBtn.classList.toggle('d-none', !signedIn);
    authBtn.classList.toggle('dropdown-toggle', signedIn);
    if (signedIn) {
        authBtn.dataset.bsToggle = 'dropdown';
        if (authBtnLabel) authBtnLabel.textContent = username;
        return;
    }
    delete authBtn.dataset.bsToggle;
    if (authBtnLabel) authBtnLabel.textContent = '';
}

function updateAuthLabel(username, signedIn) {
    if (!authUserLabel) return;
    authUserLabel.classList.toggle('d-none', !signedIn);
    authUserLabel.textContent = signedIn ? `Signed in as ${username}` : '';
}

function toggleAuthElement(el, show) {
    if (el) el.classList.toggle('d-none', !show);
}

function splitDateTime(value) {
    if (!value) return {date: '—', time: '—'};
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return {date: value, time: ''};
    return {
        date: date.toLocaleDateString(),
        time: date.toLocaleTimeString()
    };
}
