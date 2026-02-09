package com.venus.kyc.document;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
    private final DocumentRepository repository;

    public DocumentController(DocumentRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Document> getDocuments(@RequestParam Long caseId) {
        return repository.findByCaseId(caseId);
    }

    @GetMapping("/versions")
    public List<Document> getDocumentVersions(@RequestParam Long caseId, @RequestParam String name) {
        return repository.findVersions(caseId, name);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id) {
        return repository.findById(id)
                .map(doc -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(doc.mimeType()))
                        .header("Content-Disposition", "attachment; filename=\"" + doc.documentName() + "\"")
                        .body(doc.data()))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<String> uploadDocument(
            @RequestParam("caseId") Long caseId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "General") String category,
            @RequestParam(value = "comment", defaultValue = "") String comment,
            @RequestParam("uploadedBy") String uploadedBy,
            @RequestParam(value = "documentName", required = false) String documentName) {
        try {
            String nameToUse = (documentName != null && !documentName.trim().isEmpty()) ? documentName
                    : file.getOriginalFilename();
            repository.save(
                    caseId,
                    nameToUse,
                    category,
                    file.getContentType(),
                    uploadedBy,
                    comment,
                    file.getBytes());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Upload failed", e);
            return ResponseEntity.internalServerError().body("Upload failed: " + e.getMessage());
        }
    }
}
