import React, { useState, useEffect } from 'react';
import { riskBatchService } from '../services/riskBatchService';
import { clientService } from '../services/clientService';
import { useNotification } from '../contexts/NotificationContext';
import './BatchPipeline.css'; // Reusing existing CSS

const BatchRiskPipeline = () => {
    const { notify } = useNotification();
    const [clients, setClients] = useState([]);
    const [selectedClients, setSelectedClients] = useState([]);
    const [batchId, setBatchId] = useState(null);
    const [currentStep, setCurrentStep] = useState(0);
    const [fileContent, setFileContent] = useState('');
    const [viewingFile, setViewingFile] = useState(null);

    useEffect(() => {
        loadClients();
    }, []);

    const loadClients = async () => {
        try {
            const result = await clientService.getClients(0, '');
            setClients(result.content || result || []);
        } catch (error) {
            notify('Failed to load clients', 'error');
        }
    };

    const handleClientSelect = (client) => {
        const isSelected = selectedClients.some(c => c.clientID === client.clientID);
        if (isSelected) {
            setSelectedClients(selectedClients.filter(c => c.clientID !== client.clientID));
        } else {
            setSelectedClients([...selectedClients, client]);
        }
    };

    const steps = [
        { id: 0, name: "Select Clients", action: "Create Batch" },
        { id: 1, name: "Generate JSONL", action: "Generate", view: "jsonl" },
        { id: 2, name: "Zip Files", action: "Zip" },
        { id: 3, name: "Generate Control File", action: "Generate", view: "control" },
        { id: 4, name: "Upload to SFTP", action: "Upload" }
    ];

    const runStep = async () => {
        try {
            switch (currentStep) {
                case 0: // Create Batch
                    if (selectedClients.length === 0) {
                        notify('Select at least one client', 'warning');
                        return;
                    }
                    const newBatchId = await riskBatchService.createBatch(selectedClients);
                    setBatchId(newBatchId); // API returns ID string directly
                    notify(`Batch ${newBatchId} created`, 'success');
                    setCurrentStep(1);
                    break;
                case 1: // JSONL
                    await riskBatchService.generateJsonl(batchId);
                    notify('JSONL Generated', 'success');
                    setCurrentStep(2);
                    break;
                case 2: // Zip
                    await riskBatchService.zipFiles(batchId);
                    notify('Files Zipped', 'success');
                    setCurrentStep(3);
                    break;
                case 3: // Control
                    await riskBatchService.generateControl(batchId);
                    notify('Control File Generated', 'success');
                    setCurrentStep(4);
                    break;
                case 4: // Upload
                    await riskBatchService.upload(batchId);
                    notify('Uploaded to SFTP (Mock/Real)', 'success');
                    break;
                default:
                    break;
            }
        } catch (error) {
            notify(`Step failed: ${error.message}`, 'error');
        }
    };

    const viewFile = async (type) => {
        if (!type || !batchId) return;
        try {
            const content = await riskBatchService.getFileContent(batchId, type);
            setFileContent(content);
            setViewingFile(type);
        } catch (error) {
            notify('Failed to load file content', 'error');
        }
    };

    return (
        <div className="batch-pipeline-container">
            <h1 className="pipeline-header">Risk Batch Pipeline</h1>

            {/* Stepper */}
            <div className="stepper-container">
                <div className="progress-bar-bg"></div>
                <div className="progress-bar-fill"
                    style={{ width: `${(currentStep / (steps.length - 1)) * 100}%` }}></div>

                {steps.map((step, index) => (
                    <div key={step.id} className="step-item" onClick={() => setCurrentStep(index)}>
                        <div className={`step-circle 
                            ${currentStep > index ? 'completed' : ''} 
                            ${currentStep === index ? 'active' : ''}`}>
                            {currentStep > index ? '✓' : index + 1}
                        </div>
                        <span className={`step-label 
                            ${currentStep > index ? 'completed' : ''} 
                            ${currentStep === index ? 'active' : ''}`}>
                            {step.name}
                        </span>
                    </div>
                ))}
            </div>

            <div className="content-card">
                <div className="step-header">
                    <h2 className="step-title">{steps[currentStep].name}</h2>
                    {currentStep > 0 && <div className="batch-id-display">Batch ID: {batchId}</div>}
                    <button
                        onClick={runStep}
                        className="action-btn"
                    >
                        {steps[currentStep].action}
                    </button>
                </div>

                <div className="mt-4">
                    {currentStep === 0 && (
                        <div className="overflow-x-auto">
                            <table className="client-table">
                                <thead>
                                    <tr>
                                        <th><input type="checkbox"
                                            onChange={(e) => {
                                                if (e.target.checked) setSelectedClients(clients);
                                                else setSelectedClients([]);
                                            }}
                                            checked={selectedClients.length === clients.length && clients.length > 0}
                                        /></th>
                                        <th>Name</th>
                                        <th>ID</th>
                                        <th>Country</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {clients.map(client => (
                                        <tr key={client.clientID}>
                                            <td>
                                                <input type="checkbox"
                                                    checked={selectedClients.some(c => c.clientID === client.clientID)}
                                                    onChange={() => handleClientSelect(client)}
                                                />
                                            </td>
                                            <td>{client.firstName} {client.lastName}</td>
                                            <td>{client.clientID}</td>
                                            <td>{client.citizenship1}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                            {clients.length === 0 && <p className="text-gray-500 mt-4">No clients found.</p>}
                        </div>
                    )}

                    {currentStep > 0 && (
                        <div className="space-y-4">
                            <div className="flex gap-2 mb-4" style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
                                {steps.filter((s, i) => i <= currentStep && s.view).map(s => (
                                    <button
                                        key={s.view}
                                        onClick={() => viewFile(s.view)}
                                        className="view-btn"
                                        disabled={currentStep < steps.findIndex(step => step.view === s.view)}
                                    >
                                        View {s.name} File
                                    </button>
                                ))}
                            </div>

                            {viewingFile && (
                                <div className="file-viewer">
                                    <h3 className="font-bold mb-2">Content: {viewingFile}</h3>
                                    <button
                                        className="close-btn"
                                        onClick={() => { setViewingFile(null); setFileContent(''); }}
                                    >
                                        Close
                                    </button>
                                    <pre className="file-content-pre">
                                        {fileContent}
                                    </pre>
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default BatchRiskPipeline;
