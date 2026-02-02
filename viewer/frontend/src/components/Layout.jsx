import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useTheme } from '../contexts/ThemeContext';
import { useInbox } from '../contexts/InboxContext';
import Button from './Button';

const Layout = ({ children }) => {
    const { user, logout, hasPermission } = useAuth();
    const { theme, setTheme } = useTheme();
    const { inboxCount } = useInbox();
    const navigate = useNavigate();
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const menuRef = React.useRef(null);

    React.useEffect(() => {
        const handleClickOutside = (event) => {
            if (menuRef.current && !menuRef.current.contains(event.target)) {
                setIsMenuOpen(false);
            }
        };

        if (isMenuOpen) {
            document.addEventListener('mousedown', handleClickOutside);
        } else {
            document.removeEventListener('mousedown', handleClickOutside);
        }

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [isMenuOpen]);

    return (
        <div className="container">
            <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '3rem', position: 'relative', zIndex: 100 }}>
                <h1><Link to="/" style={{ color: 'inherit', textDecoration: 'none' }}>KYC Dashboard</Link></h1>

                <div style={{ position: 'relative' }} ref={menuRef}>
                    <Button onClick={() => setIsMenuOpen(!isMenuOpen)} style={{
                        minWidth: 'auto',
                        padding: '0.5rem',
                        background: 'transparent',
                        border: '1px solid var(--glass-border)',
                        color: 'var(--text-color)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        position: 'relative' // For absolute badge
                    }}>
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path d="M3 12H21" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                            <path d="M3 6H21" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                            <path d="M3 18H21" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                        </svg>
                        {inboxCount > 0 && (
                            <span style={{
                                position: 'absolute',
                                top: '-5px',
                                right: '-5px',
                                width: '10px',
                                height: '10px',
                                borderRadius: '50%',
                                backgroundColor: '#ff5555',
                                border: '1px solid var(--menu-bg)'
                            }} />
                        )}
                    </Button>

                    {isMenuOpen && (
                        <div className="glass-section" style={{
                            position: 'absolute',
                            right: 0,
                            top: '120%',
                            width: '220px',
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '0.8rem',
                            padding: '1rem',
                            zIndex: 101, // Ensure it floats above content
                            backdropFilter: 'blur(16px)', // Enhanced blur
                            background: 'var(--menu-bg)',
                            border: '1px solid var(--primary-color)',
                            boxShadow: '0 4px 20px rgba(0,0,0,0.5)'
                        }}>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.2rem' }}>
                                <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Theme</span>
                                <select
                                    value={theme}
                                    onChange={(e) => setTheme(e.target.value)}
                                    style={{ width: '100%', padding: '0.4rem', background: 'rgba(255,255,255,0.05)', color: 'var(--text-color)', border: '1px solid var(--glass-border)', borderRadius: '4px' }}
                                >
                                    <option value="theme-midnight">Midnight</option>
                                    <option value="theme-sunset">Sunset</option>
                                    <option value="theme-lightblue">Light Blue</option>
                                    <option value="theme-darkgray">Dark Gray</option>
                                    <option value="theme-light">Light</option>
                                </select>
                            </div>

                            <hr style={{ width: '100%', border: 'none', borderTop: '1px solid var(--glass-border)', margin: '0.2rem 0' }} />

                            <Link to="/inbox" className="btn btn-secondary" style={{ textAlign: 'center', display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '0.5rem' }} onClick={() => setIsMenuOpen(false)}>
                                My Inbox
                                {inboxCount > 0 && (
                                    <span style={{
                                        backgroundColor: '#ff5555',
                                        color: '#fff',
                                        fontSize: '0.75rem',
                                        padding: '0.1rem 0.4rem',
                                        borderRadius: '10px',
                                        lineHeight: 1
                                    }}>
                                        {inboxCount}
                                    </span>
                                )}
                            </Link>
                            <Link to="/adhoc-tasks" className="btn btn-secondary" style={{ textAlign: 'center' }} onClick={() => setIsMenuOpen(false)}>
                                Ad-Hoc Tasks
                            </Link>

                            {hasPermission('MANAGE_USERS') && (
                                <Link to="/users" className="btn btn-secondary" style={{ textAlign: 'center' }} onClick={() => setIsMenuOpen(false)}>
                                    Manage Users
                                </Link>
                            )}
                            {hasPermission('MANAGE_USERS') && (
                                <Link to="/admin/workflow" className="btn btn-secondary" style={{ textAlign: 'center' }} onClick={() => setIsMenuOpen(false)}>
                                    Workflow Dashboard
                                </Link>
                            )}
                            {hasPermission('MANAGE_PERMISSIONS') && (
                                <Link to="/permissions" className="btn btn-secondary" style={{ textAlign: 'center' }} onClick={() => setIsMenuOpen(false)}>
                                    Permissions
                                </Link>
                            )}
                            <Link to="/profile" className="btn btn-secondary" style={{ textAlign: 'center' }} onClick={() => setIsMenuOpen(false)}>
                                Profile
                            </Link>

                            <hr style={{ width: '100%', border: 'none', borderTop: '1px solid var(--glass-border)', margin: '0.2rem 0' }} />

                            <Button onClick={() => { logout(); setIsMenuOpen(false); }} style={{ backgroundColor: '#ff5555', width: '100%' }}>
                                Logout
                            </Button>
                        </div>
                    )}
                </div>
            </header>

            <main id="main-content">
                {children}
            </main>
        </div>
    );
};

export default Layout;
