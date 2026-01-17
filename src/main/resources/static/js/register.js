import Api from './api.js';
import Helpers from './helpers.js';
import State from './state.js';
import Theme from './theme.js';
import Validation from './validation.js';

const {clearToken} = State;
const {showToast} = Helpers;
const {getSizeState, toggleInlineMessages, toggleSizeMessages, togglePatternMessage} = Validation;

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
const passwordToggleBtn = document.getElementById('registerPasswordToggle');
const passwordToggleIcon = document.getElementById('registerPasswordToggleIcon');

const usernameRequiredMsg = document.querySelector('[data-error-type="registerUsername-required"]');
const usernameSizeMsg = document.querySelector('[data-error-type="registerUsername-size"]');
const usernamePatternMsg = document.querySelector('[data-error-type="registerUsername-pattern"]');
const emailRequiredMsg = document.querySelector('[data-error-type="registerEmail-required"]');
const emailSizeMsg = document.querySelector('[data-error-type="registerEmail-size"]');
const emailFormatMsg = document.querySelector('[data-error-type="registerEmail-format"]');
const passwordRequiredMsg = document.querySelector('[data-error-type="registerPassword-required"]');
const passwordSizeMsg = document.querySelector('[data-error-type="registerPassword-size"]');
const passwordPatternMsg = document.querySelector('[data-error-type="registerPassword-pattern"]');
const passwordConfirmRequiredMsg = document.querySelector(
    '[data-error-type="registerPasswordConfirm-required"]'
);
const passwordMismatchMsg = document.getElementById('registerPasswordConfirmMismatch');

const USERNAME_PATTERN = /^[A-Za-z0-9._-]+$/;
const PASSWORD_PATTERN = /^(?=.*[a-zçğıöşü])(?=.*[A-ZÇĞİÖŞÜ])(?=.*\d)(?=.*[!@#$%^&*()_+{};':"\\|,.<>/?\][-])(?!.*\s).+$/;

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
    usernameInput.setCustomValidity(ok ? '' : 'username-pattern');
    toggleInlineMessages(usernameInput, usernameRequiredMsg, usernameSizeMsg, showValid);
    togglePatternMessage(usernameInput, usernamePatternMsg);
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
    const pass = passwordInput.value || '';
    const confirm = passwordConfirmInput.value || '';
    const match = pass.length === 0 || confirm.length === 0 || pass === confirm;

    const {requiredInvalid, tooShort, tooLong} = getSizeState(passwordInput);
    const sizeOk = !requiredInvalid && !(tooShort || tooLong);
    const patternOk = pass.length === 0 || PASSWORD_PATTERN.test(pass);
    passwordInput.setCustomValidity(sizeOk && !patternOk ? 'password-pattern' : '');
    toggleInlineMessages(passwordInput, passwordRequiredMsg, passwordSizeMsg, showValid);

    togglePatternMessage(passwordInput, passwordPatternMsg, {requireSizeOk: true});

    toggleInlineMessages(passwordConfirmInput, passwordConfirmRequiredMsg, null, showValid);
    toggleSizeMessages(passwordConfirmInput);

    if (passwordMismatchMsg) {
        passwordMismatchMsg.classList.toggle('d-none', match);
    }
    passwordConfirmInput.classList.toggle('is-invalid', !match || !passwordConfirmInput.checkValidity());
    if (showValid) {
        passwordConfirmInput.classList.toggle('is-valid', match && passwordConfirmInput.checkValidity());
    } else {
        passwordConfirmInput.classList.remove('is-valid');
    }
    return match && passwordInput.checkValidity();
}

function bindLiveValidation() {
    usernameInput?.addEventListener('input', () => {
        hideAlert();
        validateUsername(true);
    });
    emailInput?.addEventListener('input', () => {
        hideAlert();
        validateEmail(true);
    });
    passwordInput?.addEventListener('input', () => {
        hideAlert();
        validatePasswords(true);
    });
    passwordConfirmInput?.addEventListener('input', () => {
        hideAlert();
        validatePasswords(true);
    });
}

function bindPasswordToggle() {
    if (!passwordInput || !passwordToggleBtn || !passwordToggleIcon) return;
    passwordToggleBtn.addEventListener('click', (event) => {
        event.preventDefault();
        const willShow = passwordInput.type === 'password';
        passwordInput.type = willShow ? 'text' : 'password';
        passwordToggleBtn.setAttribute('aria-label', willShow ? 'Hide password' : 'Show password');
        passwordToggleBtn.setAttribute('aria-pressed', willShow ? 'true' : 'false');
        passwordToggleIcon.classList.remove('fa-eye', 'fa-eye-slash');
        passwordToggleIcon.classList.add(willShow ? 'fa-eye-slash' : 'fa-eye');
        passwordInput.focus();
    });
}

async function handleSubmit(event) {
    event.preventDefault();
    if (!form) return;
    clearValidation();

    const usernameOk = validateUsername(true);
    const emailOk = validateEmail(true);
    const passOk = validatePasswords(true);
    form.classList.add('was-validated');
    if (!form.checkValidity() || !usernameOk || !emailOk || !passOk) {
        const firstInvalid = form.querySelector('input.is-invalid, input:invalid');
        firstInvalid?.focus();
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
    if (passwordInput && passwordToggleBtn && passwordToggleIcon) {
        passwordInput.type = 'password';
        passwordToggleBtn.setAttribute('aria-label', 'Show password');
        passwordToggleBtn.setAttribute('aria-pressed', 'false');
        passwordToggleIcon.classList.add('fa-eye');
        passwordToggleIcon.classList.remove('fa-eye-slash');
    }
    showToast(`Account created for ${registeredUsername}. Please sign in.`, 'success');
    globalThis.location.replace(
        `/login.html?registered=1&redirect=${encodeURIComponent(redirectTarget)}`
    );
}

function init() {
    clearToken();
    Theme.init({button: '#themeToggle', icon: '#themeToggleIcon', label: '#themeToggleLabel'});
    bindLiveValidation();
    bindPasswordToggle();
    usernameInput?.focus();
    form?.addEventListener('submit', handleSubmit);
}

init();
