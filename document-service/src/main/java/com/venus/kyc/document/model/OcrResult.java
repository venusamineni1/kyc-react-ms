package com.venus.kyc.document.model;

public class OcrResult {
    private OcrField surname;
    private OcrField givenNames;
    private OcrField nationality;
    private OcrField dateOfBirth;
    private OcrField expiryDate;
    private OcrField documentNumber;
    private OcrField documentType;
    private OcrField issuingCountry;
    private OcrField personalNumber;
    private String mrzCheckDigits;   // "PASS", "FAIL", or null
    private String rawMrz;
    private String rawText;          // full extracted text (PDF or OCR)
    private String barcodeData;      // decoded PDF417/QR if present
    private String source;           // "PDF_TEXT", "OCR", "MRZ", "BARCODE", "MOCK"

    public OcrResult() {}

    public OcrField getSurname() { return surname; }
    public void setSurname(OcrField surname) { this.surname = surname; }

    public OcrField getGivenNames() { return givenNames; }
    public void setGivenNames(OcrField givenNames) { this.givenNames = givenNames; }

    public OcrField getNationality() { return nationality; }
    public void setNationality(OcrField nationality) { this.nationality = nationality; }

    public OcrField getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(OcrField dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public OcrField getExpiryDate() { return expiryDate; }
    public void setExpiryDate(OcrField expiryDate) { this.expiryDate = expiryDate; }

    public OcrField getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(OcrField documentNumber) { this.documentNumber = documentNumber; }

    public OcrField getDocumentType() { return documentType; }
    public void setDocumentType(OcrField documentType) { this.documentType = documentType; }

    public OcrField getIssuingCountry() { return issuingCountry; }
    public void setIssuingCountry(OcrField issuingCountry) { this.issuingCountry = issuingCountry; }

    public OcrField getPersonalNumber() { return personalNumber; }
    public void setPersonalNumber(OcrField personalNumber) { this.personalNumber = personalNumber; }

    public String getMrzCheckDigits() { return mrzCheckDigits; }
    public void setMrzCheckDigits(String mrzCheckDigits) { this.mrzCheckDigits = mrzCheckDigits; }

    public String getRawMrz() { return rawMrz; }
    public void setRawMrz(String rawMrz) { this.rawMrz = rawMrz; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public String getBarcodeData() { return barcodeData; }
    public void setBarcodeData(String barcodeData) { this.barcodeData = barcodeData; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
