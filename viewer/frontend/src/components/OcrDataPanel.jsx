import React, { useState, useEffect } from 'react';

// Simulated OCR data shape returned by document-service
// In production this comes from GET /api/cases/{caseId}/documents/{docId}/ocr
const MOCK_OCR = {
    surname: { value: null, confidence: null, source: null },
    givenNames: { value: null, confidence: null, source: null },
    nationality: { value: null, confidence: null, source: null },
    dateOfBirth: { value: null, confidence: null, source: null },
    expiryDate: { value: null, confidence: null, source: null },
    documentNumber: { value: null, confidence: null, source: null },
    mrzCheckDigits: null,
    rawMrz: null,
};

function ConfidenceBadge({ value }) {
    if (value === null || value === undefined) return null;
    const pct = Math.round(value * 100);
    const color = pct >= 95 ? '#10b981' : pct >= 80 ? '#f59e0b' : '#ef4444';
    return (
        <span style={{
            fontSize: '0.7rem', fontWeight: 700,
            color, marginLeft: '6px'
        }}>{pct}%</span>
    );
}

function OcrField({ label, value, confidence, source }) {
    if (!value) return null;
    return (
        <div style={{
            background: 'rgba(255,255,255,0.04)',
            border: '1px solid var(--glass-border)',
            borderRadius: '7px',
            padding: '8px 12px',
        }}>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: '3px' }}>
                {label}
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                <span style={{ fontWeight: 700, fontSize: '0.92rem' }}>{value}</span>
                {confidence && <ConfidenceBadge value={confidence} />}
            </div>
            {source && (
                <div style={{ fontSize: '0.7rem', color: 'var(--text-secondary)', marginTop: '2px' }}>
                    via {source}
                </div>
            )}
        </div>
    );
}

export default function OcrDataPanel({ caseId, document }) {
    const [ocrData, setOcrData] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!document) { setOcrData(null); return; }
        setLoading(true);
        setError(null);
        // Fetch OCR results for selected document
        fetch(`/api/cases/${caseId}/documents/${document.documentID}/ocr`, {
            headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
        })
            .then(r => {
                if (!r.ok) throw new Error('No OCR data available');
                return r.json();
            })
            .then(data => setOcrData(data))
            .catch(err => setError(err.message))
            .finally(() => setLoading(false));
    }, [caseId, document?.documentID]);

    if (!document) {
        return (
            <div style={{ padding: '1rem', color: 'var(--text-secondary)', fontSize: '0.85rem', fontStyle: 'italic' }}>
                Select a document to view extracted data.
            </div>
        );
    }

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {/* Header */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '2px' }}>
                <span style={{
                    background: 'rgba(16,185,129,0.12)', color: '#10b981',
                    border: '1px solid rgba(16,185,129,0.3)',
                    fontSize: '0.7rem', fontWeight: 700, letterSpacing: '0.08em',
                    padding: '2px 7px', borderRadius: '4px', textTransform: 'uppercase'
                }}>🔤 OCR · MRZ Parser</span>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                    {document.documentName}
                </span>
            </div>

            {loading && (
                <div style={{ padding: '1.5rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                    <div style={{ fontSize: '0.85rem' }}>Extracting document data…</div>
                </div>
            )}

            {error && (
                <div style={{
                    background: 'rgba(245,158,11,0.08)', border: '1px solid rgba(245,158,11,0.25)',
                    borderRadius: '7px', padding: '10px 12px', fontSize: '0.82rem', color: '#f59e0b'
                }}>
                    ℹ OCR data not yet available for this document.
                    <div style={{ fontSize: '0.75rem', marginTop: '4px', color: 'var(--text-secondary)' }}>
                        Upload a new version or run document service to extract.
                    </div>
                </div>
            )}

            {ocrData && (() => {
                const hasFields = ocrData.surname?.value || ocrData.givenNames?.value ||
                    ocrData.nationality?.value || ocrData.dateOfBirth?.value ||
                    ocrData.expiryDate?.value || ocrData.documentNumber?.value;

                return (
                    <>
                        {hasFields ? (
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                                <OcrField label="Surname" value={ocrData.surname?.value} confidence={ocrData.surname?.confidence} source={ocrData.surname?.source} />
                                <OcrField label="Given Names" value={ocrData.givenNames?.value} confidence={ocrData.givenNames?.confidence} source={ocrData.givenNames?.source} />
                                <OcrField label="Nationality" value={ocrData.nationality?.value} confidence={ocrData.nationality?.confidence} source={ocrData.nationality?.source} />
                                <OcrField label="Date of Birth" value={ocrData.dateOfBirth?.value} confidence={ocrData.dateOfBirth?.confidence} source={ocrData.dateOfBirth?.source} />
                                <OcrField label="Expiry Date" value={ocrData.expiryDate?.value} confidence={ocrData.expiryDate?.confidence} source={ocrData.expiryDate?.source} />
                                <OcrField label="Document Number" value={ocrData.documentNumber?.value} confidence={ocrData.documentNumber?.confidence} source={ocrData.documentNumber?.source} />
                            </div>
                        ) : (
                            <div style={{
                                background: 'rgba(255,255,255,0.03)',
                                border: '1px solid var(--glass-border)',
                                borderRadius: '7px', padding: '10px 12px',
                                fontSize: '0.82rem', color: 'var(--text-secondary)'
                            }}>
                                No MRZ fields detected.{ocrData.source && (
                                    <span style={{ marginLeft: '6px', fontSize: '0.75rem' }}>
                                        (via {ocrData.source})
                                    </span>
                                )}
                            </div>
                        )}

                        {ocrData.mrzCheckDigits !== null && (
                            <div style={{
                                background: ocrData.mrzCheckDigits === 'PASS'
                                    ? 'rgba(16,185,129,0.08)' : 'rgba(239,68,68,0.08)',
                                border: `1px solid ${ocrData.mrzCheckDigits === 'PASS' ? 'rgba(16,185,129,0.3)' : 'rgba(239,68,68,0.3)'}`,
                                borderRadius: '7px', padding: '8px 12px', fontSize: '0.82rem',
                                color: ocrData.mrzCheckDigits === 'PASS' ? '#10b981' : '#ef4444',
                                display: 'flex', alignItems: 'center', gap: '8px'
                            }}>
                                <span>{ocrData.mrzCheckDigits === 'PASS' ? '✓' : '✗'}</span>
                                <span>MRZ Check Digits: <strong>{ocrData.mrzCheckDigits}</strong></span>
                            </div>
                        )}

                        {ocrData.rawMrz && (
                            <div>
                                <div style={{ fontSize: '0.7rem', color: 'var(--text-secondary)', marginBottom: '4px', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                                    Raw MRZ Output
                                </div>
                                <div style={{
                                    fontFamily: 'monospace', fontSize: '0.75rem',
                                    background: 'rgba(16,185,129,0.06)',
                                    border: '1px solid rgba(16,185,129,0.2)',
                                    borderRadius: '6px', padding: '8px 10px',
                                    color: '#34d399', letterSpacing: '0.05em', lineHeight: 1.8,
                                    wordBreak: 'break-all'
                                }}>
                                    {ocrData.rawMrz.split('\n').map((line, i) => (
                                        <div key={i}>{line}</div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {ocrData.rawText && (
                            <div>
                                <div style={{ fontSize: '0.7rem', color: 'var(--text-secondary)', marginBottom: '4px', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                                    Raw OCR Text
                                </div>
                                <div style={{
                                    fontFamily: 'monospace', fontSize: '0.72rem',
                                    background: 'rgba(255,255,255,0.03)',
                                    border: '1px solid var(--glass-border)',
                                    borderRadius: '6px', padding: '8px 10px',
                                    color: 'var(--text-secondary)', lineHeight: 1.6,
                                    whiteSpace: 'pre-wrap', wordBreak: 'break-word',
                                    maxHeight: '120px', overflowY: 'auto'
                                }}>
                                    {ocrData.rawText.trim()}
                                </div>
                            </div>
                        )}
                    </>
                );
            })()}
        </div>
    );
}
