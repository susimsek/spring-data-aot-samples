import Api from './api.js';
import Helpers from './helpers.js';
import State from './state.js';
import Theme from './theme.js';
import Validation from './validation.js';

const {clearToken} = State;
const {showToast} = Helpers;
const {toggleInlineMessages, toggleSizeMessages} = Validation;

const urlParams = new URLSearchParams(globalThis.location.search);
const rawRedirect = urlParams.get('redirect');
const redirectTarget = rawRedirect && !rawRedirect.includes('/register') ? rawRedirect : '/';

const form = document.getElementById('registerForm');
const alertBox = document.getElementById('registerAlert');
const submitBtn = document.getElementById('registerSubmit');
const spinner = document.getElementById('registerSpinner');
const usernameInput = document.getElementById('registerUsername');
const emailInput = document.getElementById('registerEmail');
const passwordInput = document.getElementById('registerPassword');
const passwordConfirmInput = document.getElementById('registerPasswordConfirm');

const usernameRequiredMsg = document.querySelector('[data-error-type="registerUsername-required"]');
const usernameSizeMsg = document.querySelector('[data-error-type="registerUsername-size"]');
const usernamePatternMsg = document.querySelector('[data-error-type="registerUsername-pattern"]');
const emailRequiredMsg = document.querySelector('[data-error-type="registerEmail-required"]');
const emailSizeMsg = document.querySelector('[data-error-type="registerEmail-size"]');
const emailFormatMsg = document.querySelector('[data-error-type="registerEmail-format"]');
const passwordRequiredMsg = document.querySelector('[data-error-type="registerPassword-required"]');
const passwordSizeMsg = document.querySelector('[data-error-type="registerPassword-size"]');
const passwordConfirmRequiredMsg = document.querySelector(
    '[data-error-type="registerPasswordConfirm-required"]'
);
const passwordMismatchMsg = document.getElementById('registerPasswordConfirmMismatch');

const USERNAME_PATTERN = /^[A-Za-z0-9._-]+$/;

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
    [usernameInput, emailInput, passwordInput, passwordConfirmInput].forEach((el) => {
        if (!el) return;
        el.classList.remove('is-invalid', 'is-valid');
    });
    passwordMismatchMsg?.classList.add('d-none');
    form?.classList.remove('was-validated');
    hideAlert();
}

function validateUsername(showValid) {
    if (!usernameInput) return true;
    toggleInlineMessages(usernameInput, usernameRequiredMsg, usernameSizeMsg, showValid);
    const value = (usernameInput.value || '').trim();
    const ok = value.length === 0 || USERNAME_PATTERN.test(value);
    if (usernamePatternMsg) {
        usernamePatternMsg.classList.toggle('d-none', ok || value.length === 0);
    }
    if (!ok) {
        usernameInput.classList.add('is-invalid');
        if (showValid) {
            usernameInput.classList.remove('is-valid');
        }
    }
    return ok;
}

function validateEmail(showValid) {
    if (!emailInput) return true;
    toggleInlineMessages(emailInput, emailRequiredMsg, emailSizeMsg, showValid);
    const value = (emailInput.value || '').trim();
    const validFormat = value.length === 0 || emailInput.checkValidity();
    if (emailFormatMsg) {
        emailFormatMsg.classList.toggle('d-none', validFormat || value.length === 0);
    }
    if (!validFormat) {
        emailInput.classList.add('is-invalid');
        if (showValid) {
            emailInput.classList.remove('is-valid');
        }
    }
    return validFormat;
}

function validatePasswords(showValid) {
    if (!passwordInput || !passwordConfirmInput) return true;
    toggleInlineMessages(passwordInput, passwordRequiredMsg, passwordSizeMsg, showValid);
    toggleInlineMessages(passwordConfirmInput, passwordConfirmRequiredMsg, null, showValid);
    toggleSizeMessages(passwordConfirmInput);

    const pass = passwordInput.value || '';
    const confirm = passwordConfirmInput.value || '';
    const match = pass.length === 0 || confirm.length === 0 || pass === confirm;

    if (passwordMismatchMsg) {
        passwordMismatchMsg.classList.toggle('d-none', match);
    }
    passwordConfirmInput.classList.toggle('is-invalid', !match || !passwordConfirmInput.checkValidity());
    if (showValid) {
        passwordConfirmInput.classList.toggle('is-valid', match && passwordConfirmInput.checkValidity());
    } else {
        passwordConfirmInput.classList.remove('is-valid');
    }
    return match;
}

function bindLiveValidation() {
    usernameInput?.addEventListener('input', () => {
        hideAlert();
        validateUsername(false);
    });
    emailInput?.addEventListener('input', () => {
        hideAlert();
        validateEmail(false);
    });
    passwordInput?.addEventListener('input', () => {
        hideAlert();
        validatePasswords(false);
    });
    passwordConfirmInput?.addEventListener('input', () => {
        hideAlert();
        validatePasswords(false);
    });
}

async function handleSubmit(event) {
    event.preventDefault();
    if (!form) return;
    clearValidation();

    const usernameOk = validateUsername(false);
    const emailOk = validateEmail(false);
    const passOk = validatePasswords(false);
    if (!form.checkValidity() || !usernameOk || !emailOk || !passOk) {
        [usernameInput, emailInput, passwordInput, passwordConfirmInput].forEach((input) => {
            if (input && !input.checkValidity()) {
                input.classList.add('is-invalid');
            }
        });
        return;
    }

    const username = usernameInput?.value?.trim();
    const email = emailInput?.value?.trim();
    const password = passwordInput?.value || '';

    setLoading(true);
    const response = await Api.register({username, email, password}).catch((err) => {
        return {error: err};
    });
    setLoading(false);

    if (!response || response.error) {
        clearValidation();
        form?.classList.remove('was-validated');
        const message = response?.error?.message || 'Registration failed.';
        if (alertBox) {
            alertBox.textContent = message;
            alertBox.classList.remove('d-none');
        } else {
            showToast(message, 'danger');
        }
        return;
    }

    const registeredUsername = response?.username || username;
    form.reset();
    clearValidation();
    showToast(`Account created for ${registeredUsername}. Please sign in.`, 'success');
    globalThis.location.replace(
        `/login.html?registered=1&redirect=${encodeURIComponent(redirectTarget)}`
    );
}

function init() {
    clearToken();
    Theme.init({button: '#themeToggle', icon: '#themeToggleIcon', label: '#themeToggleLabel'});
    bindLiveValidation();
    usernameInput?.focus();
    form?.addEventListener('submit', handleSubmit);
}

init();
