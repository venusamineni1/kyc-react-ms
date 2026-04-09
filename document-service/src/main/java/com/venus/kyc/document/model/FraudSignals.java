package com.venus.kyc.document.model;

public class FraudSignals {
    private Signal mrzCheckDigits;
    private Signal pdfSignature;
    private Signal exifMetadata;
    private Signal photoZoneEla;
    private Signal faceDetected;
    private Signal barcodeOcrMatch;

    public FraudSignals() {}

    public Signal getMrzCheckDigits() { return mrzCheckDigits; }
    public void setMrzCheckDigits(Signal s) { this.mrzCheckDigits = s; }

    public Signal getPdfSignature() { return pdfSignature; }
    public void setPdfSignature(Signal s) { this.pdfSignature = s; }

    public Signal getExifMetadata() { return exifMetadata; }
    public void setExifMetadata(Signal s) { this.exifMetadata = s; }

    public Signal getPhotoZoneEla() { return photoZoneEla; }
    public void setPhotoZoneEla(Signal s) { this.photoZoneEla = s; }

    public Signal getFaceDetected() { return faceDetected; }
    public void setFaceDetected(Signal s) { this.faceDetected = s; }

    public Signal getBarcodeOcrMatch() { return barcodeOcrMatch; }
    public void setBarcodeOcrMatch(Signal s) { this.barcodeOcrMatch = s; }
}
