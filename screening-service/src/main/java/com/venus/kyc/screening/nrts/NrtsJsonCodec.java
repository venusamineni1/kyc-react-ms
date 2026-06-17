package com.venus.kyc.screening.nrts;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * JSON codec stub — activate with {@code nrts.format=json} when NRTS migrates
 * its realtime submit/status API from XML to JSON.
 *
 * Only this class (and NrtsHttpClient if Content-Type headers change) needs
 * to be implemented. NrtsScreeningProvider and everything above it are unaffected.
 */
@Component
@ConditionalOnProperty(name = "nrts.format", havingValue = "json")
public class NrtsJsonCodec implements NrtsPayloadCodec {

    @Override
    public String serializeSubmit(int srcId, List<NrtsRecord> records) {
        throw new UnsupportedOperationException(
                "JSON submit serializer not yet implemented — NRTS migration pending");
    }

    @Override
    public SubmitResult parseSubmitResponse(String body) {
        throw new UnsupportedOperationException(
                "JSON submit response parser not yet implemented — NRTS migration pending");
    }

    @Override
    public StatusResult parseStatusResponse(String body) {
        throw new UnsupportedOperationException(
                "JSON status response parser not yet implemented — NRTS migration pending");
    }
}
