package com.venus.kyc.orchestration.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PiiMaskingUtilTest {

    @Test
    void mask_null_returnsNullPlaceholder() {
        assertEquals("[null]", PiiMaskingUtil.mask(null));
    }

    @Test
    void mask_singleChar_masksCorrectly() {
        assertEquals("A***", PiiMaskingUtil.mask("A"));
    }

    @Test
    void mask_twoChars_masksCorrectly() {
        assertEquals("J***", PiiMaskingUtil.mask("Jo"));
    }

    @Test
    void mask_normalName_masksFirstAndLast() {
        assertEquals("S***h", PiiMaskingUtil.mask("Smith"));
    }

    @Test
    void mask_longName_masksFirstAndLast() {
        assertEquals("A***r", PiiMaskingUtil.mask("Alexander"));
    }
}
