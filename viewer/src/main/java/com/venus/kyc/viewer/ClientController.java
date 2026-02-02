package com.venus.kyc.viewer;

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
public class ClientController {

    private final ClientRepository clientRepository;
    private final MaterialChangeRepository materialChangeRepository;
    private final UserAuditService userAuditService;

    public ClientController(ClientRepository clientRepository, MaterialChangeRepository materialChangeRepository,
            UserAuditService userAuditService) {
        this.clientRepository = clientRepository;
        this.materialChangeRepository = materialChangeRepository;
        this.userAuditService = userAuditService;
    }

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

    @GetMapping("/changes/export")
    public List<MaterialChange> exportMaterialChanges(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String startDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String endDate) {
        // Can't easily log user here if auth not passed, but usually security context
        // holds it.
        // Skipping specific log for export distinct from view for now unless requested.
        return materialChangeRepository.findAllForExport(startDate, endDate);
    }

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

    @GetMapping("/{id}")
    public ResponseEntity<Client> getClientById(@PathVariable Long id,
            org.springframework.security.core.Authentication authentication) {
        userAuditService.log(authentication.getName(), "VIEW_CLIENT", "Viewed Client ID: " + id);
        return clientRepository.findById(id)
                .map(client -> isAdmin(authentication) ? client : maskSensitiveData(client))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/related-parties")
    public ResponseEntity<Void> addRelatedParty(@PathVariable Long id, @RequestBody RelatedParty rp) {
        clientRepository.saveRelatedParty(id, rp);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/related-parties/{id}")
    public ResponseEntity<RelatedParty> getRelatedPartyById(@PathVariable Long id,
            org.springframework.security.core.Authentication authentication) {
        return clientRepository.findRelatedPartyById(id)
                .map(party -> isAdmin(authentication) ? party : maskRelatedPartySensitiveData(party))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

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

    private boolean isAdmin(org.springframework.security.core.Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
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
