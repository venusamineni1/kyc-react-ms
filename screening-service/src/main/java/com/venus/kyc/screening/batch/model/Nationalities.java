package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Nationalities {
    @XmlElement(name = "Nat", namespace = "http://www.db.com/NLSPartyInformation")
    private java.util.List<Nationality> natList;

    public java.util.List<Nationality> getNatList() {
        return natList;
    }

    public void setNatList(java.util.List<Nationality> natList) {
        this.natList = natList;
    }
}
