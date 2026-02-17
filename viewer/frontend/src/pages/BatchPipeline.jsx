import React, { useState, useEffect } from 'react';
import { batchService } from '../services/batchService';
import { clientService } from '../services/clientService';
import { useNotification } from '../contexts/NotificationContext';
import './BatchPipeline.css';

const BatchPipeline = () => {
    const { notify } = useNotification();
    const [clients, setClients] = useState([]);
    const [selectedClients, setSelectedClients] = useState([]);
    const [batchId, setBatchId] = useState(null);
    const [currentStep, setCurrentStep] = useState(0);
    const [fileContent, setFileContent] = useState('');
    const [viewingFile, setViewingFile] = useState(null); // 'xml', 'checksum'

    useEffect(() => {
        loadClients();
    }, []);

    const loadClients = async () => {
        try {
            const result = await clientService.getClients(0, '');
            // backend returns Page object usually, check structure if needed (e.g. result.content)
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
        { id: 1, name: "Generate XML", action: "Generate", view: "xml" },
        { id: 2, name: "Generate Checksum", action: "Generate", view: "checksum" },
        { id: 3, name: "Zip Files", action: "Zip" },
        { id: 4, name: "Encrypt", action: "Encrypt" },
        { id: 5, name: "Upload to SFTP", action: "Upload" }
    ];

    const runStep = async () => {
        try {
            switch (currentStep) {
                case 0: // Create Batch
                    if (selectedClients.length === 0) {
                        notify('Select at least one client', 'warning');
                        return;
                    }
                    // Map frontend client fields to screening service Client fields
                    const mappedClients = selectedClients.map(c => {
                        const address = c.addresses && c.addresses.length > 0 ? c.addresses[0] : {};
                        const identifier = c.identifiers && c.identifiers.length > 0 ? c.identifiers[0] : {};

                        return {
                            ...c,
                            country: address.country || c.citizenship1 || c.countryOfTax || 'US', // Map to 'country' for backend
                            addressLine1: address.addressLine1,
                            city: address.city,
                            zipCode: address.zip,
                            province: address.addressSupplement || '',
                            nationality: c.citizenship1,
                            legDocType: identifier.identifierType,
                            idNumber: identifier.identifierValue || identifier.identifierNumber,
                            placeOfBirth: c.placeOfBirth,
                            cityOfBirth: c.cityOfBirth,
                            countryOfBirth: c.countryOfBirth
                        };
                    });

                    const newBatchId = await batchService.createBatch(mappedClients);
                    setBatchId(newBatchId);
                    notify(`Batch ${newBatchId} created`, 'success');
                    setCurrentStep(1);
                    break;
                case 1: // XML
                    await batchService.generateXml(batchId);
                    notify('XML Generated', 'success');
                    setCurrentStep(2);
                    break;
                case 2: // Checksum
                    await batchService.generateChecksum(batchId);
                    notify('Checksum Generated', 'success');
                    setCurrentStep(3);
                    break;
                case 3: // Zip
                    await batchService.zipFiles(batchId);
                    notify('Files Zipped', 'success');
                    setCurrentStep(4);
                    break;
                case 4: // Encrypt
                    await batchService.encryptFile(batchId);
                    notify('File Encrypted', 'success');
                    setCurrentStep(5);
                    break;
                case 5: // Upload
                    await batchService.uploadToSftp(batchId);
                    notify('Uploaded to SFTP', 'success');
                    // Done?
                    break;
                default:
                    break;
            }
        } catch (error) {
            notify(`Step failed: ${error.message}`, 'error');
        }
    };

    const viewFile = async (type) => {
        if (!type) return;
        try {
            const content = await batchService.getFileContent(batchId, type);
            setFileContent(content);
            setViewingFile(type);
        } catch (error) {
            notify('Failed to load file content', 'error');
        }
    };

    return (
        <div className="batch-pipeline-container">
            <h1 className="pipeline-header">Batch Screening Pipeline</h1>

            {/* Stepper */}
            <div className="stepper-container">
                {/* Connecting Line Background */}
                <div className="progress-bar-bg"></div>
                {/* Connecting Line Fill */}
                <div className="progress-bar-fill"
                    style={{ width: `${(currentStep / (steps.length - 1)) * 100}%` }}></div>

                {steps.map((step, index) => (
                    <div key={step.id} className="step-item" onClick={() => setCurrentStep(index)}>
                        {/* Disabled click for future steps? Optional. keeping enabled for verifying UI */}
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
                            {/* Ability to view files from previous steps */}
                            <div className="flex gap-2 mb-4" style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
                                {steps.filter((s, i) => i < currentStep && s.view).map(s => (
                                    <button
                                        key={s.view}
                                        onClick={() => viewFile(s.view)}
                                        className="view-btn"
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

export default BatchPipeline;
