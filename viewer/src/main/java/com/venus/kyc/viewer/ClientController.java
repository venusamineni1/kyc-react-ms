package com.venus.kyc.viewer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Client Management", description = "Endpoints for managing clients, material changes, related parties, and triggering risk/screening workflows")
public class ClientController {

    private final ClientRepository clientRepository;
    private final MaterialChangeRepository materialChangeRepository;
    private final UserAuditService userAuditService;
    private final ClientDataChangeService clientDataChangeService;
    private final MaterialChangeConfigRepository configRepository;
    private final com.venus.kyc.viewer.risk.RiskAssessmentService riskService;
    private final com.venus.kyc.viewer.screening.ScreeningService screeningService;

    public ClientController(ClientRepository clientRepository, MaterialChangeRepository materialChangeRepository,
            UserAuditService userAuditService, ClientDataChangeService clientDataChangeService,
            MaterialChangeConfigRepository configRepository,
            com.venus.kyc.viewer.risk.RiskAssessmentService riskService,
            com.venus.kyc.viewer.screening.ScreeningService screeningService) {
        this.clientRepository = clientRepository;
        this.materialChangeRepository = materialChangeRepository;
        this.userAuditService = userAuditService;
        this.clientDataChangeService = clientDataChangeService;
        this.configRepository = configRepository;
        this.riskService = riskService;
        this.screeningService = screeningService;
    }

    @Operation(summary = "Get material changes", description = "Returns paginated material changes with optional date filtering and sorting")
    @GetMapping("/changes")
    public PaginatedResponse<MaterialChange> getMaterialChanges(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String startDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String endDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "changeDate") String sortBy,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "DESC") String sortDir,
            org.springframework.security.core.Authentication authentication) {
        userAuditService.log(authentication.getName(), "VIEW_CHANGES", "Viewed material changes. Page: " + page);
        return materialChangeRepository.findAllPaginated(page, size, startDate, endDate, sortBy, sortDir);
    }

    @Operation(summary = "Export material changes", description = "Exports material changes as a flat list for download, with optional date filtering")
    @GetMapping("/changes/export")
    public List<MaterialChange> exportMaterialChanges(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String startDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String endDate) {
        // Can't easily log user here if auth not passed, but usually security context
        // holds it.
        // Skipping specific log for export distinct from view for now unless requested.
        return materialChangeRepository.findAllForExport(startDate, endDate);
    }

    @Operation(summary = "Get all clients", description = "Returns paginated client list with sensitive data masked for non-admin users")
    @GetMapping
    public PaginatedResponse<Client> getAllClients(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size,
            org.springframework.security.core.Authentication authentication) {
        PaginatedResponse<Client> response = clientRepository.findAllPaginated(page, size);
        if (isAdmin(authentication)) {
            return response;
        }
        List<Client> maskedContent = response.content().stream().map(this::maskSensitiveData).toList();
        return new PaginatedResponse<>(maskedContent, response.currentPage(), response.pageSize(),
                response.totalElements(), response.totalPages());
    }

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ClientController.class);

    @Operation(summary = "Get client by ID", description = "Returns client details by ID with sensitive data masked for non-admin users")
    @GetMapping("/{id}")
    public ResponseEntity<Client> getClientById(@Parameter(description = "Client ID") @PathVariable Long id,
            org.springframework.security.core.Authentication authentication) {
        userAuditService.log(authentication.getName(), "VIEW_CLIENT", "Viewed Client ID: " + id);
        return clientRepository.findById(id)
                .map(client -> {
                    logger.info("DEBUG Authorities: {}", authentication.getAuthorities());
                    return isAdmin(authentication) ? client : maskSensitiveData(client);
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Add related party", description = "Adds a related party to a client")
    @PostMapping("/{id}/related-parties")
    public ResponseEntity<Void> addRelatedParty(@Parameter(description = "Client ID") @PathVariable Long id,
            @RequestBody RelatedParty rp) {
        clientRepository.saveRelatedParty(id, rp);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get related party by ID", description = "Returns a specific related party with sensitive data masked for non-admin users")
    @GetMapping("/related-parties/{id}")
    public ResponseEntity<RelatedParty> getRelatedPartyById(
            @Parameter(description = "Related party ID") @PathVariable Long id,
            org.springframework.security.core.Authentication authentication) {
        return clientRepository.findRelatedPartyById(id)
                .map(party -> isAdmin(authentication) ? party : maskRelatedPartySensitiveData(party))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Search clients", description = "Searches clients by name with pagination and data masking for non-admin users")
    @GetMapping("/search")
    public PaginatedResponse<Client> searchClients(
            @org.springframework.web.bind.annotation.RequestParam String query,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size,
            org.springframework.security.core.Authentication authentication) {
        PaginatedResponse<Client> response = clientRepository.searchByNamePaginated(query, page, size);
        if (isAdmin(authentication)) {
            return response;
        }
        List<Client> maskedContent = response.content().stream().map(this::maskSensitiveData).toList();
        return new PaginatedResponse<>(maskedContent, response.currentPage(), response.pageSize(),
                response.totalElements(), response.totalPages());
    }

    @Operation(summary = "Ingest client data change", description = "Processes a client data update, detecting and recording material changes")
    @PostMapping("/{id}/ingest")
    public ResponseEntity<Void> ingestClientChange(@Parameter(description = "Client ID") @PathVariable Long id,
            @RequestBody Client newData) {
        Client oldData = clientRepository.findById(id).orElseThrow();
        clientDataChangeService.processClientChanges(oldData, newData);
        clientRepository.updateClient(newData);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get client material changes", description = "Returns all material changes for a specific client")
    @GetMapping("/{id}/changes")
    public List<MaterialChange> getClientMaterialChanges(@Parameter(description = "Client ID") @PathVariable Long id) {
        return materialChangeRepository.findByClientId(id);
    }

    @Operation(summary = "Trigger risk for change", description = "Triggers a risk assessment for the client related to a material change")
    @PostMapping("/changes/{changeId}/trigger-risk")
    public ResponseEntity<Void> triggerRiskForChange(
            @Parameter(description = "Material change ID") @PathVariable Long changeId) {
        MaterialChange mc = materialChangeRepository.findById(changeId).orElseThrow();
        riskService.evaluateRiskForClient(mc.clientID());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Trigger screening for change", description = "Triggers a screening for the client related to a material change")
    @PostMapping("/changes/{changeId}/trigger-screening")
    public ResponseEntity<Void> triggerScreeningForChange(
            @Parameter(description = "Material change ID") @PathVariable Long changeId) {
        MaterialChange mc = materialChangeRepository.findById(changeId).orElseThrow();
        screeningService.initiateScreening(mc.clientID());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Mark change as reviewed", description = "Updates a material change status to REVIEWED")
    @PostMapping("/changes/{changeId}/review")
    public ResponseEntity<Void> markAsReviewed(
            @Parameter(description = "Material change ID") @PathVariable Long changeId) {
        materialChangeRepository.updateStatus(changeId, "REVIEWED");
        return ResponseEntity.ok().build();
    }

    // Admin Config Endpoints
    @Operation(summary = "Get material change configs", description = "Returns all material change detection configuration rules")
    @GetMapping("/admin/configs")
    public List<MaterialChangeConfig> getConfigs() {
        return configRepository.findAll();
    }

    @Operation(summary = "Save material change config", description = "Saves or updates a material change detection configuration rule")
    @PostMapping("/admin/configs")
    public ResponseEntity<Void> saveConfig(@RequestBody MaterialChangeConfig config) {
        configRepository.save(config);
        return ResponseEntity.ok().build();
    }

    private boolean isAdmin(org.springframework.security.core.Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
    }

    private Client maskSensitiveData(Client client) {
        // Return a new Client record with null addresses, identifiers, and related
        // parties
        return new Client(
                client.clientID(),
                client.titlePrefix(),
                client.firstName(),
                client.middleName(),
                client.lastName(),
                client.titleSuffix(),
                client.citizenship1(),
                client.citizenship2(),
                client.onboardingDate(),
                client.status(),
                client.nameAtBirth(),
                client.nickName(),
                client.gender(),
                client.dateOfBirth(),
                client.language(),
                client.occupation(),
                client.countryOfTax(),
                client.sourceOfFundsCountry(),
                client.fatcaStatus(),
                client.crsStatus(),
                client.placeOfBirth(),
                client.cityOfBirth(),
                client.countryOfBirth(),
                null, // Masked addresses
                null, // Masked identifiers
                null, // Masked related parties
                null, // Masked accounts
                null // Masked portfolios
        );
    }

    private RelatedParty maskRelatedPartySensitiveData(RelatedParty party) {
        return new RelatedParty(
                party.relatedPartyID(),
                party.clientID(),
                party.relationType(),
                party.titlePrefix(),
                party.firstName(),
                party.middleName(),
                party.lastName(),
                party.titleSuffix(),
                party.citizenship1(),
                party.citizenship2(),
                party.onboardingDate(),
                party.status(),
                party.nameAtBirth(),
                party.nickName(),
                party.gender(),
                party.dateOfBirth(),
                party.language(),
                party.occupation(),
                party.countryOfTax(),
                party.sourceOfFundsCountry(),
                party.fatcaStatus(),
                party.crsStatus(),
                null, // Masked addresses
                null // Masked identifiers
        );
    }
}
