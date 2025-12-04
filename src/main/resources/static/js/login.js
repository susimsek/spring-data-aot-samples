import State from '/js/state.js';
import Api from '/js/api.js';
import Helpers from '/js/helpers.js';
import Validation from '/js/validation.js';

const { saveToken, clearToken, setCurrentUser } = State;
const { showToast } = Helpers;

const form = document.getElementById('loginForm');
const alertBox = document.getElementById('loginAlert');
const submitBtn = document.getElementById('loginSubmit');
const spinner = document.getElementById('loginSpinner');
const usernameInput = document.getElementById('loginUsername');
const passwordInput = document.getElementById('loginPassword');
const usernameRequiredMsg = document.querySelector('[data-error-type="loginUsername-required"]');
const usernameSizeMsg = document.querySelector('[data-error-type="loginUsername-size"]');
const passwordRequiredMsg = document.querySelector('[data-error-type="loginPassword-required"]');
const passwordSizeMsg = document.querySelector('[data-error-type="loginPassword-size"]');
const themeToggle = document.getElementById('themeToggle');
const themeToggleIcon = document.getElementById('themeToggleIcon');
const themeToggleLabel = document.getElementById('themeToggleLabel');

const { toggleInlineMessages } = Validation;

function setLoading(loading) {
    if (loading) {
        submitBtn?.setAttribute('disabled', 'disabled');
        spinner?.classList.remove('d-none');
    } else {
        submitBtn?.removeAttribute('disabled');
        spinner?.classList.add('d-none');
    }
}

function markInvalid(input, message) {
    if (!input) return;
    input.classList.add('is-invalid');
    const feedback = input.parentElement?.querySelector('.invalid-feedback');
    if (feedback && message) {
        feedback.textContent = message;
    }
}

function clearValidation() {
    [usernameInput, passwordInput].forEach(el => {
        if (!el) return;
        el.classList.remove('is-invalid', 'is-valid');
    });
    if (alertBox) {
        alertBox.classList.add('d-none');
        alertBox.textContent = '';
    }
}

function bindLiveValidation() {
    const inputs = [usernameInput, passwordInput];
    if (usernameInput) {
        usernameInput.addEventListener('input', () => toggleInlineMessages(usernameInput, usernameRequiredMsg, usernameSizeMsg));
    }
    if (passwordInput) {
        passwordInput.addEventListener('input', () => toggleInlineMessages(passwordInput, passwordRequiredMsg, passwordSizeMsg));
    }
}

async function handleSubmit(event) {
    event.preventDefault();
    if (!form) return;
    clearValidation();
    form.classList.add('was-validated');
    toggleInlineMessages(usernameInput, usernameRequiredMsg, usernameSizeMsg);
    toggleInlineMessages(passwordInput, passwordRequiredMsg, passwordSizeMsg);
    if (!form.checkValidity()) {
        [usernameInput, passwordInput].forEach(input => {
            if (input && !input.checkValidity()) {
                input.classList.add('is-invalid');
            }
        });
        return;
    }

    const username = usernameInput?.value?.trim();
    const password = passwordInput?.value || '';

    setLoading(true);
    const response = await Api.login({ username, password }).catch(err => {
        return { error: err };
    });
    setLoading(false);

    if (!response || response.error) {
        const message = response?.error?.message || 'Login failed. Check credentials.';
        if (alertBox) {
            alertBox.textContent = message;
            alertBox.classList.remove('d-none');
        } else {
            showToast(message, 'danger');
        }
        markInvalid(usernameInput);
        markInvalid(passwordInput);
        return;
    }

    saveToken(response.token);
    try {
        const me = await Api.currentUser();
        setCurrentUser(me);
    } catch (e) {
        setCurrentUser({ username });
    }
    form.reset();
    form.classList.remove('was-validated');
    showToast('Signed in', 'success');
    window.location.href = '/';
}

function init() {
    clearToken();
    if (form) {
        form.addEventListener('submit', handleSubmit);
    }
    if (usernameInput) {
        usernameInput.focus();
    }
    initTheme();
    if (themeToggle) {
        themeToggle.addEventListener('click', toggleTheme);
    }
    bindLiveValidation();
}

init();

function systemPrefersDark() {
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
}

function getStoredTheme() {
    try {
        return localStorage.getItem('theme');
    } catch (e) {
        return null;
    }
}

function storeTheme(theme) {
    try {
        localStorage.setItem('theme', theme);
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

function initTheme() {
    const stored = getStoredTheme();
    const prefers = systemPrefersDark() ? 'dark' : 'light';
    const initial = document.documentElement.getAttribute('data-bs-theme') || stored || prefers;
    applyTheme(initial);
}

function toggleTheme() {
    const current = document.documentElement.getAttribute('data-bs-theme') === 'dark' ? 'dark' : 'light';
    const next = current === 'dark' ? 'light' : 'dark';
    applyTheme(next);
    storeTheme(next);
}
