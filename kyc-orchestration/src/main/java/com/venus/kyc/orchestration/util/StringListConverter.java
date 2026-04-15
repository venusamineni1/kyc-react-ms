package com.venus.kyc.orchestration.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Stores a List<String> as a pipe-delimited TEXT column.
 * e.g. ["PEP", "ADM"] ↔ "PEP|ADM"
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final String DELIMITER = "|";
    private static final String DELIMITER_REGEX = "\\|";

    @Override
    public String convertToDatabaseColumn(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return String.join(DELIMITER, list);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return Collections.emptyList();
        return Arrays.asList(dbData.split(DELIMITER_REGEX));
    }
}
