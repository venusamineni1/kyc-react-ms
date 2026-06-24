import React from 'react';
import { Link } from 'react-router-dom';

/**
 * items: [{ label, to }] - the last item is rendered as plain text (current page).
 */
const Breadcrumbs = ({ items }) => (
    <nav aria-label="Breadcrumb" className="breadcrumbs">
        {items.map((item, i) => {
            const isLast = i === items.length - 1;
            return (
                <span key={i} className="breadcrumb-item">
                    {isLast || !item.to ? (
                        <span aria-current={isLast ? 'page' : undefined} className="breadcrumb-current">
                            {item.label}
                        </span>
                    ) : (
                        <Link to={item.to} className="breadcrumb-link">{item.label}</Link>
                    )}
                    {!isLast && <span className="breadcrumb-separator" aria-hidden="true">/</span>}
                </span>
            );
        })}
    </nav>
);

export default Breadcrumbs;
