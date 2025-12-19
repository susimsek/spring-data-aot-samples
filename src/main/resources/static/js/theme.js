const Theme = (() => {
    const THEME_KEY = 'theme';
    let btn;
    let icon;
    let label;

    function systemPrefersDark() {
        return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    }

    function getStoredTheme() {
        try {
            return localStorage.getItem(THEME_KEY);
        } catch (e) {
            return null;
        }
    }

    function storeTheme(theme) {
        try {
            localStorage.setItem(THEME_KEY, theme);
        } catch (e) {
            // ignore
        }
    }

    function applyTheme(theme) {
        const next = theme === 'dark' ? 'dark' : 'light';
        document.documentElement.setAttribute('data-bs-theme', next);
        if (icon) {
            icon.classList.toggle('fa-moon', next === 'light');
            icon.classList.toggle('fa-sun', next === 'dark');
        }
        if (label) {
            label.textContent = next === 'dark' ? 'Light' : 'Dark';
        }
        if (btn) {
            btn.setAttribute('aria-pressed', next === 'dark');
            btn.setAttribute('aria-label', `Switch to ${next === 'dark' ? 'light' : 'dark'} mode`);
        }
        storeTheme(next);
    }

    function toggleTheme() {
        const current = (document.documentElement.getAttribute('data-bs-theme') === 'dark') ? 'dark' : 'light';
        applyTheme(current === 'dark' ? 'light' : 'dark');
    }

    function resolve(el) {
        if (!el) return null;
        if (typeof el === 'string') {
            return document.querySelector(el);
        }
        return el;
    }

    function init(options = {}) {
        btn = resolve(options.button);
        icon = resolve(options.icon);
        label = resolve(options.label);

        const stored = getStoredTheme();
        const prefers = systemPrefersDark() ? 'dark' : 'light';
        const initial = document.documentElement.getAttribute('data-bs-theme') || stored || prefers;
        applyTheme(initial);

        if (btn) {
            btn.addEventListener('click', toggleTheme);
        }
    }

    return {init, toggleTheme, applyTheme};
})();

export default Theme;
