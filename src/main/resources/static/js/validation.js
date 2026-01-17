const Validation = (() => {
    function getSizeState(input) {
        if (!input) {
            return {
                value: '',
                min: null,
                max: null,
                requiredInvalid: false,
                tooShort: false,
                tooLong: false
            };
        }
        const value = (input.value ?? '').trim();
        const min = input.minLength > 0 ? input.minLength : null;
        const max = input.maxLength > 0 ? input.maxLength : null;
        const requiredInvalid = value.length === 0;
        const tooShort = min !== null && value.length > 0 && value.length < min;
        const tooLong = max !== null && value.length > max;
        return {value, min, max, requiredInvalid, tooShort, tooLong};
    }

    function toggleSizeMessages(input) {
        if (!input) return;
        const name = input.id;
        const requiredMsg = document.querySelector(`[data-error-type="${name}-required"]`);
        const sizeMsg = document.querySelector(`[data-error-type="${name}-size"]`);
        const {min, max, requiredInvalid, tooShort, tooLong} = getSizeState(input);
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
        const {value, min, max, requiredInvalid, tooShort, tooLong} = getSizeState(input);
        const customInvalid = value.length > 0 && !input.checkValidity();
        const invalid = requiredInvalid || tooShort || tooLong || customInvalid;
        input.classList.toggle('is-invalid', invalid);
        if (showValid) {
            input.classList.toggle('is-valid', !invalid && input.checkValidity());
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

    function togglePatternMessage(input, messageEl, {requireSizeOk = true} = {}) {
        if (!input || !messageEl) return;
        const {value, requiredInvalid, tooShort, tooLong} = getSizeState(input);
        if (value.length === 0) {
            messageEl.classList.add('d-none');
            return;
        }
        const sizeOk = !requiredInvalid && !(tooShort || tooLong);
        if (requireSizeOk && !sizeOk) {
            messageEl.classList.add('d-none');
            return;
        }
        messageEl.classList.toggle('d-none', input.checkValidity());
    }

    return {getSizeState, toggleSizeMessages, toggleInlineMessages, togglePatternMessage};
})();

export default Validation;
