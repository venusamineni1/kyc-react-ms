package com.venus.kyc.viewer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventService {
    private final CaseEventRepository eventRepository;

    public EventService(CaseEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public void logEvent(Long caseId, String eventType, String description, String source) {
        eventRepository.addEvent(caseId, eventType, description, source);
    }

    public List<CaseEvent> getEventsForCase(Long caseId) {
        return eventRepository.findEventsByCaseId(caseId);
    }
}
