'use client';

import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import ToastContainer from 'react-bootstrap/ToastContainer';
import Toast from 'react-bootstrap/Toast';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCircleCheck, faCircleInfo, faTriangleExclamation } from '@fortawesome/free-solid-svg-icons';

const ToastContext = createContext({
  pushToast: () => {},
});

const iconMap = {
  success: faCircleCheck,
  info: faCircleInfo,
  warning: faTriangleExclamation,
  danger: faTriangleExclamation,
};

export function useToasts() {
  return useContext(ToastContext);
}

export default function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const pushToast = useCallback((message, variant = 'success', title, action) => {
    const id = `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    setToasts(prev => [...prev, { id, message, variant, title, action }]);
  }, []);

  const removeToast = useCallback(id => {
    setToasts(prev => prev.filter(toast => toast.id !== id));
  }, []);

  const value = useMemo(() => ({ pushToast }), [pushToast]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <ToastContainer position="top-end" className="p-3" style={{ zIndex: 1080 }}>
        {toasts.map(toast => {
          const icon = iconMap[toast.variant] || iconMap.info;
          return (
            <Toast key={toast.id} bg={toast.variant} onClose={() => removeToast(toast.id)} delay={4000} autohide className="text-white">
              {toast.title ? (
                <Toast.Header closeButton={true} className={`text-bg-${toast.variant} border-0`}>
                  <FontAwesomeIcon icon={icon} className="me-2" />
                  <strong className="me-auto">{toast.title}</strong>
                </Toast.Header>
              ) : null}
              <Toast.Body className="d-flex align-items-center gap-2">
                {!toast.title ? <FontAwesomeIcon icon={icon} /> : null}
                <span>{toast.message}</span>
                {toast.action ? (
                  <button
                    type="button"
                    className="btn btn-outline-light btn-sm ms-2 d-inline-flex align-items-center gap-1"
                    onClick={async () => {
                      await toast.action.handler?.();
                      removeToast(toast.id);
                    }}
                  >
                    <FontAwesomeIcon icon={toast.action.icon || faCircleInfo} />
                    {toast.action.label}
                  </button>
                ) : null}
              </Toast.Body>
            </Toast>
          );
        })}
      </ToastContainer>
    </ToastContext.Provider>
  );
}
