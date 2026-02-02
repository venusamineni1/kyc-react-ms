import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import Button from '../components/Button';

const Profile = () => {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    if (!user) return <p className="loading">Loading profile...</p>;

    return (
        <div className="glass-section" style={{ maxWidth: '600px', margin: '0 auto' }}>
            <h2 style={{ marginBottom: '2rem', textAlign: 'center' }}>User Profile</h2>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '1rem', background: 'rgba(255,255,255,0.05)', borderRadius: '8px' }}>
                    <span style={{ color: 'var(--text-muted)' }}>Username</span>
                    <span style={{ fontWeight: 'bold' }}>{user.username}</span>
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '1rem', background: 'rgba(255,255,255,0.05)', borderRadius: '8px' }}>
                    <span style={{ color: 'var(--text-muted)' }}>Role</span>
                    <span className="status-badge" style={{ alignSelf: 'center' }}>{user.role}</span>
                </div>

                <div style={{ padding: '1rem', background: 'rgba(255,255,255,0.05)', borderRadius: '8px' }}>
                    <h3 style={{ fontSize: '1rem', marginBottom: '1rem', color: 'var(--text-muted)' }}>Permissions</h3>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                        {user.permissions && user.permissions.length > 0 ? (
                            user.permissions.map(perm => (
                                <span key={perm} style={{
                                    padding: '0.25rem 0.75rem',
                                    borderRadius: '20px',
                                    background: 'var(--primary-color)',
                                    fontSize: '0.8rem',
                                    opacity: 0.9
                                }}>
                                    {perm}
                                </span>
                            ))
                        ) : (
                            <span style={{ color: 'var(--text-muted)', fontStyle: 'italic' }}>No specific permissions assigned.</span>
                        )}
                    </div>
                </div>

                <div style={{ marginTop: '2rem', display: 'flex', justifyContent: 'center', gap: '1rem' }}>
                    <Button variant="secondary" onClick={() => navigate('/')} style={{ width: '100%', maxWidth: '200px' }}>
                        Close
                    </Button>
                    <Button onClick={logout} style={{ width: '100%', maxWidth: '200px', backgroundColor: '#ff5555' }}>
                        Logout
                    </Button>
                </div>
            </div>
        </div>
    );
};

export default Profile;
