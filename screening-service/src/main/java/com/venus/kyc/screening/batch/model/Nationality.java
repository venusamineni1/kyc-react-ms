package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
@jakarta.xml.bind.annotation.XmlType(propOrder = { "cntr", "legDoc", "idNr", "ca" })
public class Nationality {
    @XmlElement(name = "Cntr", namespace = "http://www.db.com/NLSPartyInformation")
    private String cntr;

    @XmlElement(name = "LegDoc", namespace = "http://www.db.com/NLSPartyInformation")
    private String legDoc;

    @XmlElement(name = "IdNr", namespace = "http://www.db.com/NLSPartyInformation")
    private String idNr;

    @XmlElement(name = "CA", namespace = "http://www.db.com/NLSPartyInformation")
    private String ca;

    public String getCntr() {
        return cntr;
    }

    public void setCntr(String cntr) {
        this.cntr = cntr;
    }

    public String getLegDoc() {
        return legDoc;
    }

    public void setLegDoc(String legDoc) {
        this.legDoc = legDoc;
    }

    public String getIdNr() {
        return idNr;
    }

    public void setIdNr(String idNr) {
        this.idNr = idNr;
    }

    public String getCa() {
        return ca;
    }

    public void setCa(String ca) {
        this.ca = ca;
    }
}
