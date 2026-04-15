package com.venus.kyc.orchestration.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StringListConverterTest {

    private StringListConverter converter;

    @BeforeEach
    void setUp() {
        converter = new StringListConverter();
    }

    @Test
    void convertToDatabaseColumn_null_returnsNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToDatabaseColumn_emptyList_returnsNull() {
        assertNull(converter.convertToDatabaseColumn(Collections.emptyList()));
    }

    @Test
    void convertToDatabaseColumn_singleElement_returnsValue() {
        assertEquals("PEP", converter.convertToDatabaseColumn(List.of("PEP")));
    }

    @Test
    void convertToDatabaseColumn_multipleElements_returnsPipeDelimited() {
        assertEquals("PEP|ADM|SAN", converter.convertToDatabaseColumn(Arrays.asList("PEP", "ADM", "SAN")));
    }

    @Test
    void convertToEntityAttribute_null_returnsEmptyList() {
        assertEquals(Collections.emptyList(), converter.convertToEntityAttribute(null));
    }

    @Test
    void convertToEntityAttribute_blank_returnsEmptyList() {
        assertEquals(Collections.emptyList(), converter.convertToEntityAttribute("   "));
    }

    @Test
    void convertToEntityAttribute_single_returnsSingletonList() {
        List<String> result = converter.convertToEntityAttribute("PEP");
        assertEquals(1, result.size());
        assertEquals("PEP", result.get(0));
    }

    @Test
    void convertToEntityAttribute_pipeDelimited_returnsList() {
        List<String> result = converter.convertToEntityAttribute("PEP|ADM");
        assertEquals(Arrays.asList("PEP", "ADM"), result);
    }

    @Test
    void roundTrip_multipleElements_survivesConversion() {
        List<String> original = Arrays.asList("PEP", "ADM", "SAN");
        String encoded = converter.convertToDatabaseColumn(original);
        List<String> decoded = converter.convertToEntityAttribute(encoded);
        assertEquals(original, decoded);
    }
}
