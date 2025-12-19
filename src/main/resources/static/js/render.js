import Helpers from './helpers.js';

const {escapeHtml, formatDate} = Helpers;

function renderTags(note) {
    const tags = (note?.tags || [])
        .map(tagLabel)
        .filter(t => t && t.trim().length > 0);
    if (!tags.length) return '';
    const items = tags.map(tag => `<span class="badge rounded-pill bg-secondary-subtle text-secondary">${escapeHtml(tag)}</span>`);
    return `<div class="d-flex flex-wrap gap-1 mt-1">${items.join('')}</div>`;
}

function revisionTypeBadge(type) {
    const map = {
        ADD: 'bg-success-subtle text-success border-success-subtle',
        MOD: 'bg-info-subtle text-info border-info-subtle',
        DEL: 'bg-danger-subtle text-danger border-danger-subtle'
    };
    return map[type] || 'bg-secondary-subtle text-secondary border-secondary-subtle';
}

export default {
    renderTags,
    revisionTypeBadge,
    escapeHtml,
    formatDate
};

function tagLabel(tag) {
    if (tag == null) return '';
    if (typeof tag === 'string') return tag;
    if (typeof tag === 'object') {
        return tag.name ?? tag.label ?? '';
    }
    return String(tag);
}
