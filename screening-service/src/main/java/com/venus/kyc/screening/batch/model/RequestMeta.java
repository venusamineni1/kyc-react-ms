package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class RequestMeta {
    @XmlElement(name = "SrcId", namespace = "http://www.db.com/NLSRequest")
    private String srcId;

    @XmlElement(name = "ToR", namespace = "http://www.db.com/NLSRequest")
    private String tor;

    @XmlElement(name = "CrtTm", namespace = "http://www.db.com/NLSRequest")
    private String crtTm;

    @XmlElement(name = "AoD", namespace = "http://www.db.com/NLSRequest")
    private String aod;

    @XmlElement(name = "NoR", namespace = "http://www.db.com/NLSRequest")
    private int nor;

    @XmlElement(name = "FInfo", namespace = "http://www.db.com/NLSRequest")
    private FInfo fInfo;

    // Getters and Setters
    public String getSrcId() {
        return srcId;
    }

    public void setSrcId(String srcId) {
        this.srcId = srcId;
    }

    public String getTor() {
        return tor;
    }

    public void setTor(String tor) {
        this.tor = tor;
    }

    public String getCrtTm() {
        return crtTm;
    }

    public void setCrtTm(String crtTm) {
        this.crtTm = crtTm;
    }

    public String getAod() {
        return aod;
    }

    public void setAod(String aod) {
        this.aod = aod;
    }

    public int getNor() {
        return nor;
    }

    public void setNor(int nor) {
        this.nor = nor;
    }

    public FInfo getfInfo() {
        return fInfo;
    }

    public void setfInfo(FInfo fInfo) {
        this.fInfo = fInfo;
    }
}
