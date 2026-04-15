package com.venus.kyc.orchestration.service;

import com.venus.kyc.orchestration.dto.KycStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookNotificationServiceTest {

    @Mock RestTemplate externalRestTemplate;

    private WebhookNotificationService service;

    @BeforeEach
    void setUp() {
        service = new WebhookNotificationService(externalRestTemplate, Runnable::run);
    }

    private KycStatusResponse buildPayload(String kycId) {
        return KycStatusResponse.builder()
                .kycId(kycId)
                .build();
    }

    @Test
    void notify_nullUrl_noInteraction() {
        service.notify(null, buildPayload("x3Yq9mZ1"));
        verifyNoInteractions(externalRestTemplate);
    }

    @Test
    void notify_blankUrl_noInteraction() {
        service.notify("   ", buildPayload("x3Yq9mZ1"));
        verifyNoInteractions(externalRestTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void notify_validUrl_postsPayload() {
        when(externalRestTemplate.postForEntity(any(String.class), any(), eq(Void.class)))
                .thenReturn(ResponseEntity.ok(null));

        service.notify("https://example.com/callback", buildPayload("x3Yq9mZ1"));

        verify(externalRestTemplate, times(1))
                .postForEntity(eq("https://example.com/callback"), any(), eq(Void.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void notify_checksKycIdHeader() {
        when(externalRestTemplate.postForEntity(any(String.class), any(), eq(Void.class)))
                .thenReturn(ResponseEntity.ok(null));

        KycStatusResponse payload = buildPayload("x3Yq9mZ1");
        service.notify("https://example.com/callback", payload);

        ArgumentCaptor<HttpEntity<?>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(externalRestTemplate).postForEntity(any(String.class), captor.capture(), eq(Void.class));

        HttpEntity<?> captured = captor.getValue();
        assertEquals("x3Yq9mZ1", captured.getHeaders().getFirst("X-KYC-Id"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void notify_firstAttemptFails_retriesAndSucceeds() {
        when(externalRestTemplate.postForEntity(any(String.class), any(), eq(Void.class)))
                .thenThrow(new RestClientException("Connection refused"))
                .thenReturn(ResponseEntity.ok(null));

        service.notify("https://example.com/callback", buildPayload("x3Yq9mZ1"));

        verify(externalRestTemplate, times(2))
                .postForEntity(eq("https://example.com/callback"), any(), eq(Void.class));
    }
}
