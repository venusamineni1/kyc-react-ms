package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
@jakarta.xml.bind.annotation.XmlType(propOrder = { "type", "line", "city", "zipCode", "prov", "cntr" })
public class Address {
    @XmlElement(name = "Type", namespace = "http://www.db.com/NLSPartyInformation")
    private String type;

    @XmlElement(name = "Line", namespace = "http://www.db.com/NLSPartyInformation")
    private String line;

    @XmlElement(name = "City", namespace = "http://www.db.com/NLSPartyInformation")
    private String city;

    @XmlElement(name = "ZipCode", namespace = "http://www.db.com/NLSPartyInformation")
    private String zipCode;

    @XmlElement(name = "Prov", namespace = "http://www.db.com/NLSPartyInformation")
    private String prov;

    @XmlElement(name = "Cntr", namespace = "http://www.db.com/NLSPartyInformation")
    private String cntr;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getProv() {
        return prov;
    }

    public void setProv(String prov) {
        this.prov = prov;
    }

    public String getCntr() {
        return cntr;
    }

    public void setCntr(String cntr) {
        this.cntr = cntr;
    }
}
