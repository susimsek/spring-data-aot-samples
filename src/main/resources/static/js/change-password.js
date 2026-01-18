import Api from './api.js';
import Helpers from './helpers.js';
import State from './state.js';
import Theme from './theme.js';
import Validation from './validation.js';

const {clearToken} = State;
const {showToast} = Helpers;
const {getSizeState, toggleInlineMessages, toggleSizeMessages, togglePatternMessage} = Validation;

const form = document.getElementById('changePasswordForm');
const alertBox = document.getElementById('changePasswordAlert');
const submitBtn = document.getElementById('changePasswordSubmit');
const spinner = document.getElementById('changePasswordSpinner');

const currentPasswordInput = document.getElementById('currentPassword');
const currentPasswordToggleBtn = document.getElementById('currentPasswordToggle');
const currentPasswordToggleIcon = document.getElementById('currentPasswordToggleIcon');

const newPasswordInput = document.getElementById('newPassword');
const newPasswordToggleBtn = document.getElementById('newPasswordToggle');
const newPasswordToggleIcon = document.getElementById('newPasswordToggleIcon');

const newPasswordConfirmInput = document.getElementById('newPasswordConfirm');

const currentPasswordRequiredMsg = document.querySelector(
    '[data-error-type="currentPassword-required"]'
);

const newPasswordRequiredMsg = document.querySelector('[data-error-type="newPassword-required"]');
const newPasswordSizeMsg = document.querySelector('[data-error-type="newPassword-size"]');
const newPasswordPatternMsg = document.querySelector('[data-error-type="newPassword-pattern"]');

const newPasswordConfirmRequiredMsg = document.querySelector(
    '[data-error-type="newPasswordConfirm-required"]'
);
const newPasswordMismatchMsg = document.getElementById('newPasswordConfirmMismatch');

const PASSWORD_PATTERN = /^(?=.*[A-ZÇĞİÖŞÜ])(?=.*[a-zçğıöşü])(?=.*\d).+$/;

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
    [currentPasswordInput, newPasswordInput, newPasswordConfirmInput].forEach((el) => {
        if (!el) return;
        el.classList.remove('is-invalid', 'is-valid');
    });
    newPasswordMismatchMsg?.classList.add('d-none');
    form?.classList.remove('was-validated');
    hideAlert();
}

function validateCurrentPassword(showValid) {
    if (!currentPasswordInput) return true;
    toggleInlineMessages(currentPasswordInput, currentPasswordRequiredMsg, null, showValid);
    return currentPasswordInput.checkValidity();
}

function validateNewPassword(showValid) {
    if (!newPasswordInput) return true;
    const pass = newPasswordInput.value || '';

    const {requiredInvalid, tooShort, tooLong} = getSizeState(newPasswordInput);
    const sizeOk = !requiredInvalid && !(tooShort || tooLong);
    const patternOk = pass.length === 0 || PASSWORD_PATTERN.test(pass);
    newPasswordInput.setCustomValidity(sizeOk && !patternOk ? 'password-pattern' : '');

    toggleInlineMessages(newPasswordInput, newPasswordRequiredMsg, newPasswordSizeMsg, showValid);
    togglePatternMessage(newPasswordInput, newPasswordPatternMsg, {requireSizeOk: true});
    return newPasswordInput.checkValidity();
}

function validateConfirmPassword(showValid) {
    if (!newPasswordInput || !newPasswordConfirmInput) return true;
    const pass = newPasswordInput.value || '';
    const confirm = newPasswordConfirmInput.value || '';
    const match = pass.length === 0 || confirm.length === 0 || pass === confirm;

    toggleInlineMessages(newPasswordConfirmInput, newPasswordConfirmRequiredMsg, null, showValid);
    toggleSizeMessages(newPasswordConfirmInput);

    if (newPasswordMismatchMsg) {
        newPasswordMismatchMsg.classList.toggle('d-none', match);
    }
    newPasswordConfirmInput.classList.toggle(
        'is-invalid',
        !match || !newPasswordConfirmInput.checkValidity()
    );
    if (showValid) {
        newPasswordConfirmInput.classList.toggle(
            'is-valid',
            match && newPasswordConfirmInput.checkValidity()
        );
    } else {
        newPasswordConfirmInput.classList.remove('is-valid');
    }
    return match && newPasswordConfirmInput.checkValidity();
}

function bindPasswordToggle(input, button, icon) {
    if (!input || !button || !icon) return;
    button.addEventListener('click', (event) => {
        event.preventDefault();
        const willShow = input.type === 'password';
        input.type = willShow ? 'text' : 'password';
        button.setAttribute('aria-label', willShow ? 'Hide password' : 'Show password');
        button.setAttribute('aria-pressed', willShow ? 'true' : 'false');
        icon.classList.remove('fa-eye', 'fa-eye-slash');
        icon.classList.add(willShow ? 'fa-eye-slash' : 'fa-eye');
        input.focus();
    });
}

function bindLiveValidation() {
    currentPasswordInput?.addEventListener('input', () => {
        hideAlert();
        validateCurrentPassword(true);
    });
    newPasswordInput?.addEventListener('input', () => {
        hideAlert();
        validateNewPassword(true);
        validateConfirmPassword(true);
    });
    newPasswordConfirmInput?.addEventListener('input', () => {
        hideAlert();
        validateConfirmPassword(true);
    });
}

async function handleSubmit(event) {
    event.preventDefault();
    if (!form) return;
    clearValidation();

    const currentOk = validateCurrentPassword(true);
    const newOk = validateNewPassword(true);
    const confirmOk = validateConfirmPassword(true);
    form.classList.add('was-validated');
    if (!form.checkValidity() || !currentOk || !newOk || !confirmOk) {
        const firstInvalid = form.querySelector('input.is-invalid, input:invalid');
        firstInvalid?.focus();
        return;
    }

    const currentPassword = currentPasswordInput?.value || '';
    const newPassword = newPasswordInput?.value || '';

    setLoading(true);
    const response = await Api.changePassword({currentPassword, newPassword}).catch((err) => {
        return {error: err};
    });
    setLoading(false);

    if (!response || response.error) {
        clearValidation();
        form?.classList.remove('was-validated');
        const message = response?.error?.message || 'Password update failed.';
        if (alertBox) {
            alertBox.textContent = message;
            alertBox.classList.remove('d-none');
        } else {
            showToast(message, 'danger');
        }
        return;
    }

    form.reset();
    clearValidation();
    clearToken();
    showToast('Password updated. Please sign in again.', 'success');
    globalThis.location.replace('/login.html');
}

function init() {
    Theme.init({button: '#themeToggle', icon: '#themeToggleIcon', label: '#themeToggleLabel'});
    bindLiveValidation();
    bindPasswordToggle(
        currentPasswordInput,
        currentPasswordToggleBtn,
        currentPasswordToggleIcon
    );
    bindPasswordToggle(newPasswordInput, newPasswordToggleBtn, newPasswordToggleIcon);
    currentPasswordInput?.focus();
    form?.addEventListener('submit', handleSubmit);
}

init();
