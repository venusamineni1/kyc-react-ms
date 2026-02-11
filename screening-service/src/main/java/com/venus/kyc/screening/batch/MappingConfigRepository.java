package com.venus.kyc.screening.batch;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MappingConfigRepository {

    private final JdbcClient jdbcClient;

    public MappingConfigRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<MappingConfig> findAll() {
        return jdbcClient.sql("SELECT * FROM MappingConfigs")
                .query(MappingConfig.class)
                .list();
    }

    public void saveAll(List<MappingConfig> configs) {
        // Simple implementation: delete and re-insert
        jdbcClient.sql("DELETE FROM MappingConfigs").update();
        for (MappingConfig config : configs) {
            jdbcClient.sql("INSERT INTO MappingConfigs (TargetPath, SourceField, DefaultValue, Transformation) VALUES (:targetPath, :sourceField, :defaultValue, :transformation)")
                    .param("targetPath", config.targetPath())
                    .param("sourceField", config.sourceField())
                    .param("defaultValue", config.defaultValue())
                    .param("transformation", config.transformation())
                    .update();
        }
    }
}
