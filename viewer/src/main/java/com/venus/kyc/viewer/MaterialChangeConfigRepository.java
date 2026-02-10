package com.venus.kyc.viewer;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class MaterialChangeConfigRepository {

    private final JdbcClient jdbcClient;

    public MaterialChangeConfigRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<MaterialChangeConfig> findAll() {
        return jdbcClient.sql("SELECT * FROM MaterialChangeConfigs")
                .query((rs, rowNum) -> new MaterialChangeConfig(
                        rs.getLong("ConfigID"),
                        rs.getString("EntityName"),
                        rs.getString("ColumnName"),
                        rs.getString("Category")))
                .list();
    }

    public Map<String, String> getAllConfigsAsMap() {
        return findAll().stream()
                .collect(Collectors.toMap(
                        c -> c.entityName() + ":" + c.columnName(),
                        MaterialChangeConfig::category));
    }

    public void save(MaterialChangeConfig config) {
        jdbcClient.sql(
                "MERGE INTO MaterialChangeConfigs (EntityName, ColumnName, Category) KEY(EntityName, ColumnName) VALUES (:entityName, :columnName, :category)")
                .param("entityName", config.entityName())
                .param("columnName", config.columnName())
                .param("category", config.category())
                .update();
    }
}
