import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { useAuth } from './AuthContext';
import { caseService } from '../services/caseService';

const InboxContext = createContext();

export const useInbox = () => {
    const context = useContext(InboxContext);
    if (!context) {
        throw new Error('useInbox must be used within an InboxProvider');
    }
    return context;
};

export const InboxProvider = ({ children }) => {
    const { user } = useAuth();
    const [inboxCount, setInboxCount] = useState(0);

    const refreshInbox = useCallback(async () => {
        if (!user) {
            setInboxCount(0);
            return;
        }
        try {
            // Safety check: verify caseService exists and has getUserTasks
            if (caseService && typeof caseService.getUserTasks === 'function') {
                const tasks = await caseService.getUserTasks();
                if (Array.isArray(tasks)) {
                    setInboxCount(tasks.length);
                } else {
                    setInboxCount(0);
                }
            }
        } catch (error) {
            console.warn("Inbox fetch failed silently:", error);
            // Non-blocking error
        }
    }, [user]);

    useEffect(() => {
        refreshInbox();

        // Optional: Polling interval? For now fast follow strictly what's needed.
        // Let's just fetch on mount/user change.
    }, [refreshInbox]);

    return (
        <InboxContext.Provider value={{ inboxCount, refreshInbox }}>
            {children}
        </InboxContext.Provider>
    );
};
