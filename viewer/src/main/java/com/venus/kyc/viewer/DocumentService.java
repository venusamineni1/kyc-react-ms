package com.venus.kyc.viewer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class DocumentService {

    @Value("${document.service.url}")
    private String documentServiceUrl;

    private final RestTemplate restTemplate;

    public DocumentService() {
        this.restTemplate = new RestTemplate();
    }

    public List<CaseDocument> getDocuments(Long caseId) {
        CaseDocument[] docs = restTemplate.getForObject(
                documentServiceUrl + "?caseId=" + caseId,
                CaseDocument[].class);
        return Arrays.asList(Objects.requireNonNullElse(docs, new CaseDocument[0]));
    }

    public List<CaseDocument> getDocumentVersions(Long caseId, String name) {
        CaseDocument[] docs = restTemplate.getForObject(
                documentServiceUrl + "/versions?caseId=" + caseId + "&name=" + name,
                CaseDocument[].class);
        return Arrays.asList(Objects.requireNonNullElse(docs, new CaseDocument[0]));
    }

    public void uploadDocument(Long caseId, MultipartFile file, String category, String comment, String uploadedBy,
            String documentName) {
        String url = documentServiceUrl;

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("caseId", caseId);
        body.add("file", file.getResource());
        body.add("category", category);
        body.add("comment", comment);
        body.add("uploadedBy", uploadedBy);
        if (documentName != null) {
            body.add("documentName", documentName);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        restTemplate.postForEntity(url, requestEntity, String.class);
    }

    public ResponseEntity<byte[]> downloadDocument(Long docId) {
        return restTemplate.getForEntity(documentServiceUrl + "/" + docId, byte[].class);
    }
}
