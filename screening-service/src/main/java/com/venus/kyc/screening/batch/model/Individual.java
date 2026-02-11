package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@jakarta.xml.bind.annotation.XmlType(propOrder = { "names", "gender", "dob", "cntr", "placeOfBirth", "occupation",
        "nationalities", "addresses" })
public class Individual {
    @XmlElement(name = "Names", namespace = "http://www.db.com/NLSPartyInformation")
    private Names names;

    @XmlElement(name = "G", namespace = "http://www.db.com/NLSPartyInformation")
    private String gender;

    @XmlElement(name = "DOB", namespace = "http://www.db.com/NLSPartyInformation")
    private String dob;

    @XmlElement(name = "Plc", namespace = "http://www.db.com/NLSPartyInformation")
    private String placeOfBirth;

    @XmlElement(name = "Cntr", namespace = "http://www.db.com/NLSPartyInformation")
    private String cntr;

    @XmlElement(name = "Occ", namespace = "http://www.db.com/NLSPartyInformation")
    private String occupation;

    @XmlElement(name = "Nats", namespace = "http://www.db.com/NLSPartyInformation")
    private Nationalities nationalities;

    @XmlElement(name = "Addrs", namespace = "http://www.db.com/NLSPartyInformation")
    private Addresses addresses;

    // Getters and Setters
    public Names getNames() {
        return names;
    }

    public void setNames(Names names) {
        this.names = names;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getPlaceOfBirth() {
        return placeOfBirth;
    }

    public void setPlaceOfBirth(String placeOfBirth) {
        this.placeOfBirth = placeOfBirth;
    }

    public String getCntr() {
        return cntr;
    }

    public void setCntr(String cntr) {
        this.cntr = cntr;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public Nationalities getNationalities() {
        return nationalities;
    }

    public void setNationalities(Nationalities nationalities) {
        this.nationalities = nationalities;
    }

    public Addresses getAddresses() {
        return addresses;
    }

    public void setAddresses(Addresses addresses) {
        this.addresses = addresses;
    }
}
