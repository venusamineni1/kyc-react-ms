import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import Button from '../components/Button';

const Profile = () => {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    if (!user) return <div className="loading-container"><p className="loading">Loading profile...</p></div>;

    return (
        <div style={{
            padding: '2rem',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'flex-start',
            minHeight: 'calc(100vh - 100px)'
        }}>
            <div className="glass-section" style={{
                maxWidth: '700px',
                width: '100%',
                padding: '3rem',
                position: 'relative',
                overflow: 'hidden'
            }}>
                {/* Decorative Background Element */}
                <div style={{
                    position: 'absolute',
                    top: '-50px',
                    right: '-50px',
                    width: '150px',
                    height: '150px',
                    background: 'radial-gradient(circle, rgba(0, 242, 254, 0.15) 0%, transparent 70%)',
                    borderRadius: '50%',
                    zIndex: 0
                }} />

                <div style={{ position: 'relative', zIndex: 1 }}>
                    {/* Header Section */}
                    <div style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '2rem',
                        marginBottom: '3rem',
                        borderBottom: '1px solid rgba(255,255,255,0.1)',
                        paddingBottom: '2rem'
                    }}>
                        <div style={{
                            width: '90px',
                            height: '90px',
                            borderRadius: '50%',
                            background: 'linear-gradient(135deg, var(--primary-color), #4facfe)',
                            display: 'flex',
                            justifyContent: 'center',
                            alignItems: 'center',
                            fontSize: '2.5rem',
                            color: 'white',
                            fontWeight: 'bold',
                            boxShadow: '0 8px 16px rgba(0,0,0,0.2)'
                        }}>
                            {user.username.charAt(0).toUpperCase()}
                        </div>
                        <div>
                            <h2 style={{ margin: 0, fontSize: '1.8rem', color: 'var(--text-primary)' }}>{user.username}</h2>
                            <p style={{ margin: '0.2rem 0 0 0', color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
                                System User • <span style={{ color: '#00f2fe' }}>Active Now</span>
                            </p>
                        </div>
                    </div>

                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem' }}>
                        {/* Identity Section */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                            <h3 style={{ fontSize: '1.1rem', color: 'var(--text-primary)', margin: '0 0 0.5rem 0', fontWeight: '600' }}>Account Identity</h3>

                            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
                                <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: '600', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Member Since</label>
                                <span style={{ color: 'var(--text-primary)', fontSize: '1rem' }}>January 2024</span>
                            </div>

                            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
                                <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: '600', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Assigned Role</label>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                    <span className="status-badge" style={{ margin: 0, background: 'rgba(0, 242, 254, 0.1)', color: '#00f2fe' }}>{user.role}</span>
                                </div>
                            </div>
                        </div>

                        {/* Security Section */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                            <h3 style={{ fontSize: '1.1rem', color: 'var(--text-primary)', margin: '0 0 0.5rem 0', fontWeight: '600' }}>System Access</h3>

                            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.8rem' }}>
                                <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: '600', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Active Permissions</label>
                                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                                    {user.permissions && user.permissions.length > 0 ? (
                                        user.permissions.map(perm => (
                                            <span key={perm} style={{
                                                padding: '0.3rem 0.8rem',
                                                borderRadius: '6px',
                                                background: 'rgba(255,255,255,0.05)',
                                                border: '1px solid rgba(255,255,255,0.1)',
                                                fontSize: '0.75rem',
                                                color: 'var(--text-secondary)',
                                                fontWeight: '500'
                                            }}>
                                                {perm}
                                            </span>
                                        ))
                                    ) : (
                                        <span style={{ color: 'var(--text-muted)', fontSize: '0.9rem', fontStyle: 'italic' }}>No specific permissions.</span>
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Footer Actions */}
                    <div style={{
                        marginTop: '4rem',
                        display: 'flex',
                        justifyContent: 'flex-end',
                        gap: '1rem',
                        borderTop: '1px solid rgba(255,255,255,0.1)',
                        paddingTop: '2rem'
                    }}>
                        <Button variant="secondary" onClick={() => navigate('/')} style={{ px: '2rem' }}>
                            Return to Dashboard
                        </Button>
                        <Button onClick={logout} style={{
                            background: 'linear-gradient(135deg, #ff416c, #ff4b2b)',
                            border: 'none',
                            padding: '0.8rem 2.5rem'
                        }}>
                            Log Out
                        </Button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Profile;
