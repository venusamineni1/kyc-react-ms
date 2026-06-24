import React from 'react';

const VARIANT_CLASSES = {
    secondary: 'btn-secondary',
    danger: 'btn-danger',
    ghost: 'btn-ghost',
};

const Button = ({ children, onClick, type = 'button', variant = 'primary', className = '', ...props }) => {
    const baseClass = 'btn';
    const variantClass = VARIANT_CLASSES[variant] || '';

    return (
        <button
            type={type}
            className={`${baseClass} ${variantClass} ${className}`}
            onClick={onClick}
            {...props}
        >
            {children}
        </button>
    );
};

export default Button;
