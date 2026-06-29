package com.venus.kyc.screening.batch;

import com.venus.kyc.screening.batch.model.Client;
import com.venus.kyc.screening.crypto.PiiCryptoService;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for the BatchScreeningStaging table — used only in persist-first mode (Mode B).
 * Stores all client records from the CSV before orchestration begins, enabling
 * crash-safe resumable dispatch.
 *
 * The genuinely sensitive PII columns (name, DOB, ID number, address, place of birth) are
 * encrypted at rest via PiiCryptoService — operational columns (Status, ProcessingStatus,
 * RunGroupId, etc.) stay plaintext since they're not PII and need to stay queryable.
 */
@Repository
public class BatchScreeningStagingRepository {

    private final JdbcClient jdbcClient;
    private final PiiCryptoService crypto;

    public BatchScreeningStagingRepository(JdbcClient jdbcClient, PiiCryptoService crypto) {
        this.jdbcClient = jdbcClient;
        this.crypto = crypto;
    }

    /** Bulk-insert a chunk of Client records with the given runGroupId. */
    public void insertChunk(String runGroupId, List<Client> clients) {
        String sql = """
                INSERT INTO BatchScreeningStaging
                  (RunGroupId, ClientId, TitlePrefix, FirstName, MiddleName, LastName, TitleSuffix,
                   Citizenship1, Citizenship2, OnboardingDate, Status, NameAtBirth, NickName, Gender,
                   DateOfBirth, Language, Occupation, CountryOfTax, SourceOfFundsCountry, FatcaStatus,
                   CrsStatus, AddressLine1, City, ZipCode, Province, Country, Nationality,
                   LegDocType, IdNumber, PlaceOfBirth, CityOfBirth, CountryOfBirth, ProcessingStatus)
                VALUES
                  (:runGroupId, :clientId, :titlePrefix, :firstName, :middleName, :lastName, :titleSuffix,
                   :citizenship1, :citizenship2, :onboardingDate, :status, :nameAtBirth, :nickName, :gender,
                   :dateOfBirth, :language, :occupation, :countryOfTax, :sourceOfFundsCountry, :fatcaStatus,
                   :crsStatus, :addressLine1, :city, :zipCode, :province, :country, :nationality,
                   :legDocType, :idNumber, :placeOfBirth, :cityOfBirth, :countryOfBirth, 'PENDING')
                """;
        for (Client c : clients) {
            jdbcClient.sql(sql)
                    .param("runGroupId", runGroupId)
                    .param("clientId", c.clientID())
                    .param("titlePrefix", c.titlePrefix())
                    .param("firstName", crypto.encrypt(c.firstName()))
                    .param("middleName", crypto.encrypt(c.middleName()))
                    .param("lastName", crypto.encrypt(c.lastName()))
                    .param("titleSuffix", c.titleSuffix())
                    .param("citizenship1", c.citizenship1())
                    .param("citizenship2", c.citizenship2())
                    .param("onboardingDate", c.onboardingDate())
                    .param("status", c.status())
                    .param("nameAtBirth", crypto.encrypt(c.nameAtBirth()))
                    .param("nickName", crypto.encrypt(c.nickName()))
                    .param("gender", c.gender())
                    .param("dateOfBirth", crypto.encrypt(c.dateOfBirth() != null ? c.dateOfBirth().toString() : null))
                    .param("language", c.language())
                    .param("occupation", c.occupation())
                    .param("countryOfTax", c.countryOfTax())
                    .param("sourceOfFundsCountry", c.sourceOfFundsCountry())
                    .param("fatcaStatus", c.fatcaStatus())
                    .param("crsStatus", c.crsStatus())
                    .param("addressLine1", crypto.encrypt(c.addressLine1()))
                    .param("city", c.city())
                    .param("zipCode", c.zipCode())
                    .param("province", c.province())
                    .param("country", c.country())
                    .param("nationality", c.nationality())
                    .param("legDocType", c.legDocType())
                    .param("idNumber", crypto.encrypt(c.idNumber()))
                    .param("placeOfBirth", crypto.encrypt(c.placeOfBirth()))
                    .param("cityOfBirth", crypto.encrypt(c.cityOfBirth()))
                    .param("countryOfBirth", c.countryOfBirth())
                    .update();
        }
    }

    /**
     * Fetch next page of PENDING staging rows (cursor-based, by Id) and map them to Client objects.
     * Returns empty list when no more rows remain.
     */
    public List<Client> fetchPendingPage(String runGroupId, long lastSeenId, int pageSize) {
        String sql = """
                SELECT * FROM BatchScreeningStaging
                WHERE RunGroupId = :runGroupId AND ProcessingStatus = 'PENDING' AND Id > :lastSeenId
                ORDER BY Id ASC
                LIMIT :pageSize
                """;
        return jdbcClient.sql(sql)
                .param("runGroupId", runGroupId)
                .param("lastSeenId", lastSeenId)
                .param("pageSize", pageSize)
                .query((rs, rowNum) -> {
                    String decryptedDob = crypto.decrypt(rs.getString("DateOfBirth"));
                    return new Client(
                        rs.getLong("ClientId"),
                        rs.getString("TitlePrefix"),
                        crypto.decrypt(rs.getString("FirstName")),
                        crypto.decrypt(rs.getString("MiddleName")),
                        crypto.decrypt(rs.getString("LastName")),
                        rs.getString("TitleSuffix"),
                        rs.getString("Citizenship1"),
                        rs.getString("Citizenship2"),
                        rs.getDate("OnboardingDate") != null ? rs.getDate("OnboardingDate").toLocalDate() : null,
                        rs.getString("Status"),
                        crypto.decrypt(rs.getString("NameAtBirth")),
                        crypto.decrypt(rs.getString("NickName")),
                        rs.getString("Gender"),
                        decryptedDob != null ? java.time.LocalDate.parse(decryptedDob) : null,
                        rs.getString("Language"),
                        rs.getString("Occupation"),
                        rs.getString("CountryOfTax"),
                        rs.getString("SourceOfFundsCountry"),
                        rs.getString("FatcaStatus"),
                        rs.getString("CrsStatus"),
                        crypto.decrypt(rs.getString("AddressLine1")),
                        rs.getString("City"),
                        rs.getString("ZipCode"),
                        rs.getString("Province"),
                        rs.getString("Country"),
                        rs.getString("Nationality"),
                        rs.getString("LegDocType"),
                        crypto.decrypt(rs.getString("IdNumber")),
                        crypto.decrypt(rs.getString("PlaceOfBirth")),
                        crypto.decrypt(rs.getString("CityOfBirth")),
                        rs.getString("CountryOfBirth")
                    );
                })
                .list();
    }

    /** Mark a list of staging row IDs as DISPATCHED. */
    public void markDispatched(List<Long> stagingIds) {
        if (stagingIds.isEmpty()) return;
        String placeholders = String.join(",", stagingIds.stream().map(id -> "?").toList());
        String sql = "UPDATE BatchScreeningStaging SET ProcessingStatus = 'DISPATCHED' WHERE Id IN (" + placeholders + ")";
        var stmt = jdbcClient.sql(sql);
        for (Long id : stagingIds) stmt = stmt.param(id);
        stmt.update();
    }

    public long countPending(String runGroupId) {
        return jdbcClient.sql("SELECT COUNT(*) FROM BatchScreeningStaging WHERE RunGroupId = :runGroupId AND ProcessingStatus = 'PENDING'")
                .param("runGroupId", runGroupId)
                .query(Long.class).single();
    }

    public long maxId(String runGroupId) {
        Long max = jdbcClient.sql("SELECT MAX(Id) FROM BatchScreeningStaging WHERE RunGroupId = :runGroupId AND ProcessingStatus = 'PENDING'")
                .param("runGroupId", runGroupId)
                .query(Long.class).optional().orElse(null);
        return max != null ? max : 0L;
    }

    /** Fetch staging row IDs for a given result page (used for markDispatched). */
    public List<Long> fetchPendingIds(String runGroupId, long lastSeenId, int pageSize) {
        return jdbcClient.sql("""
                SELECT Id FROM BatchScreeningStaging
                WHERE RunGroupId = :runGroupId AND ProcessingStatus = 'PENDING' AND Id > :lastSeenId
                ORDER BY Id ASC LIMIT :pageSize
                """)
                .param("runGroupId", runGroupId)
                .param("lastSeenId", lastSeenId)
                .param("pageSize", pageSize)
                .query(Long.class).list();
    }

    /** Delete all staging rows for a completed run (cleanup). */
    public void deleteByRunGroupId(String runGroupId) {
        jdbcClient.sql("DELETE FROM BatchScreeningStaging WHERE RunGroupId = :runGroupId")
                .param("runGroupId", runGroupId)
                .update();
    }
}
