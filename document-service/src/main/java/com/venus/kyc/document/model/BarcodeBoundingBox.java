package com.venus.kyc.document.model;

/**
 * Pixel location of a decoded barcode within the original (unscaled) document image —
 * lets the frontend draw an auto-detected region overlay without re-running detection.
 */
public record BarcodeBoundingBox(int x, int y, int width, int height, int imageWidth, int imageHeight) {}
