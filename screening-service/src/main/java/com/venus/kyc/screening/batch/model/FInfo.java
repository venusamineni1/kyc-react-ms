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

    @XmlElement(name = "BD", namespace = "http://www.db.com/NLSRequest")
    private String bd;

    @XmlElement(name = "LBJ", namespace = "http://www.db.com/NLSRequest")
    private String lbj;

    @XmlElement(name = "LAFCJ", namespace = "http://www.db.com/NLSRequest")
    private String lafcj;

    @XmlElement(name = "BSRL", namespace = "http://www.db.com/NLSRequest")
    private String bsrl;

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

    public String getBd() {
        return bd;
    }

    public void setBd(String bd) {
        this.bd = bd;
    }

    public String getLbj() {
        return lbj;
    }

    public void setLbj(String lbj) {
        this.lbj = lbj;
    }

    public String getLafcj() {
        return lafcj;
    }

    public void setLafcj(String lafcj) {
        this.lafcj = lafcj;
    }

    public String getBsrl() {
        return bsrl;
    }

    public void setBsrl(String bsrl) {
        this.bsrl = bsrl;
    }
}
