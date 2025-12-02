// Toasts, loader toggles, formatting, and validation helpers
const Helpers = (() => {
    const toastContainer = document.getElementById('toastContainer');

    function escapeHtml(str) {
        if (str == null) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function formatDate(value) {
        if (!value) return '';
        const date = new Date(value);
        if (isNaN(date.getTime())) {
            return escapeHtml(String(value));
        }
        return date.toLocaleDateString('en-GB', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit'
        }) + ' ' + date.toLocaleTimeString('en-GB', {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    }

    function showToast(message, variant = 'success', action, title) {
        if (!toastContainer) return;
        const icons = {
            success: 'fa-circle-check',
            info: 'fa-circle-info',
            warning: 'fa-triangle-exclamation',
            danger: 'fa-triangle-exclamation'
        };
        const iconClass = icons[variant] || icons.info;
        const wrapper = document.createElement('div');
        wrapper.className = `toast text-bg-${variant} border-0`;
        wrapper.setAttribute('role', 'alert');
        const actionButton = action ? `<button type="button" class="btn btn-outline-light btn-sm ms-2 d-inline-flex align-items-center gap-1" data-action="toast-action"><i class="fa-solid fa-rotate-left"></i>${action.label}</button>` : '';
        const header = title ? `
            <div class="toast-header text-bg-${variant} border-0">
                <i class="fa-solid ${iconClass} me-2"></i>
                <strong class="me-auto">${escapeHtml(title)}</strong>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>` : '';
        const body = `
            <div class="toast-body d-flex align-items-center gap-2">
                ${title ? '' : `<i class="fa-solid ${iconClass}"></i>`}
                <span>${message}</span>
                ${actionButton}
            </div>`;
        wrapper.innerHTML = `
            ${header}
            ${body}`;
        toastContainer.appendChild(wrapper);
        const toast = new bootstrap.Toast(wrapper, {delay: 4000, autohide: true});
        if (action && typeof action.handler === 'function') {
            const btn = wrapper.querySelector('[data-action="toast-action"]');
            btn?.addEventListener('click', async (e) => {
                e.stopPropagation();
                await action.handler();
                toast.hide();
            });
        }
        toast.show();
        setTimeout(() => toast.hide(), 4000);
        toast._element.addEventListener('hidden.bs.toast', () => wrapper.remove());
    }

    function toggleSpinner(button, spinnerEl, labelEl, workingText) {
        if (!button) return;
        const disabled = button.disabled;
        button.disabled = true;
        spinnerEl?.classList.remove('d-none');
        if (labelEl && workingText) {
            labelEl.dataset.prev = labelEl.textContent;
            labelEl.textContent = workingText;
        }
        return () => {
            button.disabled = disabled;
            spinnerEl?.classList.add('d-none');
            if (labelEl && labelEl.dataset.prev) {
                labelEl.textContent = labelEl.dataset.prev;
                delete labelEl.dataset.prev;
            }
        };
    }

    function debounce(fn, delay) {
        let t;
        return (...args) => {
            clearTimeout(t);
            t = setTimeout(() => fn(...args), delay);
        };
    }

    return { escapeHtml, formatDate, showToast, toggleSpinner, debounce };
})();

export default Helpers;
