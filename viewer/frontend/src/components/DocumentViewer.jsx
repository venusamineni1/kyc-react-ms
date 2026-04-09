import React, { useState, useEffect, useRef } from 'react';
import { Worker, Viewer } from '@react-pdf-viewer/core';
import { defaultLayoutPlugin } from '@react-pdf-viewer/default-layout';
import { highlightPlugin, Trigger } from '@react-pdf-viewer/highlight';
import '@react-pdf-viewer/core/lib/styles/index.css';
import '@react-pdf-viewer/default-layout/lib/styles/index.css';
import '@react-pdf-viewer/highlight/lib/styles/index.css';
import { Annotorious, ImageAnnotator, ImageAnnotationPopup, useAnnotator } from '@annotorious/react';
import '@annotorious/react/annotorious-react.css';
import { caseService } from '../services/caseService';

const WORKER_URL = '/pdfjs/pdf.worker.min.js';

const IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'tiff', 'tif', 'webp', 'heic'];

// Fetch a document URL with the JWT auth header and return a blob URL.
// This is needed because the PDF viewer and <img> tags cannot send auth headers.
function useAuthenticatedBlobUrl(apiUrl) {
    const [blobUrl, setBlobUrl] = useState(null);
    const [fetchError, setFetchError] = useState(null);

    useEffect(() => {
        if (!apiUrl) return;
        let objectUrl = null;
        setBlobUrl(null);
        setFetchError(null);

        const token = localStorage.getItem('token');
        fetch(apiUrl, {
            headers: token ? { Authorization: `Bearer ${token}` } : {}
        })
            .then(r => {
                if (!r.ok) throw new Error(`${r.status} ${r.statusText}`);
                return r.blob();
            })
            .then(blob => {
                objectUrl = URL.createObjectURL(blob);
                setBlobUrl(objectUrl);
            })
            .catch(err => setFetchError(err.message));

        // Revoke previous blob URL on cleanup to avoid memory leaks
        return () => { if (objectUrl) URL.revokeObjectURL(objectUrl); };
    }, [apiUrl]);

    return { blobUrl, fetchError };
}

function getFileExt(name = '') {
    return name.split('.').pop().toLowerCase();
}

function isPdf(name) {
    return getFileExt(name) === 'pdf';
}

function isImage(name) {
    return IMAGE_EXTENSIONS.includes(getFileExt(name));
}

// ── Annotation Sidebar ──────────────────────────────────────────────────────
function AnnotationSidebar({ annotations, onAdd, currentUser }) {
    const [draft, setDraft] = useState('');
    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {annotations.length === 0 && (
                <p style={{ color: 'var(--text-secondary)', fontStyle: 'italic', fontSize: '0.85rem' }}>
                    No annotations yet. Select a region on the document to add one.
                </p>
            )}
            {annotations.map((ann, i) => (
                <div key={i} style={{
                    background: 'rgba(139,92,246,0.08)',
                    border: '1px solid rgba(139,92,246,0.3)',
                    borderLeft: '3px solid #8b5cf6',
                    borderRadius: '6px',
                    padding: '10px 12px',
                }}>
                    <div style={{ fontSize: '0.8rem', fontWeight: 700, color: '#c084fc', marginBottom: '4px' }}>
                        {ann.label || 'Annotation'}
                    </div>
                    <div style={{ fontSize: '0.85rem', color: 'var(--text-primary)' }}>{ann.body}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '4px' }}>
                        {ann.creator} · {ann.created}
                    </div>
                </div>
            ))}
            <div style={{ marginTop: '6px', display: 'flex', gap: '8px' }}>
                <input
                    value={draft}
                    onChange={e => setDraft(e.target.value)}
                    placeholder="Add annotation note..."
                    style={{
                        flex: 1, background: 'var(--glass-bg)', border: '1px solid var(--glass-border)',
                        borderRadius: '6px', padding: '6px 10px', color: 'var(--text-primary)', fontSize: '0.85rem'
                    }}
                    onKeyDown={e => {
                        if (e.key === 'Enter' && draft.trim()) {
                            onAdd(draft.trim());
                            setDraft('');
                        }
                    }}
                />
                <button
                    onClick={() => { if (draft.trim()) { onAdd(draft.trim()); setDraft(''); } }}
                    style={{
                        background: '#7c3aed', border: 'none', color: '#fff',
                        borderRadius: '6px', padding: '6px 12px', cursor: 'pointer', fontSize: '0.82rem'
                    }}
                >+ Add</button>
            </div>
        </div>
    );
}

// ── Annotorious Image Viewer (for image-type documents) ─────────────────────

function NotePopup({ autoFocus, annotation, onCreateBody, onAnnotationSaved }) {
    const [note, setNote] = React.useState('');
    const save = async () => {
        if (!note.trim()) return;
        onCreateBody({ type: 'TextualBody', value: note.trim(), purpose: 'commenting' });
        await onAnnotationSaved(annotation, note.trim());
    };
    return (
        <div style={{
            background: '#1e1b4b', border: '1px solid #7c3aed', borderRadius: '6px',
            padding: '10px', minWidth: '200px', boxShadow: '0 4px 16px rgba(0,0,0,0.5)',
        }}>
            <input
                autoFocus={autoFocus}
                placeholder="Add a note..."
                value={note}
                onChange={e => setNote(e.target.value)}
                onKeyDown={e => { if (e.key === 'Enter') save(); }}
                style={{
                    width: '100%', background: 'transparent', color: '#fff',
                    border: 'none', borderBottom: '1px solid #7c3aed',
                    outline: 'none', fontSize: '13px', paddingBottom: '4px', boxSizing: 'border-box'
                }}
            />
            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '8px', gap: '6px' }}>
                <button onClick={save} style={{
                    background: '#7c3aed', color: '#fff', border: 'none',
                    borderRadius: '4px', padding: '3px 10px', cursor: 'pointer', fontSize: '12px'
                }}>Save</button>
            </div>
        </div>
    );
}

// Must be rendered inside <Annotorious> to use useAnnotator
function AnnotoriousImageInner({ url, name, docAnnotations, onAnnotationSaved }) {
    const anno = useAnnotator();

    // Load existing geometric annotations into the canvas when ready
    useEffect(() => {
        if (!anno) return;
        const geometricAnns = docAnnotations.filter(a => a.geometry).map(a => a.geometry);
        if (geometricAnns.length) anno.setAnnotations(geometricAnns, true);
    }, [anno, docAnnotations]);

    return (
        <>
            <ImageAnnotator tool="rectangle">
                <img
                    src={url}
                    alt={name}
                    style={{ maxWidth: '100%', borderRadius: '4px', boxShadow: '0 4px 24px rgba(0,0,0,0.5)' }}
                />
            </ImageAnnotator>

            {/* Popup shown after user draws a shape — user enters a note and saves */}
            <ImageAnnotationPopup popup={(props) => (
                <NotePopup 
                    autoFocus
                    annotation={props.annotation}
                    onCreateBody={props.onCreateBody}
                    onAnnotationSaved={onAnnotationSaved}
                />
            )} />
        </>
    );
}

function AnnotoriousImageViewer({ url, name, docAnnotations, onAnnotationSaved }) {
    return (
        <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
            {/* Toolbar — download only (zoom/rotate omitted: CSS transforms break Annotorious coordinates) */}
            <div style={{
                background: 'var(--glass-bg)', borderBottom: '1px solid var(--glass-border)',
                padding: '8px 12px', display: 'flex', gap: '8px', alignItems: 'center', flexShrink: 0
            }}>
                <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                    Draw a rectangle to annotate a region
                </span>
                <a href={url} download={name} className="dv-tool-btn"
                    style={{ textDecoration: 'none', marginLeft: 'auto' }}>⬇ Download</a>
            </div>
            <div style={{
                flex: 1, overflow: 'auto', display: 'flex',
                alignItems: 'center', justifyContent: 'center',
                background: '#1a1a2e', padding: '16px'
            }}>
                <Annotorious>
                    <AnnotoriousImageInner
                        url={url}
                        name={name}
                        docAnnotations={docAnnotations}
                        onAnnotationSaved={onAnnotationSaved}
                    />
                </Annotorious>
            </div>
        </div>
    );
}

// ── Main DocumentViewer Component ───────────────────────────────────────────
export default function DocumentViewer({ docs, caseId, currentUser, onUploadNew, onDocumentSelect, hideSelector = false }) {
    const [selectedDoc, setSelectedDoc] = useState(docs[0] || null);

    useEffect(() => {
        if (docs[0]) {
            setSelectedDoc(docs[0]);
            onDocumentSelect?.(docs[0]);
        }
    }, []); // run once on mount

    const [showAnnotations, setShowAnnotations] = useState(false);
    const [annotations, setAnnotations] = useState({});
    const [annotationsLoaded, setAnnotationsLoaded] = useState(new Set());
    const [highlightNote, setHighlightNote] = useState('');

    const selected = selectedDoc;
    const rawDocUrl = selected ? `/api/cases/documents/${selected.documentID}` : null;
    const { blobUrl, fetchError } = useAuthenticatedBlobUrl(rawDocUrl);
    const docAnnotations = (selected && annotations[selected.documentID]) || [];

    // Load annotations from backend when document changes
    useEffect(() => {
        if (!selected || !caseId || annotationsLoaded.has(selected.documentID)) return;
        caseService.getDocumentAnnotations(caseId, selected.documentID)
            .then(data => {
                setAnnotations(prev => ({
                    ...prev,
                    [selected.documentID]: data.map(a => ({
                        body: a.annotationText,
                        label: a.label || 'Analyst Note',
                        creator: a.userID,
                        created: new Date(a.createdDate).toLocaleString(),
                        geometry: a.geometry ? JSON.parse(a.geometry) : null,
                    }))
                }));
                setAnnotationsLoaded(prev => new Set([...prev, selected.documentID]));
            })
            .catch(err => console.error('Failed to load annotations', err));
    }, [selected?.documentID, caseId]);

    // Persist annotation to backend and update local state
    const persistAnnotation = async (text, geometry = null, label = 'Analyst Note') => {
        if (!selected || !caseId) return;
        await caseService.addDocumentAnnotation(caseId, selected.documentID, text, geometry, label);
        setAnnotations(prev => ({
            ...prev,
            [selected.documentID]: [
                ...(prev[selected.documentID] || []),
                { body: text, label, creator: currentUser || 'analyst', created: new Date().toLocaleString(), geometry }
            ]
        }));
    };

    // Sidebar freeform note (text only, no geometry)
    const handleAddAnnotation = (text) => {
        persistAnnotation(text, null, 'Analyst Note');
    };

    // Annotorious shape callback — called after user draws a region and enters a note
    const handleImageAnnotationSaved = async (annotation, noteText) => {
        await persistAnnotation(noteText, annotation, 'Region');
    };

    // PDF highlight plugin — renders a note popup on text selection
    const highlightNoteRef = useRef('');
    const renderHighlightTarget = (props) => (
        <div style={{
            position: 'absolute',
            left: `${props.selectionRegion.left}%`,
            top: `${props.selectionRegion.top + props.selectionRegion.height}%`,
            background: '#1e1b4b', border: '1px solid #7c3aed', borderRadius: '6px',
            padding: '8px', zIndex: 9999, minWidth: '220px',
            boxShadow: '0 4px 16px rgba(0,0,0,0.5)',
        }}>
            <input
                autoFocus
                placeholder="Add a note..."
                defaultValue=""
                onChange={e => { highlightNoteRef.current = e.target.value; }}
                onKeyDown={async (e) => {
                    if (e.key === 'Enter' && highlightNoteRef.current.trim()) {
                        await persistAnnotation(highlightNoteRef.current.trim(), props.highlightAreas, 'Highlight');
                        highlightNoteRef.current = '';
                        props.cancel();
                    }
                }}
                style={{
                    width: '100%', background: 'transparent', color: '#fff',
                    border: 'none', outline: 'none', fontSize: '13px', boxSizing: 'border-box'
                }}
            />
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '6px', marginTop: '6px' }}>
                <button onClick={props.cancel}
                    style={{ fontSize: '11px', background: 'transparent', color: '#aaa', border: 'none', cursor: 'pointer' }}>
                    Cancel
                </button>
                <button onClick={async () => {
                    if (highlightNoteRef.current.trim()) {
                        await persistAnnotation(highlightNoteRef.current.trim(), props.highlightAreas, 'Highlight');
                        highlightNoteRef.current = '';
                        props.cancel();
                    }
                }} style={{
                    fontSize: '11px', background: '#7c3aed', color: '#fff',
                    border: 'none', borderRadius: '4px', padding: '2px 8px', cursor: 'pointer'
                }}>Save</button>
            </div>
        </div>
    );

    const renderHighlights = (props) => {
        const docAnns = (annotations[selected?.documentID] || [])
            .filter(a => a.geometry && a.label === 'Highlight');
        return (
            <>
                {docAnns.flatMap((ann, i) =>
                    (ann.geometry || [])
                        .filter(area => area.pageIndex === props.pageIndex)
                        .map((area, j) => (
                            <div key={`${i}-${j}`} style={{
                                background: 'rgba(124, 58, 237, 0.3)',
                                ...props.getCssProperties(area, props.rotation),
                            }} title={ann.body} />
                        ))
                )}
            </>
        );
    };

    const defaultLayoutPluginInstance = defaultLayoutPlugin({
        sidebarTabs: (defaultTabs) => [defaultTabs[0], defaultTabs[1]],
    });

    const highlightPluginInstance = highlightPlugin({
        trigger: Trigger.TextSelection,
        renderHighlightTarget,
        renderHighlights,
    });

    if (!docs || docs.length === 0) {
        return (
            <div style={{
                border: '2px dashed var(--glass-border)', borderRadius: '10px',
                padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)'
            }}>
                <div style={{ fontSize: '2.5rem', marginBottom: '1rem' }}>📄</div>
                <p>No documents uploaded yet.</p>
                <button
                    onClick={onUploadNew}
                    style={{
                        marginTop: '1rem', background: 'var(--primary-color)', border: 'none',
                        color: '#fff', padding: '8px 20px', borderRadius: '7px', cursor: 'pointer'
                    }}
                >Upload First Document</button>
            </div>
        );
    }

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {/* Document Selector Tabs — hidden in single-doc modal mode */}
            {!hideSelector && <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', alignItems: 'center' }}>
                {docs.map(doc => (
                    <button
                        key={doc.documentID}
                        onClick={() => { setSelectedDoc(doc); onDocumentSelect?.(doc); }}
                        style={{
                            padding: '6px 14px',
                            borderRadius: '6px',
                            border: `1px solid ${selected?.documentID === doc.documentID ? 'var(--primary-color)' : 'var(--glass-border)'}`,
                            background: selected?.documentID === doc.documentID ? 'var(--primary-color)' : 'var(--glass-bg)',
                            color: selected?.documentID === doc.documentID ? '#fff' : 'var(--text-secondary)',
                            cursor: 'pointer',
                            fontSize: '0.82rem',
                            fontWeight: selected?.documentID === doc.documentID ? 700 : 400,
                            display: 'flex', alignItems: 'center', gap: '6px',
                            transition: 'all 0.15s'
                        }}
                    >
                        <span>{isPdf(doc.documentName) ? '📋' : isImage(doc.documentName) ? '🖼️' : '📎'}</span>
                        <span>{doc.documentName}</span>
                        <span style={{
                            fontSize: '0.7rem', background: 'rgba(255,255,255,0.1)',
                            padding: '1px 5px', borderRadius: '99px'
                        }}>v{doc.version || 1}</span>
                    </button>
                ))}
                <button
                    onClick={onUploadNew}
                    style={{
                        marginLeft: 'auto', padding: '6px 14px', borderRadius: '6px',
                        border: '1px dashed var(--glass-border)', background: 'transparent',
                        color: 'var(--text-secondary)', cursor: 'pointer', fontSize: '0.82rem'
                    }}
                >+ Upload New</button>
            </div>}

            {/* Viewer + Annotation Sidebar */}
            <div style={{ display: 'flex', gap: '12px', height: '520px' }}>
                {/* Viewer Panel */}
                <div style={{
                    flex: 1,
                    border: '1px solid var(--glass-border)',
                    borderRadius: '10px',
                    overflow: 'hidden',
                    background: '#1a1a2e',
                    minWidth: 0
                }}>
                    {/* Loading state while fetching blob */}
                    {selected && !blobUrl && !fetchError && (
                        <div style={{
                            height: '100%', display: 'flex', alignItems: 'center',
                            justifyContent: 'center', flexDirection: 'column', gap: '12px',
                            color: 'var(--text-secondary)'
                        }}>
                            <div style={{
                                width: '32px', height: '32px',
                                border: '3px solid var(--glass-border)',
                                borderTopColor: 'var(--primary-color)',
                                borderRadius: '50%',
                                animation: 'spin 0.8s linear infinite'
                            }} />
                            <span style={{ fontSize: '0.85rem' }}>Loading document…</span>
                        </div>
                    )}
                    {/* Auth/fetch error state */}
                    {fetchError && (
                        <div style={{
                            height: '100%', display: 'flex', alignItems: 'center',
                            justifyContent: 'center', flexDirection: 'column', gap: '12px',
                            color: 'var(--text-secondary)', padding: '2rem', textAlign: 'center'
                        }}>
                            <div style={{ fontSize: '2rem' }}>⚠️</div>
                            <div style={{ fontWeight: 600, color: '#ef4444' }}>Failed to load document</div>
                            <div style={{ fontSize: '0.82rem' }}>{fetchError}</div>
                        </div>
                    )}
                    {/* PDF viewer with text selection highlight */}
                    {blobUrl && isPdf(selected?.documentName) && (
                        <Worker workerUrl={WORKER_URL}>
                            <div style={{ height: '100%' }}>
                                <Viewer
                                    fileUrl={blobUrl}
                                    plugins={[defaultLayoutPluginInstance, highlightPluginInstance]}
                                    theme="dark"
                                />
                            </div>
                        </Worker>
                    )}
                    {/* Image viewer with Annotorious region drawing */}
                    {blobUrl && isImage(selected?.documentName) && (
                        <AnnotoriousImageViewer
                            url={blobUrl}
                            name={selected?.documentName}
                            docAnnotations={docAnnotations}
                            onAnnotationSaved={handleImageAnnotationSaved}
                        />
                    )}
                    {/* Other file types */}
                    {blobUrl && !isPdf(selected?.documentName) && !isImage(selected?.documentName) && (
                        <div style={{
                            height: '100%', display: 'flex', flexDirection: 'column',
                            alignItems: 'center', justifyContent: 'center', gap: '16px',
                            color: 'var(--text-secondary)'
                        }}>
                            <div style={{ fontSize: '3rem' }}>📎</div>
                            <div style={{ fontWeight: 600 }}>{selected?.documentName}</div>
                            <a
                                href={blobUrl}
                                download={selected?.documentName}
                                style={{
                                    background: 'var(--primary-color)', color: '#fff',
                                    padding: '8px 20px', borderRadius: '7px',
                                    textDecoration: 'none', fontSize: '0.9rem'
                                }}
                            >⬇ Download File</a>
                        </div>
                    )}
                </div>

                {/* Annotation Toggle Panel */}
                <div style={{ width: showAnnotations ? '280px' : '40px', flexShrink: 0, transition: 'width 0.2s' }}>
                    <div style={{
                        background: 'var(--glass-bg)', border: '1px solid var(--glass-border)',
                        borderRadius: '10px', height: '100%', overflow: 'hidden',
                        display: 'flex', flexDirection: 'column'
                    }}>
                        {/* Toggle button */}
                        <button
                            onClick={() => setShowAnnotations(s => !s)}
                            title={showAnnotations ? 'Hide annotations' : 'Show annotations'}
                            style={{
                                background: docAnnotations.length > 0 ? 'rgba(139,92,246,0.15)' : 'transparent',
                                border: 'none',
                                borderBottom: '1px solid var(--glass-border)',
                                color: docAnnotations.length > 0 ? '#c084fc' : 'var(--text-secondary)',
                                padding: '10px',
                                cursor: 'pointer',
                                display: 'flex',
                                alignItems: 'center',
                                gap: '8px',
                                fontSize: '0.82rem',
                                fontWeight: 600,
                                width: '100%',
                                whiteSpace: 'nowrap',
                                overflow: 'hidden'
                            }}
                        >
                            <span>🖊</span>
                            {showAnnotations && (
                                <>
                                    <span>Annotations</span>
                                    {docAnnotations.length > 0 && (
                                        <span style={{
                                            marginLeft: 'auto', background: '#7c3aed', color: '#fff',
                                            fontSize: '0.72rem', padding: '1px 6px', borderRadius: '99px'
                                        }}>{docAnnotations.length}</span>
                                    )}
                                </>
                            )}
                        </button>
                        {showAnnotations && (
                            <div style={{ flex: 1, overflow: 'auto', padding: '12px' }}>
                                <AnnotationSidebar
                                    annotations={docAnnotations}
                                    onAdd={handleAddAnnotation}
                                    currentUser={currentUser}
                                />
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* Document Meta Row */}
            {selected && (
                <div style={{
                    display: 'flex', gap: '16px', flexWrap: 'wrap',
                    background: 'var(--glass-bg)', borderRadius: '8px',
                    padding: '10px 14px', fontSize: '0.8rem', color: 'var(--text-secondary)',
                    border: '1px solid var(--glass-border)'
                }}>
                    <span><strong style={{ color: 'var(--text-primary)' }}>Category:</strong> {selected.category}</span>
                    <span><strong style={{ color: 'var(--text-primary)' }}>Version:</strong> v{selected.version || 1}</span>
                    <span><strong style={{ color: 'var(--text-primary)' }}>Uploaded by:</strong> {selected.uploadedBy}</span>
                    <span><strong style={{ color: 'var(--text-primary)' }}>Date:</strong> {new Date(selected.uploadDate).toLocaleDateString()}</span>
                    {selected.comment && (
                        <span><strong style={{ color: 'var(--text-primary)' }}>Note:</strong> {selected.comment}</span>
                    )}
                    <a
                        href={blobUrl || '#'}
                        download={selected.documentName}
                        style={{ marginLeft: 'auto', color: 'var(--primary-color)', textDecoration: 'none', fontWeight: 600 }}
                    >⬇ Download</a>
                </div>
            )}

            <style>{`
                @keyframes spin { to { transform: rotate(360deg); } }
                .dv-tool-btn {
                    background: rgba(255,255,255,0.08);
                    border: 1px solid var(--glass-border);
                    color: var(--text-primary);
                    padding: 4px 10px;
                    border-radius: 5px;
                    cursor: pointer;
                    font-size: 0.8rem;
                    transition: background 0.15s;
                }
                .dv-tool-btn:hover {
                    background: rgba(255,255,255,0.14);
                }
            `}</style>
        </div>
    );
}
