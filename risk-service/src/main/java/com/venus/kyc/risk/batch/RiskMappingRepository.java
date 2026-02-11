package com.venus.kyc.risk.batch;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RiskMappingRepository {

    private final JdbcClient jdbcClient;

    public RiskMappingRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<RiskMapping> findAll() {
        return jdbcClient.sql("SELECT * FROM RiskMappings")
                .query(RiskMapping.class)
                .list();
    }

    public void saveAll(List<RiskMapping> configs) {
        // Simple delete and insert for simplicity in this phase
        jdbcClient.sql("DELETE FROM RiskMappings").update();
        for (RiskMapping config : configs) {
            jdbcClient.sql(
                    "INSERT INTO RiskMappings (TargetPath, SourceField, DefaultValue, Category) VALUES (:targetPath, :sourceField, :defaultValue, :category)")
                    .param("targetPath", config.targetPath())
                    .param("sourceField", config.sourceField())
                    .param("defaultValue", config.defaultValue())
                    .param("category", config.category())
                    .update();
        }
    }
}
