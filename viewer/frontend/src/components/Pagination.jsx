import React from 'react';

const Pagination = ({ data, onPageChange }) => {
    if (!data || data.totalPages <= 1) return null;

    return (
        <div className="pagination-container">
            <button
                className="pagination-btn"
                disabled={data.currentPage === 0}
                onClick={() => onPageChange(data.currentPage - 1)}
            >
                Previous
            </button>
            <span className="pagination-info">Page {data.currentPage + 1} of {data.totalPages}</span>
            <button
                className="pagination-btn"
                disabled={data.currentPage >= data.totalPages - 1}
                onClick={() => onPageChange(data.currentPage + 1)}
            >
                Next
            </button>
        </div>
    );
};

export default Pagination;
