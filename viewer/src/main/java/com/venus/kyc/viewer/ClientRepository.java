package com.venus.kyc.viewer;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ClientRepository {

        private final JdbcClient jdbcClient;

        public ClientRepository(JdbcClient jdbcClient) {
                this.jdbcClient = jdbcClient;
        }

        public List<Client> findAll() {
                return findAllPaginated(0, Integer.MAX_VALUE).content();
        }

        public long countClients() {
                return jdbcClient.sql("SELECT COUNT(*) FROM Clients")
                                .query(Long.class)
                                .single();
        }

        public PaginatedResponse<Client> findAllPaginated(int page, int size) {
                long totalElements = countClients();
                int totalPages = (int) Math.ceil((double) totalElements / size);

                List<Client> clients = jdbcClient.sql(
                                "SELECT ClientID, TitlePrefix, FirstName, MiddleName, LastName, TitleSuffix, Citizenship1, Citizenship2, OnboardingDate, Status, NameAtBirth, NickName, Gender, DateOfBirth, Language, Occupation, CountryOfTax, SourceOfFundsCountry, FATCAStatus, CRSStatus FROM Clients LIMIT :limit OFFSET :offset")
                                .param("limit", size)
                                .param("offset", page * size)
                                .query((rs, rowNum) -> new Client(
                                                rs.getLong("ClientID"),
                                                rs.getString("TitlePrefix"),
                                                rs.getString("FirstName"),
                                                rs.getString("MiddleName"),
                                                rs.getString("LastName"),
                                                rs.getString("TitleSuffix"),
                                                rs.getString("Citizenship1"),
                                                rs.getString("Citizenship2"),
                                                rs.getDate("OnboardingDate").toLocalDate(),
                                                rs.getString("Status"),
                                                rs.getString("NameAtBirth"),
                                                rs.getString("NickName"),
                                                rs.getString("Gender"),
                                                rs.getDate("DateOfBirth") != null
                                                                ? rs.getDate("DateOfBirth").toLocalDate()
                                                                : null,
                                                rs.getString("Language"),
                                                rs.getString("Occupation"),
                                                rs.getString("CountryOfTax"),
                                                rs.getString("SourceOfFundsCountry"),
                                                rs.getString("FATCAStatus"),
                                                rs.getString("CRSStatus"),
                                                new java.util.ArrayList<>(),
                                                new java.util.ArrayList<>(),
                                                new java.util.ArrayList<>(),
                                                new java.util.ArrayList<>(),
                                                new java.util.ArrayList<>()))
                                .list();

                for (Client client : clients) {
                        client.addresses().addAll(fetchAddresses(client.clientID()));
                        client.identifiers().addAll(fetchIdentifiers(client.clientID()));
                        client.relatedParties().addAll(fetchRelatedParties(client.clientID()));
                        client.accounts().addAll(fetchAccounts(client.clientID()));
                        client.portfolios().addAll(fetchPortfolios(client.clientID()));
                }

                return new PaginatedResponse<>(clients, page, size, totalElements, totalPages);
        }

        public Optional<Client> findById(Long id) {
                Optional<Client> clientOpt = jdbcClient.sql(
                                "SELECT ClientID, TitlePrefix, FirstName, MiddleName, LastName, TitleSuffix, Citizenship1, Citizenship2, OnboardingDate, Status, NameAtBirth, NickName, Gender, DateOfBirth, Language, Occupation, CountryOfTax, SourceOfFundsCountry, FATCAStatus, CRSStatus FROM Clients WHERE ClientID = :id")
                                .param("id", id)
                                .query((rs, rowNum) -> new Client(
                                                rs.getLong("ClientID"),
                                                rs.getString("TitlePrefix"),
                                                rs.getString("FirstName"),
                                                rs.getString("MiddleName"),
                                                rs.getString("LastName"),
                                                rs.getString("TitleSuffix"),
                                                rs.getString("Citizenship1"),
                                                rs.getString("Citizenship2"),
                                                rs.getDate("OnboardingDate").toLocalDate(),
                                                rs.getString("Status"),
                                                rs.getString("NameAtBirth"),
                                                rs.getString("NickName"),
                                                rs.getString("Gender"),
                                                rs.getDate("DateOfBirth") != null
                                                                ? rs.getDate("DateOfBirth").toLocalDate()
                                                                : null,
                                                rs.getString("Language"),
                                                rs.getString("Occupation"),
                                                rs.getString("CountryOfTax"),
                                                rs.getString("SourceOfFundsCountry"),
                                                rs.getString("FATCAStatus"),
                                                rs.getString("CRSStatus"),
                                                new java.util.ArrayList<>(),
                                                new java.util.ArrayList<>(),
                                                new java.util.ArrayList<>(),
                                                new java.util.ArrayList<>(),
                                                new java.util.ArrayList<>()))
                                .optional();

                if (clientOpt.isPresent()) {
                        Client client = clientOpt.get();
                        client.addresses().addAll(fetchAddresses(id));
                        client.identifiers().addAll(fetchIdentifiers(id));
                        client.relatedParties().addAll(fetchRelatedParties(id));
                        client.accounts().addAll(fetchAccounts(id));
                        client.portfolios().addAll(fetchPortfolios(id));
                }

                return clientOpt;
        }

        private List<Address> fetchAddresses(Long id) {
                return jdbcClient.sql(
                                "SELECT AddressID, AddressType, AddressLine1, AddressLine2, City, Zip, Country, AddressNumber, AddressSupplement FROM ClientAddresses WHERE ClientID = :id")
                                .param("id", id)
                                .query(Address.class)
                                .list();
        }

        private List<Account> fetchAccounts(Long id) {
                return jdbcClient.sql(
                                "SELECT AccountID, AccountNumber, AccountStatus FROM Accounts WHERE ClientID = :id")
                                .param("id", id)
                                .query(Account.class)
                                .list();
        }

        private List<Portfolio> fetchPortfolios(Long id) {
                return jdbcClient.sql(
                                "SELECT PortfolioID, ClientID, AccountNumber, PortfolioText, OnboardingDate, OffboardingDate, Status FROM Portfolios WHERE ClientID = :id")
                                .param("id", id)
                                .query(Portfolio.class)
                                .list();
        }

        private List<Identifier> fetchIdentifiers(Long id) {
                return jdbcClient.sql(
                                "SELECT IdentifierID, IdentifierType, IdentifierValue, IssuingAuthority, IdentifierNumber FROM ClientIdentifiers WHERE ClientID = :id")
                                .param("id", id)
                                .query(Identifier.class)
                                .list();
        }

        private List<RelatedParty> fetchRelatedParties(Long clientID) {
                List<RelatedParty> parties = jdbcClient.sql(
                                "SELECT RelatedPartyID, ClientID, RelationType, TitlePrefix, FirstName, MiddleName, LastName, TitleSuffix, Citizenship1, Citizenship2, OnboardingDate, Status, NameAtBirth, NickName, Gender, DateOfBirth, Language, Occupation, CountryOfTax, SourceOfFundsCountry, FATCAStatus, CRSStatus FROM RelatedParties WHERE ClientID = :id")
                                .param("id", clientID)
                                .query((rs, rowNum) -> new RelatedParty(
                                                rs.getLong("RelatedPartyID"),
                                                rs.getLong("ClientID"),
                                                rs.getString("RelationType"),
                                                rs.getString("TitlePrefix"),
                                                rs.getString("FirstName"),
                                                rs.getString("MiddleName"),
                                                rs.getString("LastName"),
                                                rs.getString("TitleSuffix"),
                                                rs.getString("Citizenship1"),
                                                rs.getString("Citizenship2"),
                                                rs.getDate("OnboardingDate") != null
                                                                ? rs.getDate("OnboardingDate").toLocalDate()
                                                                : null,
                                                rs.getString("Status"),
                                                rs.getString("NameAtBirth"),
                                                rs.getString("NickName"),
                                                rs.getString("Gender"),
                                                rs.getDate("DateOfBirth") != null
                                                                ? rs.getDate("DateOfBirth").toLocalDate()
                                                                : null,
                                                rs.getString("Language"),
                                                rs.getString("Occupation"),
                                                rs.getString("CountryOfTax"),
                                                rs.getString("SourceOfFundsCountry"),
                                                rs.getString("FATCAStatus"),
                                                rs.getString("CRSStatus"),
                                                new java.util.ArrayList<>(),
                                                new java.util.ArrayList<>()))
                                .list();

                for (RelatedParty party : parties) {
                        party.addresses().addAll(jdbcClient.sql(
                                        "SELECT AddressID, AddressType, AddressLine1, AddressLine2, City, Zip, Country, AddressNumber, AddressSupplement FROM RelatedPartyAddresses WHERE RelatedPartyID = :id")
                                        .param("id", party.relatedPartyID())
                                        .query(Address.class)
                                        .list());
                        party.identifiers().addAll(jdbcClient.sql(
                                        "SELECT IdentifierID, IdentifierType, IdentifierValue, IssuingAuthority, IdentifierNumber FROM RelatedPartyIdentifiers WHERE RelatedPartyID = :id")
                                        .param("id", party.relatedPartyID())
                                        .query(Identifier.class)
                                        .list());
                }
                return parties;
        }

        public void saveRelatedParty(Long clientID, RelatedParty rp) {
                jdbcClient.sql(
                                "INSERT INTO RelatedParties (ClientID, RelationType, TitlePrefix, FirstName, MiddleName, LastName, TitleSuffix, Citizenship1, Citizenship2, OnboardingDate, Status, NameAtBirth, NickName, Gender, DateOfBirth, Language, Occupation, CountryOfTax, SourceOfFundsCountry, FATCAStatus, CRSStatus) VALUES (:clientID, :relationType, :titlePrefix, :firstName, :middleName, :lastName, :titleSuffix, :citizenship1, :citizenship2, :onboardingDate, :status, :nameAtBirth, :nickName, :gender, :dateOfBirth, :language, :occupation, :countryOfTax, :sourceOfFundsCountry, :fatcaStatus, :crsStatus)")
                                .param("clientID", clientID)
                                .param("relationType", rp.relationType())
                                .param("titlePrefix", rp.titlePrefix())
                                .param("firstName", rp.firstName())
                                .param("middleName", rp.middleName())
                                .param("lastName", rp.lastName())
                                .param("titleSuffix", rp.titleSuffix())
                                .param("citizenship1", rp.citizenship1())
                                .param("citizenship2", rp.citizenship2())
                                .param("onboardingDate", rp.onboardingDate())
                                .param("status", rp.status())
                                .param("nameAtBirth", rp.nameAtBirth())
                                .param("nickName", rp.nickName())
                                .param("gender", rp.gender())
                                .param("dateOfBirth", rp.dateOfBirth())
                                .param("language", rp.language())
                                .param("occupation", rp.occupation())
                                .param("countryOfTax", rp.countryOfTax())
                                .param("sourceOfFundsCountry", rp.sourceOfFundsCountry())
                                .param("fatcaStatus", rp.fatcaStatus())
                                .param("crsStatus", rp.crsStatus())
                                .update();
        }

        public Optional<RelatedParty> findRelatedPartyById(Long id) {
                Optional<RelatedParty> partyOpt = jdbcClient.sql(
                                "SELECT RelatedPartyID, ClientID, RelationType, TitlePrefix, FirstName, MiddleName, LastName, TitleSuffix, Citizenship1, Citizenship2, OnboardingDate, Status, NameAtBirth, NickName, Gender, DateOfBirth, Language, Occupation, CountryOfTax, SourceOfFundsCountry, FATCAStatus, CRSStatus FROM RelatedParties WHERE RelatedPartyID = :id")
                                .param("id", id)
                                .query((rs, rowNum) -> new RelatedParty(
                                                rs.getLong("RelatedPartyID"),
                                                rs.getLong("ClientID"),
                                                rs.getString("RelationType"),
                                                rs.getString("TitlePrefix"),
                                                rs.getString("FirstName"),
                                                rs.getString("MiddleName"),
                                                rs.getString("LastName"),
                                                rs.getString("TitleSuffix"),
                                                rs.getString("Citizenship1"),
                                                rs.getString("Citizenship2"),
                                                rs.getDate("OnboardingDate") != null
                                                                ? rs.getDate("OnboardingDate").toLocalDate()
                                                                : null,
                                                rs.getString("Status"),
                                                rs.getString("NameAtBirth"),
                                                rs.getString("NickName"),
                                                rs.getString("Gender"),
                                                rs.getDate("DateOfBirth") != null
                                                                ? rs.getDate("DateOfBirth").toLocalDate()
                                                                : null,
                                                rs.getString("Language"),
                                                rs.getString("Occupation"),
                                                rs.getString("CountryOfTax"),
                                                rs.getString("SourceOfFundsCountry"),
                                                rs.getString("FATCAStatus"),
                                                rs.getString("CRSStatus"),
                                                new java.util.ArrayList<>(),
                                                new java.util.ArrayList<>()))
                                .optional();

                if (partyOpt.isPresent()) {
                        RelatedParty party = partyOpt.get();
                        party.addresses().addAll(jdbcClient.sql(
                                        "SELECT AddressID, AddressType, AddressLine1, AddressLine2, City, Zip, Country, AddressNumber, AddressSupplement FROM RelatedPartyAddresses WHERE RelatedPartyID = :id")
                                        .param("id", id)
                                        .query(Address.class)
                                        .list());
                        party.identifiers().addAll(jdbcClient.sql(
                                        "SELECT IdentifierID, IdentifierType, IdentifierValue, IssuingAuthority, IdentifierNumber FROM RelatedPartyIdentifiers WHERE RelatedPartyID = :id")
                                        .param("id", id)
                                        .query(Identifier.class)
                                        .list());
                }
                return partyOpt;
        }

        public List<Client> searchByName(String query) {
                return searchByNamePaginated(query, 0, Integer.MAX_VALUE).content();
        }

        public long countSearchClients(String query) {
                String likeQuery = "%" + query + "%";
                return jdbcClient.sql(
                                "SELECT COUNT(*) FROM Clients WHERE FirstName LIKE :query OR MiddleName LIKE :query OR LastName LIKE :query")
                                .param("query", likeQuery)
                                .query(Long.class)
                                .single();
        }

        public PaginatedResponse<Client> searchByNamePaginated(String query, int page, int size) {
                String likeQuery = "%" + query + "%";
                long totalElements = countSearchClients(query);
                int totalPages = (int) Math.ceil((double) totalElements / size);

                List<Client> clients = jdbcClient.sql(
                                "SELECT ClientID, TitlePrefix, FirstName, MiddleName, LastName, TitleSuffix, Citizenship1, Citizenship2, OnboardingDate, Status, NameAtBirth, NickName, Gender, DateOfBirth, Language, Occupation, CountryOfTax, SourceOfFundsCountry, FATCAStatus, CRSStatus FROM Clients WHERE FirstName LIKE :query OR MiddleName LIKE :query OR LastName LIKE :query LIMIT :limit OFFSET :offset")
                                .param("query", likeQuery)
                                .param("limit", size)
                                .param("offset", page * size)
                                .query((rs, rowNum) -> new Client(
                                                rs.getLong("ClientID"),
                                                rs.getString("TitlePrefix"),
                                                rs.getString("FirstName"),
                                                rs.getString("MiddleName"),
                                                rs.getString("LastName"),
                                                rs.getString("TitleSuffix"),
                                                rs.getString("Citizenship1"),
                                                rs.getString("Citizenship2"),
                                                rs.getDate("OnboardingDate").toLocalDate(),
                                                rs.getString("Status"),
                                                rs.getString("NameAtBirth"),
                                                rs.getString("NickName"),
                                                rs.getString("Gender"),
                                                rs.getDate("DateOfBirth") != null
                                                                ? rs.getDate("DateOfBirth").toLocalDate()
                                                                : null,
                                                rs.getString("Language"),
                                                rs.getString("Occupation"),
                                                rs.getString("CountryOfTax"),
                                                rs.getString("SourceOfFundsCountry"),
                                                rs.getString("FATCAStatus"),
                                                rs.getString("CRSStatus"),
                                                new java.util.ArrayList<>(),
                                                new java.util.ArrayList<>(),
                                                new java.util.ArrayList<>(),
                                                new java.util.ArrayList<>(),
                                                new java.util.ArrayList<>()))
                                .list();

                for (Client client : clients) {
                        client.addresses().addAll(fetchAddresses(client.clientID()));
                        client.identifiers().addAll(fetchIdentifiers(client.clientID()));
                        client.relatedParties().addAll(fetchRelatedParties(client.clientID()));
                        client.accounts().addAll(fetchAccounts(client.clientID()));
                        client.portfolios().addAll(fetchPortfolios(client.clientID()));
                }

                return new PaginatedResponse<>(clients, page, size, totalElements, totalPages);
        }
}
