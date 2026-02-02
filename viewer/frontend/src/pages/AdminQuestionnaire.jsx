import React, { useState, useEffect } from 'react';
import Button from '../components/Button';
import { useAuth } from '../contexts/AuthContext';
import { useNotification } from '../contexts/NotificationContext';
import apiClient from '../services/apiClient';

const AdminQuestionnaire = () => {
    const { hasPermission } = useAuth();
    const { notify } = useNotification();
    const [sections, setSections] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // Modal States
    const [sectionModal, setSectionModal] = useState({ open: false, isEdit: false, data: {} });
    const [questionModal, setQuestionModal] = useState({ open: false, isEdit: false, sectionId: null, data: {} });

    const fetchQuestionnaire = async () => {
        try {
            const data = await apiClient.get('/questionnaire/template');
            setSections(data);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchQuestionnaire();
    }, []);

    // Section Handlers
    const openSectionModal = (section = null) => {
        setSectionModal({
            open: true,
            isEdit: !!section,
            data: section || { sectionName: '', displayOrder: sections.length + 1 }
        });
    };

    const handleSaveSection = async () => {
        try {
            await apiClient.post('/questionnaire/template/section', sectionModal.data);
            setSectionModal({ ...sectionModal, open: false });
            fetchQuestionnaire();
            notify('Section saved successfully', 'success');
        } catch (err) {
            notify('Error: ' + err.message, 'error');
        }
    };

    const handleDeleteSection = async (id) => {
        if (!window.confirm('Delete this section?')) return;
        try {
            await apiClient.delete(`/questionnaire/template/section/${id}`);
            fetchQuestionnaire();
            notify('Section deleted successfully', 'success');
        } catch (err) {
            notify('Error: ' + err.message, 'error');
        }
    };

    // Question Handlers
    const openQuestionModal = (sectionId, question = null) => {
        setQuestionModal({
            open: true,
            isEdit: !!question,
            sectionId,
            data: question || {
                sectionID: sectionId,
                questionText: '',
                questionType: 'TEXT',
                isMandatory: false,
                options: '',
                displayOrder: 1,
                riskFactorKey: ''
            }
        });
    };

    const handleSaveQuestion = async () => {
        try {
            const payload = { ...questionModal.data, sectionID: questionModal.sectionId };
            await apiClient.post('/questionnaire/template/question', payload);
            setQuestionModal({ ...questionModal, open: false });
            fetchQuestionnaire();
            notify('Question saved successfully', 'success');
        } catch (err) {
            notify('Error: ' + err.message, 'error');
        }
    };

    const handleDeleteQuestion = async (id) => {
        if (!window.confirm('Delete this question?')) return;
        try {
            await apiClient.delete(`/questionnaire/template/question/${id}`);
            fetchQuestionnaire();
            notify('Question deleted successfully', 'success');
        } catch (err) {
            notify('Error: ' + err.message, 'error');
        }
    };

    if (!hasPermission('MANAGE_PERMISSIONS')) return <div className="error">Unauthorized</div>;

    return (
        <div className="glass-section">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                <h2 style={{ margin: 0 }}>Questionnaire Configuration</h2>
                <Button onClick={() => openSectionModal()}>Add Section</Button>
            </div>

            {loading ? (
                <p className="loading">Loading configuration...</p>
            ) : error ? (
                <p className="error">{error}</p>
            ) : (
                <div>
                    {sections.map(section => (
                        <div key={section.sectionID} className="glass-section" style={{ marginBottom: '1.5rem', background: 'rgba(255,255,255,0.03)' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--glass-border)', paddingBottom: '0.5rem', marginBottom: '1rem' }}>
                                <h3 style={{ margin: 0, color: 'var(--primary-color)' }}>{section.sectionName}</h3>
                                <div>
                                    <Button variant="secondary" style={{ padding: '0.2rem 0.5rem', marginRight: '0.5rem' }} onClick={() => openSectionModal(section)}>Edit</Button>
                                    <Button variant="secondary" style={{ padding: '0.2rem 0.5rem', marginRight: '0.5rem', background: 'rgba(255,0,0,0.2)' }} onClick={() => handleDeleteSection(section.sectionID)}>Delete</Button>
                                    <Button style={{ padding: '0.2rem 0.5rem' }} onClick={() => openQuestionModal(section.sectionID)}>+ Add Question</Button>
                                </div>
                            </div>
                            <table>
                                <thead>
                                    <tr>
                                        <th>Question</th>
                                        <th>Type</th>
                                        <th>Required</th>
                                        <th>Order</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {section.questions.map(q => (
                                        <tr key={q.questionID}>
                                            <td>
                                                <div style={{ fontWeight: 'bold' }}>{q.questionText}</div>
                                                {q.options && <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Options: {q.options}</div>}
                                            </td>
                                            <td>{q.questionType}</td>
                                            <td>{q.isMandatory ? 'Yes' : 'No'}</td>
                                            <td>{q.displayOrder}</td>
                                            <td>
                                                <Button variant="secondary" style={{ padding: '0.1rem 0.4rem', marginRight: '0.5rem' }} onClick={() => openQuestionModal(section.sectionID, q)}>Edit</Button>
                                                <Button variant="secondary" style={{ padding: '0.1rem 0.4rem' }} onClick={() => handleDeleteQuestion(q.questionID)}>✕</Button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    ))}
                </div>
            )}

            {/* Section Modal */}
            {sectionModal.open && (
                <div className="modal" style={{ display: 'block' }}>
                    <div className="modal-content">
                        <h3>{sectionModal.isEdit ? 'Edit Section' : 'Add Section'}</h3>
                        <div className="form-group">
                            <label>Section Name</label>
                            <input
                                type="text"
                                value={sectionModal.data.sectionName}
                                onChange={(e) => setSectionModal({ ...sectionModal, data: { ...sectionModal.data, sectionName: e.target.value } })}
                            />
                        </div>
                        <div className="form-group">
                            <label>Display Order</label>
                            <input
                                type="number"
                                value={sectionModal.data.displayOrder}
                                onChange={(e) => setSectionModal({ ...sectionModal, data: { ...sectionModal.data, displayOrder: parseInt(e.target.value) } })}
                            />
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginTop: '1rem' }}>
                            <Button variant="secondary" onClick={() => setSectionModal({ ...sectionModal, open: false })}>Cancel</Button>
                            <Button onClick={handleSaveSection}>Save</Button>
                        </div>
                    </div>
                </div>
            )}

            {/* Question Modal */}
            {questionModal.open && (
                <div className="modal" style={{ display: 'block' }}>
                    <div className="modal-content">
                        <h3>{questionModal.isEdit ? 'Edit Question' : 'Add Question'}</h3>
                        <div className="form-group">
                            <label>Question Text</label>
                            <input
                                type="text"
                                value={questionModal.data.questionText}
                                onChange={(e) => setQuestionModal({ ...questionModal, data: { ...questionModal.data, questionText: e.target.value } })}
                            />
                        </div>
                        <div className="form-group">
                            <label>Type</label>
                            <select
                                value={questionModal.data.questionType}
                                onChange={(e) => setQuestionModal({ ...questionModal, data: { ...questionModal.data, questionType: e.target.value } })}
                            >
                                <option value="TEXT">Text</option>
                                <option value="YES_NO">Yes/No</option>
                                <option value="CHOICE">Single Choice (Dropdown)</option>
                                <option value="MULTI_CHOICE">Multi Choice (Checkbox)</option>
                            </select>
                        </div>
                        {(questionModal.data.questionType === 'CHOICE' || questionModal.data.questionType === 'MULTI_CHOICE') && (
                            <div className="form-group">
                                <label>Options (comma separated)</label>
                                <input
                                    type="text"
                                    placeholder="Option A,Option B,Option C"
                                    value={questionModal.data.options || ''}
                                    onChange={(e) => setQuestionModal({ ...questionModal, data: { ...questionModal.data, options: e.target.value } })}
                                />
                            </div>
                        )}
                        <div className="form-group">
                            <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}>
                                <input
                                    type="checkbox"
                                    checked={questionModal.data.isMandatory}
                                    onChange={(e) => setQuestionModal({ ...questionModal, data: { ...questionModal.data, isMandatory: e.target.checked } })}
                                />
                                Mandatory?
                            </label>
                        </div>
                        <div className="form-group">
                            <input
                                type="number"
                                value={questionModal.data.displayOrder}
                                onChange={(e) => setQuestionModal({ ...questionModal, data: { ...questionModal.data, displayOrder: parseInt(e.target.value) } })}
                            />
                        </div>

                        {/* Risk Factor Key - Only relevant for Yes/No, but editable for all */}
                        <div className="form-group">
                            <label>Risk Factor Key (Optional)</label>
                            <select
                                value={questionModal.data.riskFactorKey || ''}
                                onChange={(e) => setQuestionModal({ ...questionModal, data: { ...questionModal.data, riskFactorKey: e.target.value } })}
                            >
                                <option value="">-- None --</option>
                                <option value="ADVERSE_MEDIA">Adverse Media</option>
                                <option value="SUSPICIOUS_ACTIVITY_REPORT">Suspicious Activity Report</option>
                                <option value="INVESTOR_VISA">Investor Visa</option>
                            </select>
                            <p style={{ fontSize: '0.8rem', color: '#888', marginTop: '0.25rem' }}>
                                If mapped, a "Yes" answer will trigger this risk rule.
                            </p>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginTop: '1rem' }}>
                            <Button variant="secondary" onClick={() => setQuestionModal({ ...questionModal, open: false })}>Cancel</Button>
                            <Button onClick={handleSaveQuestion}>Save</Button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AdminQuestionnaire;
