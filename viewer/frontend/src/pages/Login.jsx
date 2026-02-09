import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import './Login.css';

function Login() {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const navigate = useNavigate();
    const { checkAuth } = useAuth();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        try {
            // Direct call to Gateway which routes to auth-service
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ username, password }),
            });

            if (!response.ok) {
                throw new Error('Login failed: ' + response.statusText);
            }

            const data = await response.json();

            // Store token (and username if needed)
            localStorage.setItem('token', data.accessToken);
            localStorage.setItem('username', username);

            // Update Auth Context
            await checkAuth();

            // Redirect to dashboard or clients list
            navigate('/');
        } catch (err) {
            console.error(err);
            setError('Invalid username or password');
        }
    };

    return (
        <div className="login-page">
            <div className="login-card glass-section">
                {/* Decorative elements */}
                <div className="blob blob-1"></div>
                <div className="blob blob-2"></div>

                <div className="login-header">
                    <h1>Welcome Back</h1>
                    <p>Sign in to continue to KYC Viewer</p>
                </div>

                {error && (
                    <div className="error-message">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="login-form">
                    <div className="form-group">
                        <label>Username</label>
                        <input
                            type="text"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            placeholder="Enter your username"
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label>Password</label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="••••••••"
                            required
                        />
                    </div>

                    <button
                        type="submit"
                        className="btn login-btn"
                    >
                        Sign In
                    </button>
                </form>
            </div>
        </div>
    );
}

export default Login;
