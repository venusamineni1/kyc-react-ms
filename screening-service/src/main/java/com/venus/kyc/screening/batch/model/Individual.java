package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Individual {
    @XmlElement(name = "Names", namespace = "http://www.db.com/NLSPartyInformation")
    private Names names;

    @XmlElement(name = "G", namespace = "http://www.db.com/NLSPartyInformation")
    private String gender;

    @XmlElement(name = "DOB", namespace = "http://www.db.com/NLSPartyInformation")
    private String dob;

    @XmlElement(name = "Plc", namespace = "http://www.db.com/NLSPartyInformation")
    private String placeOfBirth;

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
}
