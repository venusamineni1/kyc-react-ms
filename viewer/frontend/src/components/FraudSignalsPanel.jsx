import React, { useState, useEffect } from 'react';

// Signal severity levels
const LEVEL = {
    PASS: 'pass',
    WARN: 'warn',
    FAIL: 'fail',
    PENDING: 'pending',
};

function SignalRow({ icon, title, detail, level }) {
    const colors = {
        pass:    { bg: 'rgba(16,185,129,0.08)',  border: 'rgba(16,185,129,0.25)',  text: '#10b981', icon: '✓' },
        warn:    { bg: 'rgba(245,158,11,0.08)',  border: 'rgba(245,158,11,0.25)',  text: '#f59e0b', icon: '!' },
        fail:    { bg: 'rgba(239,68,68,0.08)',   border: 'rgba(239,68,68,0.25)',   text: '#ef4444', icon: '✗' },
        pending: { bg: 'rgba(100,116,139,0.08)', border: 'rgba(100,116,139,0.25)', text: '#64748b', icon: '…' },
    };
    const c = colors[level] || colors.pending;
    return (
        <div style={{
            display: 'flex', alignItems: 'flex-start', gap: '10px',
            padding: '8px 10px', background: c.bg,
            border: `1px solid ${c.border}`, borderRadius: '7px',
        }}>
            <div style={{
                width: '22px', height: '22px', borderRadius: '50%',
                background: c.border, display: 'flex', alignItems: 'center',
                justifyContent: 'center', fontSize: '0.75rem', fontWeight: 700,
                color: c.text, flexShrink: 0, marginTop: '1px'
            }}>{c.icon}</div>
            <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-primary)' }}>{title}</div>
                {detail && (
                    <div style={{ fontSize: '0.77rem', color: 'var(--text-secondary)', marginTop: '2px', lineHeight: 1.4 }}>
                        {detail}
                    </div>
                )}
            </div>
        </div>
    );
}

export default function FraudSignalsPanel({ caseId, document }) {
    const [signals, setSignals] = useState(null);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (!document) { setSignals(null); return; }
        setLoading(true);
        fetch(`/api/cases/${caseId}/documents/${document.documentID}/signals`, {
            headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
        })
            .then(r => r.ok ? r.json() : null)
            .then(data => setSignals(data))
            .catch(() => setSignals(null))
            .finally(() => setLoading(false));
    }, [caseId, document?.documentID]);

    const warnCount  = signals ? Object.values(signals).filter(s => s.level === LEVEL.WARN).length : 0;
    const failCount  = signals ? Object.values(signals).filter(s => s.level === LEVEL.FAIL).length : 0;

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {/* Header */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                <span style={{
                    background: 'rgba(245,158,11,0.1)', color: '#f59e0b',
                    border: '1px solid rgba(245,158,11,0.3)',
                    fontSize: '0.7rem', fontWeight: 700, letterSpacing: '0.08em',
                    padding: '2px 7px', borderRadius: '4px', textTransform: 'uppercase'
                }}>🛡 Tika · PDFBox · OpenCV</span>
                {signals && (failCount > 0 || warnCount > 0) && (
                    <span style={{
                        fontSize: '0.75rem', fontWeight: 600,
                        color: failCount > 0 ? '#ef4444' : '#f59e0b'
                    }}>
                        {failCount > 0 ? `${failCount} fail${failCount > 1 ? 's' : ''}` : ''}
                        {failCount > 0 && warnCount > 0 ? ' · ' : ''}
                        {warnCount > 0 ? `${warnCount} warning${warnCount > 1 ? 's' : ''}` : ''}
                    </span>
                )}
            </div>

            {!document && (
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontStyle: 'italic' }}>
                    Select a document to view verification signals.
                </p>
            )}

            {loading && (
                <div style={{ padding: '1.5rem', textAlign: 'center', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                    Analysing document…
                </div>
            )}

            {/* Static structure shown while OCR/signals API not wired up */}
            {document && !loading && !signals && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '7px' }}>
                    <SignalRow
                        title="MRZ Check Digits"
                        detail="Awaiting OCR extraction"
                        level={LEVEL.PENDING}
                    />
                    <SignalRow
                        title="PDF Digital Signature"
                        detail={document.documentName?.endsWith('.pdf') ? 'Awaiting PDFBox verification' : 'N/A — not a PDF'}
                        level={LEVEL.PENDING}
                    />
                    <SignalRow
                        title="EXIF Metadata"
                        detail="Awaiting Apache Tika extraction"
                        level={LEVEL.PENDING}
                    />
                    <SignalRow
                        title="Photo Zone Analysis"
                        detail="Awaiting OpenCV ELA check"
                        level={LEVEL.PENDING}
                    />
                    <SignalRow
                        title="Barcode vs OCR Cross-Check"
                        detail={document.category === 'IDENTIFICATION' ? 'Awaiting ZXing decode' : 'N/A for this category'}
                        level={LEVEL.PENDING}
                    />
                    <div style={{
                        marginTop: '6px',
                        background: 'rgba(59,130,246,0.08)',
                        border: '1px solid rgba(59,130,246,0.2)',
                        borderRadius: '7px', padding: '8px 12px',
                        fontSize: '0.78rem', color: '#60a5fa', lineHeight: 1.5
                    }}>
                        ℹ Signals will populate automatically once the <strong>document-service</strong> backend is deployed with Tess4J, Tika, PDFBox and OpenCV.
                    </div>
                </div>
            )}

            {/* Live signals from API */}
            {signals && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '7px' }}>
                    {signals.mrzCheckDigits && (
                        <SignalRow title="MRZ Check Digits" detail={signals.mrzCheckDigits.detail} level={signals.mrzCheckDigits.level} />
                    )}
                    {signals.pdfSignature && (
                        <SignalRow title="PDF Digital Signature" detail={signals.pdfSignature.detail} level={signals.pdfSignature.level} />
                    )}
                    {signals.exifMetadata && (
                        <SignalRow title="EXIF Metadata" detail={signals.exifMetadata.detail} level={signals.exifMetadata.level} />
                    )}
                    {signals.photoZoneEla && (
                        <SignalRow title="Photo Zone (ELA)" detail={signals.photoZoneEla.detail} level={signals.photoZoneEla.level} />
                    )}
                    {signals.faceDetected && (
                        <SignalRow title="Face Detection" detail={signals.faceDetected.detail} level={signals.faceDetected.level} />
                    )}
                    {signals.barcodeOcrMatch && (
                        <SignalRow title="Barcode vs OCR" detail={signals.barcodeOcrMatch.detail} level={signals.barcodeOcrMatch.level} />
                    )}
                    {(failCount > 0 || warnCount > 0) && (
                        <div style={{
                            marginTop: '4px',
                            background: failCount > 0 ? 'rgba(239,68,68,0.08)' : 'rgba(245,158,11,0.08)',
                            border: `1px solid ${failCount > 0 ? 'rgba(239,68,68,0.25)' : 'rgba(245,158,11,0.25)'}`,
                            borderRadius: '7px', padding: '8px 12px',
                            fontSize: '0.8rem',
                            color: failCount > 0 ? '#ef4444' : '#f59e0b'
                        }}>
                            {failCount > 0
                                ? `⚠ ${failCount} signal${failCount > 1 ? 's' : ''} require analyst review before approval.`
                                : `ℹ ${warnCount} signal${warnCount > 1 ? 's' : ''} noted — review recommended.`}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
