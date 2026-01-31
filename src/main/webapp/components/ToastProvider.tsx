'use client';

import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import ToastContainer from 'react-bootstrap/ToastContainer';
import Toast from 'react-bootstrap/Toast';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCircleCheck, faCircleInfo, faTriangleExclamation } from '@fortawesome/free-solid-svg-icons';
import type { IconDefinition } from '@fortawesome/fontawesome-svg-core';

export type ToastVariant = 'success' | 'info' | 'warning' | 'danger';

export interface ToastAction {
  label: string;
  icon?: IconDefinition;
  handler?: () => void | Promise<void>;
}

export interface ToastItem {
  id: string;
  message: string;
  variant: ToastVariant;
  title?: string;
  action?: ToastAction;
}

let toastIdCounter = 0;

function createToastId(): string {
  const cryptoObj = globalThis.crypto;
  if (typeof cryptoObj?.randomUUID === 'function') {
    return cryptoObj.randomUUID();
  }

  const getRandomValues = cryptoObj?.getRandomValues?.bind(cryptoObj);
  if (getRandomValues) {
    const bytes = new Uint8Array(16);
    getRandomValues(bytes);
    return Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('');
  }

  toastIdCounter += 1;
  return `${Date.now()}-${toastIdCounter}`;
}

const ToastContext = createContext<{ pushToast: (message: string, variant?: ToastVariant, title?: string, action?: ToastAction) => void }>({
  pushToast: () => {},
});

const iconMap = {
  success: faCircleCheck,
  info: faCircleInfo,
  warning: faTriangleExclamation,
  danger: faTriangleExclamation,
} satisfies Record<ToastVariant, IconDefinition>;

export function useToasts() {
  return useContext(ToastContext);
}

export default function ToastProvider({ children }: Readonly<{ children: ReactNode }>) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const pushToast = useCallback((message: string, variant: ToastVariant = 'success', title?: string, action?: ToastAction) => {
    const id = createToastId();
    setToasts((prev) => [...prev, { id, message, variant, title, action }]);
  }, []);

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((toast) => toast.id !== id));
  }, []);

  const value = useMemo(() => ({ pushToast }), [pushToast]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <ToastContainer position="top-end" className="p-3" style={{ zIndex: 1080 }}>
        {toasts.map((toast) => {
          const icon = iconMap[toast.variant] || iconMap.info;
          const action = toast.action;
          const hasTitle = Boolean(toast.title);
          const showBodyIcon = !hasTitle;
          return (
            <Toast key={toast.id} bg={toast.variant} onClose={() => removeToast(toast.id)} delay={4000} autohide className="text-white">
              {hasTitle && (
                <Toast.Header closeButton={true} className={`text-bg-${toast.variant} border-0`}>
                  <FontAwesomeIcon icon={icon} className="me-2" />
                  <strong className="me-auto">{toast.title}</strong>
                </Toast.Header>
              )}
              <Toast.Body className="d-flex align-items-center gap-2">
                {showBodyIcon && <FontAwesomeIcon icon={icon} />}
                <span>{toast.message}</span>
                {action && (
                  <button
                    type="button"
                    className="btn btn-outline-light btn-sm ms-2 d-inline-flex align-items-center gap-1"
                    onClick={async () => {
                      await action.handler?.();
                      removeToast(toast.id);
                    }}
                  >
                    <FontAwesomeIcon icon={action.icon || faCircleInfo} />
                    {action.label}
                  </button>
                )}
              </Toast.Body>
            </Toast>
          );
        })}
      </ToastContainer>
    </ToastContext.Provider>
  );
}
