import State from '/js/state.js';
import Api from '/js/api.js';
import Helpers from '/js/helpers.js';
import Validation from '/js/validation.js';
import Theme from '/js/theme.js';

const { clearToken, setCurrentUser } = State;
const { showToast } = Helpers;
const urlParams = new URLSearchParams(window.location.search);
const rawRedirect = urlParams.get('redirect');
const redirectTarget = rawRedirect && !rawRedirect.includes('/login') ? rawRedirect : '/';

const form = document.getElementById('loginForm');
const alertBox = document.getElementById('loginAlert');
const submitBtn = document.getElementById('loginSubmit');
const spinner = document.getElementById('loginSpinner');
const usernameInput = document.getElementById('loginUsername');
const passwordInput = document.getElementById('loginPassword');
const rememberMeInput = document.getElementById('loginRememberMe');
const { toggleInlineMessages } = Validation;
const usernameRequiredMsg = document.querySelector('[data-error-type="loginUsername-required"]');

function disablePasswordSizeValidation() {
    if (!passwordInput) return;
    passwordInput.removeAttribute('minlength');
    passwordInput.removeAttribute('maxlength');
}

function hideAlert() {
    if (alertBox) {
        alertBox.classList.add('d-none');
        alertBox.textContent = '';
    }
}

function setLoading(loading) {
    if (loading) {
        submitBtn?.setAttribute('disabled', 'disabled');
        spinner?.classList.remove('d-none');
    } else {
        submitBtn?.removeAttribute('disabled');
        spinner?.classList.add('d-none');
    }
}

function clearValidation() {
    [usernameInput, passwordInput, rememberMeInput].forEach(el => {
        if (!el) return;
        el.classList.remove('is-invalid', 'is-valid');
    });
    form?.classList.remove('was-validated');
    hideAlert();
}

function bindLiveValidation() {
    if (usernameInput) {
        usernameInput.addEventListener('input', () => {
            hideAlert();
            toggleInlineMessages(usernameInput, usernameRequiredMsg, null, false);
        });
    }
    if (passwordInput) {
        passwordInput.addEventListener('input', () => {
            hideAlert();
            toggleInlineMessages(passwordInput, null, null, false);
        });
    }
}

async function handleSubmit(event) {
    event.preventDefault();
    if (!form) return;
    clearValidation();
    toggleInlineMessages(usernameInput, usernameRequiredMsg, null, false);
    toggleInlineMessages(passwordInput, null, null, false);
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
    const rememberMe = !!rememberMeInput?.checked;

    setLoading(true);
    const response = await Api.login({ username, password, rememberMe }).catch(err => {
        return { error: err };
    });
    setLoading(false);

    if (!response || response.error) {
        clearValidation();
        form?.classList.remove('was-validated');
        const message = response?.error?.message || 'Login failed. Check credentials.';
        if (alertBox) {
            alertBox.textContent = message;
            alertBox.classList.remove('d-none');
        } else {
            showToast(message, 'danger');
        }
        return;
    }

    try {
        const me = await Api.currentUser();
        setCurrentUser(me);
    } catch (e) {
        setCurrentUser({ username });
    }
    form.reset();
    clearValidation();
    showToast('Signed in', 'success');
    window.location.replace(redirectTarget);
}

function init() {
    clearToken();
    if (form) {
        form.addEventListener('submit', handleSubmit);
    }
    if (usernameInput) {
        usernameInput.focus();
    }
    disablePasswordSizeValidation();
    Theme.init({ button: '#themeToggle', icon: '#themeToggleIcon', label: '#themeToggleLabel' });
    bindLiveValidation();
}

init();
