package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Name {
    @XmlElement(name = "Type", namespace = "http://www.db.com/NLSPartyInformation")
    private String type;

    @XmlElement(name = "Full", namespace = "http://www.db.com/NLSPartyInformation")
    private String full;

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
}
