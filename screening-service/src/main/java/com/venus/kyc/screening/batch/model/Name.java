package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
@jakarta.xml.bind.annotation.XmlType(propOrder = { "type", "tit", "fir", "mid", "sur", "ma", "full" })
public class Name {
    @XmlElement(name = "Type", namespace = "http://www.db.com/NLSPartyInformation")
    private String type;

    @XmlElement(name = "Full", namespace = "http://www.db.com/NLSPartyInformation")
    private String full;

    @XmlElement(name = "Tit", namespace = "http://www.db.com/NLSPartyInformation")
    private String tit;

    @XmlElement(name = "Fir", namespace = "http://www.db.com/NLSPartyInformation")
    private String fir;

    @XmlElement(name = "Mid", namespace = "http://www.db.com/NLSPartyInformation")
    private String mid;

    @XmlElement(name = "Sur", namespace = "http://www.db.com/NLSPartyInformation")
    private String sur;

    @XmlElement(name = "Ma", namespace = "http://www.db.com/NLSPartyInformation")
    private String ma;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFull() {
        return full;
    }

    public void setFull(String full) {
        this.full = full;
    }

    public String getTit() {
        return tit;
    }

    public void setTit(String tit) {
        this.tit = tit;
    }

    public String getFir() {
        return fir;
    }

    public void setFir(String fir) {
        this.fir = fir;
    }

    public String getMid() {
        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    public String getSur() {
        return sur;
    }

    public void setSur(String sur) {
        this.sur = sur;
    }

    public String getMa() {
        return ma;
    }

    public void setMa(String ma) {
        this.ma = ma;
    }
}
