// Event binding helpers for the notes UI
const Ui = (() => {
    function bindNoteGridActions(noteGrid, handlers) {
        if (!noteGrid) return;
        noteGrid.addEventListener('click', (e) => {
            const btn = e.target.closest('[data-action]');
            if (!btn) return;
            const action = btn.dataset.action;
            const id = Number.parseInt(btn.dataset.id, 10);
            if (!action || Number.isNaN(id)) return;
            const handler = handlers?.[action];
            if (typeof handler === 'function') {
                handler(id, e);
            }
        });
    }

    function bindRevisionActions(revisionList, handler) {
        if (!revisionList || typeof handler !== 'function') return;
        revisionList.addEventListener('click', (e) => {
            const btn = e.target.closest('[data-action="revision-restore"]');
            if (!btn) return;
            const noteId = Number.parseInt(btn.dataset.noteId, 10);
            const revId = Number.parseInt(btn.dataset.revId, 10);
            if (Number.isNaN(noteId) || Number.isNaN(revId)) return;
            handler(noteId, revId, e);
        });
    }

    return {bindNoteGridActions, bindRevisionActions};
})();

export default Ui;
