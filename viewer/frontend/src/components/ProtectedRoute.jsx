import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const ProtectedRoute = ({ children, requiredRole, requiredPermission }) => {
    const { user, loading, hasPermission } = useAuth();
    const location = useLocation();

    if (loading) {
        return <div className="loading" style={{ color: 'white', textAlign: 'center', marginTop: '2rem' }}>Loading user session...</div>;
    }

    if (!user) {
        // Redirect to login page but save the attempted location
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    if (requiredRole && user.role !== requiredRole) {
        // Redirect to dashboard if role doesn't match
        return <Navigate to="/" replace />;
    }

    if (requiredPermission && !hasPermission(requiredPermission)) {
        // Redirect to dashboard if permission missing
        return <Navigate to="/" replace />;
    }

    return children;
};

export default ProtectedRoute;
