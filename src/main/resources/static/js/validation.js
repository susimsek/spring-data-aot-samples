const Validation = (() => {
    function toggleSizeMessages(input) {
        if (!input) return;
        const name = input.id;
        const requiredMsg = document.querySelector(`[data-error-type="${name}-required"]`);
        const sizeMsg = document.querySelector(`[data-error-type="${name}-size"]`);
        const value = input.value.trim();
        const min = input.minLength > 0 ? input.minLength : null;
        const max = input.maxLength > 0 ? input.maxLength : null;
        const tooShort = min !== null && value.length > 0 && value.length < min;
        const tooLong = max !== null && value.length > max;
        const requiredInvalid = value.length === 0;
        if (requiredMsg) {
            requiredMsg.classList.toggle('d-none', !requiredInvalid);
        }
        if (sizeMsg) {
            sizeMsg.textContent = `Size must be between ${min ?? '0'} and ${max ?? '∞'} characters.`;
            sizeMsg.classList.toggle('d-none', requiredInvalid || !(tooShort || tooLong));
        }
    }

    function toggleInlineMessages(input, requiredEl, sizeEl, showValid = true) {
        if (!input) return;
        const value = input.value.trim();
        const min = input.minLength > 0 ? input.minLength : null;
        const max = input.maxLength > 0 ? input.maxLength : null;
        const requiredInvalid = value.length === 0;
        const tooShort = min !== null && value.length > 0 && value.length < min;
        const tooLong = max !== null && value.length > max;
        input.classList.toggle('is-invalid', requiredInvalid || tooShort || tooLong);
        if (showValid) {
            input.classList.toggle('is-valid', !requiredInvalid && !(tooShort || tooLong));
        } else {
            input.classList.remove('is-valid');
        }
        if (requiredEl) {
            requiredEl.classList.toggle('d-none', !requiredInvalid);
        }
        if (sizeEl) {
            sizeEl.textContent = `Size must be between ${min ?? '0'} and ${max ?? '∞'} characters.`;
            sizeEl.classList.toggle('d-none', requiredInvalid || !(tooShort || tooLong));
        }
    }

    return { toggleSizeMessages, toggleInlineMessages };
})();

export default Validation;
