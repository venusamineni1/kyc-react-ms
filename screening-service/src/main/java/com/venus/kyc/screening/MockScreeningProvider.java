package com.venus.kyc.screening;

import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MockScreeningProvider implements ScreeningProvider {

    private final Random random = new Random();
    // Cache to store the "target" result for a request so it persists across checks
    private final Map<String, Boolean> requestHitMap = new ConcurrentHashMap<>();

    @Override
    public String initiate(ScreeningDTOs.ExternalScreeningRequest request) {
        String reqId = UUID.randomUUID().toString();

        // Deterministic Logic:
        // If name contains "Osama", "Pablo", or "Putin", force a HIT.
        // Otherwise, 10% chance of random hit.
        boolean shouldHit;
        String name = request.name().toLowerCase();
        if (name.contains("osama") || name.contains("pablo") || name.contains("putin")) {
            shouldHit = true;
        } else {
            shouldHit = random.nextInt(100) < 10; // 10% chance
        }

        requestHitMap.put(reqId, shouldHit);
        return reqId;
    }

    @Override
    public List<ScreeningDTOs.ContextResult> checkStatus(String externalRequestId) {
        // Simulate delay: In a real polling scenario, we might return "empty" to say
        // "still working".

        Boolean shouldHit = requestHitMap.getOrDefault(externalRequestId, false);

        List<ScreeningDTOs.ContextResult> results = new ArrayList<>();
        String[] contexts = { "PEP", "ADM", "INT", "SAN" };

        for (String ctx : contexts) {
            String status = "NO_HIT";
            String alertMsg = null;

            // If the request is destined to HIT, make PEP and SAN hit.
            if (shouldHit && (ctx.equals("PEP") || ctx.equals("SAN"))) {
                status = "HIT";
                alertMsg = ctx + " Match found in Mock DB";
            }

            results.add(new ScreeningDTOs.ContextResult(ctx, status, alertMsg));
        }

        return results;
    }
}
