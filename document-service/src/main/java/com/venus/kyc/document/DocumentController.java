package com.venus.kyc.document;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Document Management", description = "Endpoints for uploading, downloading, and versioning case documents")
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
    private final DocumentRepository repository;

    public DocumentController(DocumentRepository repository) {
        this.repository = repository;
    }

    @Operation(summary = "Get documents by case", description = "Returns all documents associated with a specific case")
    @GetMapping
    public List<Document> getDocuments(@Parameter(description = "Case ID") @RequestParam Long caseId) {
        return repository.findByCaseId(caseId);
    }

    @Operation(summary = "Get document versions", description = "Returns all versions of a specific document in a case")
    @GetMapping("/versions")
    public List<Document> getDocumentVersions(@Parameter(description = "Case ID") @RequestParam Long caseId,
            @Parameter(description = "Document name") @RequestParam String name) {
        return repository.findVersions(caseId, name);
    }

    @Operation(summary = "Download a document", description = "Downloads the binary content of a document by its ID")
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> downloadDocument(@Parameter(description = "Document ID") @PathVariable Long id) {
        return repository.findById(id)
                .map(doc -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(doc.mimeType()))
                        .header("Content-Disposition", "attachment; filename=\"" + doc.documentName() + "\"")
                        .body(doc.data()))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Upload a document", description = "Uploads a new document file and associates it with a case, supporting versioning and categorization")
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
