package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class FInfo {
    @XmlElement(name = "Name", namespace = "http://www.db.com/NLSRequest")
    private String name;

    @XmlElement(name = "BatchNr", namespace = "http://www.db.com/NLSRequest")
    private String batchNr;

    @XmlElement(name = "FBatch", namespace = "http://www.db.com/NLSRequest")
    private String fBatch;

    @XmlElement(name = "Cntr", namespace = "http://www.db.com/NLSRequest")
    private String cntr;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBatchNr() {
        return batchNr;
    }

    public void setBatchNr(String batchNr) {
        this.batchNr = batchNr;
    }

    public String getfBatch() {
        return fBatch;
    }

    public void setfBatch(String fBatch) {
        this.fBatch = fBatch;
    }

    public String getCntr() {
        return cntr;
    }

    public void setCntr(String cntr) {
        this.cntr = cntr;
    }
}
