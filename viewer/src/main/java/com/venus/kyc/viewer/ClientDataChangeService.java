package com.venus.kyc.viewer;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Objects;

@Service
public class ClientDataChangeService {

    private final MaterialChangeRepository materialChangeRepository;
    private final MaterialChangeConfigRepository configRepository;

    public ClientDataChangeService(MaterialChangeRepository materialChangeRepository,
            MaterialChangeConfigRepository configRepository) {
        this.materialChangeRepository = materialChangeRepository;
        this.configRepository = configRepository;
    }

    public void processClientChanges(Client oldClient, Client newClient) {
        Map<String, String> configs = configRepository.getAllConfigsAsMap();
        Long clientID = newClient.clientID();

        // Check Client Core Fields
        compareAndLog(clientID, clientID, "Client", "citizenship1", oldClient.citizenship1(), newClient.citizenship1(),
                configs);
        compareAndLog(clientID, clientID, "Client", "citizenship2", oldClient.citizenship2(), newClient.citizenship2(),
                configs);
        compareAndLog(clientID, clientID, "Client", "occupation", oldClient.occupation(), newClient.occupation(),
                configs);
        compareAndLog(clientID, clientID, "Client", "countryOfTax", oldClient.countryOfTax(), newClient.countryOfTax(),
                configs);
        compareAndLog(clientID, clientID, "Client", "sourceOfFundsCountry", oldClient.sourceOfFundsCountry(),
                newClient.sourceOfFundsCountry(), configs);
        compareAndLog(clientID, clientID, "Client", "fatcaStatus", oldClient.fatcaStatus(), newClient.fatcaStatus(),
                configs);
        compareAndLog(clientID, clientID, "Client", "crsStatus", oldClient.crsStatus(), newClient.crsStatus(), configs);
        compareAndLog(clientID, clientID, "Client", "firstName", oldClient.firstName(), newClient.firstName(), configs);
        compareAndLog(clientID, clientID, "Client", "lastName", oldClient.lastName(), newClient.lastName(), configs);
        compareAndLog(clientID, clientID, "Client", "dateOfBirth",
                oldClient.dateOfBirth() != null ? oldClient.dateOfBirth().toString() : null,
                newClient.dateOfBirth() != null ? newClient.dateOfBirth().toString() : null, configs);

        // Addresses (Simplified list comparison by Type)
        if (newClient.addresses() != null) {
            for (Address newAddr : newClient.addresses()) {
                Address oldAddr = findAddressByType(oldClient.addresses(), newAddr.addressType());
                if (oldAddr == null) {
                    logChange(clientID, newAddr.addressID(), "Address", "ALL", "CREATE", null,
                            "New Address: " + newAddr.country(), configs);
                } else {
                    compareAndLog(clientID, newAddr.addressID(), "Address", "country", oldAddr.country(),
                            newAddr.country(), configs);
                    compareAndLog(clientID, newAddr.addressID(), "Address", "city", oldAddr.city(), newAddr.city(),
                            configs);
                }
            }
        }

        // Identifiers
        if (newClient.identifiers() != null) {
            for (Identifier newId : newClient.identifiers()) {
                Identifier oldId = findIdentifierByType(oldClient.identifiers(), newId.identifierType());
                if (oldId == null) {
                    logChange(clientID, newId.identifierID(), "Identifier", "ALL", "CREATE", null,
                            newId.identifierNumber(), configs);
                } else {
                    compareAndLog(clientID, newId.identifierID(), "Identifier", "identifierNumber",
                            oldId.identifierNumber(), newId.identifierNumber(), configs);
                }
            }
        }

        // Related Parties
        if (newClient.relatedParties() != null) {
            for (RelatedParty newRP : newClient.relatedParties()) {
                RelatedParty oldRP = findRelatedPartyByNames(oldClient.relatedParties(), newRP.firstName(),
                        newRP.lastName());
                if (oldRP == null) {
                    logChange(clientID, newRP.relatedPartyID(), "RelatedParty", "ALL", "CREATE", null,
                            newRP.firstName() + " " + newRP.lastName(), configs);
                } else {
                    compareAndLog(clientID, newRP.relatedPartyID(), "RelatedParty", "relationType",
                            oldRP.relationType(), newRP.relationType(), configs);
                    compareAndLog(clientID, newRP.relatedPartyID(), "RelatedParty", "citizenship1",
                            oldRP.citizenship1(), newRP.citizenship1(), configs);
                }
            }
        }
    }

    private void compareAndLog(Long clientID, Long entityID, String entityName, String col, String oldVal,
            String newVal, Map<String, String> configs) {
        if (!Objects.equals(oldVal, newVal)) {
            logChange(clientID, entityID, entityName, col, "UPDATE", oldVal, newVal, configs);
        }
    }

    private void logChange(Long clientID, Long entityID, String entityName, String col, String op, String oldVal,
            String newVal, Map<String, String> configs) {
        String key = entityName + ":" + col;
        String category = configs.getOrDefault(key, "NONE");

        if (!"NONE".equalsIgnoreCase(category)) {
            MaterialChange mc = new MaterialChange(
                    null, null, clientID, null, entityID, entityName, col, op, oldVal, newVal, "PENDING", category);
            materialChangeRepository.save(mc);
        }
    }

    private Address findAddressByType(java.util.List<Address> list, String type) {
        if (list == null)
            return null;
        return list.stream().filter(a -> Objects.equals(a.addressType(), type)).findFirst().orElse(null);
    }

    private Identifier findIdentifierByType(java.util.List<Identifier> list, String type) {
        if (list == null)
            return null;
        return list.stream().filter(i -> Objects.equals(i.identifierType(), type)).findFirst().orElse(null);
    }

    private RelatedParty findRelatedPartyByNames(java.util.List<RelatedParty> list, String first, String last) {
        if (list == null)
            return null;
        return list.stream().filter(rp -> Objects.equals(rp.firstName(), first) && Objects.equals(rp.lastName(), last))
                .findFirst().orElse(null);
    }
}
