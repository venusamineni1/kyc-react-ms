import React, { useEffect, useRef, useId } from 'react';
import { createPortal } from 'react-dom';
import { FiX } from 'react-icons/fi';

const FOCUSABLE_SELECTOR = 'a[href], button:not([disabled]), textarea, input, select, [tabindex]:not([tabindex="-1"])';

const Modal = ({ isOpen, onClose, title, children, maxWidth = '600px', closeOnOutsideClick = true }) => {
    const contentRef = useRef(null);
    const triggerRef = useRef(null);
    const titleId = useId();
    const onCloseRef = useRef(onClose);

    // Keep the ref current without making it an effect dependency below —
    // callers pass a new inline onClose on every render, and re-running the
    // focus-trap setup on every keystroke inside the modal was stealing focus
    // back to the first focusable element (the close button) as the user typed.
    useEffect(() => {
        onCloseRef.current = onClose;
    }, [onClose]);

    useEffect(() => {
        if (!isOpen) return;

        triggerRef.current = document.activeElement;

        const focusables = contentRef.current?.querySelectorAll(FOCUSABLE_SELECTOR);
        focusables?.[0]?.focus();

        const handleKeyDown = (e) => {
            if (e.key === 'Escape') {
                onCloseRef.current();
                return;
            }
            if (e.key !== 'Tab' || !contentRef.current) return;

            const nodes = contentRef.current.querySelectorAll(FOCUSABLE_SELECTOR);
            if (nodes.length === 0) return;
            const first = nodes[0];
            const last = nodes[nodes.length - 1];

            if (e.shiftKey && document.activeElement === first) {
                e.preventDefault();
                last.focus();
            } else if (!e.shiftKey && document.activeElement === last) {
                e.preventDefault();
                first.focus();
            }
        };

        document.addEventListener('keydown', handleKeyDown);
        return () => {
            document.removeEventListener('keydown', handleKeyDown);
            triggerRef.current?.focus?.();
        };
    }, [isOpen]);

    if (!isOpen) return null;

    const modalContent = (
        <div className="modal" style={{ display: 'block' }} onClick={closeOnOutsideClick ? onClose : undefined}>
            <div
                className="modal-content"
                style={{ maxWidth }}
                role="dialog"
                aria-modal="true"
                aria-labelledby={titleId}
                ref={contentRef}
                onClick={(e) => e.stopPropagation()}
            >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                    <h2 id={titleId} style={{ margin: 0 }}>{title}</h2>
                    <button
                        className="btn btn-secondary"
                        onClick={onClose}
                        aria-label="Close dialog"
                        style={{ padding: '0.2rem 0.5rem' }}
                    >
                        <FiX size={16} />
                    </button>
                </div>
                {children}
            </div>
        </div>
    );

    return createPortal(modalContent, document.body);
};

export default Modal;
