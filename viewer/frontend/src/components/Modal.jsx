import React from 'react';
import { createPortal } from 'react-dom';

const Modal = ({ isOpen, onClose, title, children, maxWidth = '600px' }) => {
    if (!isOpen) return null;

    const modalContent = (
        <div className="modal" style={{ display: 'block' }} onClick={onClose}>
            <div
                className="modal-content"
                style={{ maxWidth }}
                onClick={(e) => e.stopPropagation()}
            >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                    <h2 style={{ margin: 0 }}>{title}</h2>
                    <button
                        className="btn btn-secondary"
                        onClick={onClose}
                        style={{ padding: '0.2rem 0.5rem' }}
                    >
                        ✕
                    </button>
                </div>
                {children}
            </div>
        </div>
    );

    return createPortal(modalContent, document.body);
};

export default Modal;
