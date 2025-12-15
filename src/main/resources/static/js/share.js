import Api from '/js/api.js';
import Theme from '/js/theme.js';
import State from '/js/state.js';

const shareToken = extractTokenFromPath() || new URLSearchParams(window.location.search).get('share_token');
let noteId = null;
const alertBox = document.getElementById('alert');
const card = document.getElementById('card');
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
    if (alertBox) {
        alertBox.textContent = msg || 'Could not load note.';
        alertBox.classList.remove('d-none');
    }
    if (card) {
        card.classList.add('d-none');
    }
}

async function loadNote() {
    if (!shareToken) {
        showError('Invalid share link.');
        return;
    }
    try {
        const note = await Api.fetchNoteWithShareTokenViaShareApi(shareToken);
        noteId = note.id;
        if (titleEl) titleEl.textContent = note.title || 'Untitled';
        if (contentEl) contentEl.textContent = note.content || '';
        if (ownerText) ownerText.textContent = `Owner: ${note.owner || '—'}`;
        if (createdByText) createdByText.textContent = `Created by: ${note.createdBy || '—'}`;
        if (updatedByText) updatedByText.textContent = `Updated by: ${note.lastModifiedBy || '—'}`;
        const createdParts = splitDateTime(note.createdDate);
        const updatedParts = splitDateTime(note.lastModifiedDate);
        if (createdDateEl) createdDateEl.textContent = createdParts.date;
        if (createdTimeEl) createdTimeEl.textContent = createdParts.time;
        if (updatedDateEl) updatedDateEl.textContent = updatedParts.date;
        if (updatedTimeEl) updatedTimeEl.textContent = updatedParts.time;
        if (colorDot) {
            if (note.color) {
                colorDot.innerHTML = `<span class="badge rounded-pill bg-body-secondary border text-body" title="Color" style="border-color:${note.color};color:${note.color}"><i class="fa-solid fa-circle" style="color:${note.color}"></i></span>`;
            } else {
                colorDot.textContent = '';
            }
        }
        if (pinnedIcon) {
            pinnedIcon.classList.toggle('d-none', !note.pinned);
        }
        if (tagsEl) {
            const tags = Array.isArray(note.tags) ? note.tags : [];
            tagsEl.innerHTML = tags.map(t => `<span class="badge bg-secondary-subtle text-secondary">${t}</span>`).join('');
        }
        if (card) card.classList.remove('d-none');
        if (alertBox) alertBox.classList.add('d-none');
    } catch (err) {
        if (err?.status === 401 || err?.status === 403) {
            showError('This share link is no longer available. Please request a new link.');
            return;
        }
        showError(err?.message || 'Could not load note.');
    }
}

function extractTokenFromPath() {
    const path = window.location.pathname || '';
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
            window.location.reload();
        } catch (err) {
            // ignore errors
            window.location.reload();
        }
    });
}

function updateAuthUi(username, signedIn) {
    if (authBtn) {
        authBtn.classList.toggle('d-none', !signedIn);
        authBtn.classList.toggle('dropdown-toggle', signedIn);
        if (signedIn) {
            authBtn.setAttribute('data-bs-toggle', 'dropdown');
            if (authBtnLabel) {
                authBtnLabel.textContent = username;
            }
        } else {
            authBtn.removeAttribute('data-bs-toggle');
            if (authBtnLabel) {
                authBtnLabel.textContent = '';
            }
        }
    }
    if (authUserLabel) {
        authUserLabel.classList.toggle('d-none', !signedIn);
        authUserLabel.textContent = signedIn ? `Signed in as ${username}` : '';
    }
    if (authMenu) {
        authMenu.classList.toggle('d-none', !signedIn);
    }
    if (signOutDivider) {
        signOutDivider.classList.toggle('d-none', !signedIn);
    }
    if (signOutBtn) {
        signOutBtn.classList.toggle('d-none', !signedIn);
    }
    if (homeLink) homeLink.classList.toggle('d-none', !signedIn);
    if (sharedLinksNav) sharedLinksNav.classList.toggle('d-none', !signedIn);
}

function splitDateTime(value) {
    if (!value) return { date: '—', time: '—' };
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return { date: value, time: '' };
    return {
        date: date.toLocaleDateString(),
        time: date.toLocaleTimeString()
    };
}
