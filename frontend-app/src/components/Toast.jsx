import React, { useState, useCallback, useEffect, useRef } from 'react';

/**
 * Notificações no estilo pop-up (toast) — substituem os alertas nativos
 * (window.alert) e os banners de alerta inline. Seguem a paleta do projeto
 * definida em global.css.
 *
 * Uso:
 *   const { toasts, pushToast, dismissToast } = useToasts();
 *   pushToast({ type: 'success', title: 'Feito', text: 'Mensagem...' });
 *   <ToastStack toasts={toasts} onDismiss={dismissToast} />
 */

let _seq = 0;

const DEFAULT_DURATION = { success: 5000, info: 5000, warning: 6000, error: 8000 };

export function useToasts() {
    const [toasts, setToasts] = useState([]);
    const timers = useRef({});

    const dismissToast = useCallback((id) => {
        setToasts((list) => list.filter((t) => t.id !== id));
        if (timers.current[id]) {
            clearTimeout(timers.current[id]);
            delete timers.current[id];
        }
    }, []);

    const pushToast = useCallback((toast) => {
        const id = ++_seq;
        const type = toast.type || 'info';
        const duration = toast.duration ?? DEFAULT_DURATION[type] ?? 5000;

        setToasts((list) => [
            ...list,
            { id, type, title: toast.title || '', text: toast.text || '' },
        ]);

        if (duration > 0) {
            timers.current[id] = setTimeout(() => dismissToast(id), duration);
        }
        return id;
    }, [dismissToast]);

    useEffect(() => () => {
        Object.values(timers.current).forEach(clearTimeout);
        timers.current = {};
    }, []);

    return { toasts, pushToast, dismissToast };
}

const ICONS = { success: '✓', warning: '!', error: '✕', info: 'i' };
const FALLBACK_TITLE = {
    success: 'Sucesso',
    warning: 'Atenção',
    error: 'Erro',
    info: 'Informação',
};

export function useConfirmation() {
    const [confirmation, setConfirmation] = useState(null);
    const resolver = useRef(null);

    const confirm = useCallback((options) => new Promise((resolve) => {
        resolver.current = resolve;
        setConfirmation({
            type: options.type || 'info',
            title: options.title || 'Confirmação',
            text: options.text || '',
            confirmLabel: options.confirmLabel || 'Confirmar',
            cancelLabel: options.cancelLabel || 'Cancelar',
        });
    }), []);

    const resolveConfirmation = useCallback((confirmed) => {
        setConfirmation(null);
        resolver.current?.(confirmed);
        resolver.current = null;
    }, []);

    useEffect(() => () => resolver.current?.(false), []);

    return {
        confirmation,
        confirm,
        confirmAction: () => resolveConfirmation(true),
        cancelConfirmation: () => resolveConfirmation(false),
    };
}

export function ConfirmationDialog({ confirmation, onConfirm, onCancel }) {
    useEffect(() => {
        if (!confirmation) return undefined;

        const handleKeyDown = (event) => {
            if (event.key === 'Escape') onCancel();
        };

        window.addEventListener('keydown', handleKeyDown);
        return () => window.removeEventListener('keydown', handleKeyDown);
    }, [confirmation, onCancel]);

    if (!confirmation) return null;

    return (
        <div className="confirmation-overlay" role="presentation" onMouseDown={onCancel}>
            <div
                className={`toast confirmation-dialog toast-${confirmation.type}`}
                role="alertdialog"
                aria-modal="true"
                aria-labelledby="confirmation-title"
                aria-describedby="confirmation-text"
                onMouseDown={(event) => event.stopPropagation()}
            >
                <span className="toast-icon" aria-hidden="true">{ICONS[confirmation.type] || ICONS.info}</span>
                <div className="toast-body">
                    <div id="confirmation-title" className="toast-title">{confirmation.title}</div>
                    {confirmation.text && <div id="confirmation-text" className="toast-text">{confirmation.text}</div>}
                    <div className="confirmation-actions">
                        <button type="button" className="confirmation-cancel" onClick={onCancel}>
                            {confirmation.cancelLabel}
                        </button>
                        <button type="button" className="confirmation-confirm" onClick={onConfirm} autoFocus>
                            {confirmation.confirmLabel}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
export function ToastStack({ toasts, onDismiss }) {
    if (!toasts || toasts.length === 0) return null;

    return (
        <div className="toast-stack" aria-live="polite" aria-atomic="false">
            {toasts.map((t) => (
                <div
                    key={t.id}
                    className={`toast toast-${t.type}`}
                    role={t.type === 'error' ? 'alert' : 'status'}
                >
                    <span className="toast-icon" aria-hidden="true">
                        {ICONS[t.type] || ICONS.info}
                    </span>
                    <div className="toast-body">
                        <div className="toast-title">{t.title || FALLBACK_TITLE[t.type] || FALLBACK_TITLE.info}</div>
                        {t.text && <div className="toast-text">{t.text}</div>}
                    </div>
                    <button
                        type="button"
                        className="toast-close"
                        aria-label="Fechar notificação"
                        onClick={() => onDismiss(t.id)}
                    >
                        ×
                    </button>
                </div>
            ))}
        </div>
    );
}

export default ToastStack;
