import React from 'react';
import { useNotification } from '../contexts/NotificationContext';
import '../styles/Toast.css';

const ToastContainer = () => {
    const { notifications, removeNotification } = useNotification();

    return (
        <div className="toast-container">
            {notifications.map(n => (
                <div key={n.id} className={`toast toast-${n.type} slide-in`}>
                    <div className="toast-content">{n.message}</div>
                    <button className="toast-close" onClick={() => removeNotification(n.id)}>×</button>
                </div>
            ))}
        </div>
    );
};

export default ToastContainer;
