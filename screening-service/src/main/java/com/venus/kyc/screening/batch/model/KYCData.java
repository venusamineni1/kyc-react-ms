package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class KYCData {
    @XmlElement(name = "PEPFlag", namespace = "http://www.db.com/NLSRequest")
    private String pepFlag;

    @XmlElement(name = "NextRvw", namespace = "http://www.db.com/NLSRequest")
    private NextReview nextRvw;

    public String getPepFlag() {
        return pepFlag;
    }

    public void setPepFlag(String pepFlag) {
        this.pepFlag = pepFlag;
    }

    public NextReview getNextRvw() {
        return nextRvw;
    }

    public void setNextRvw(NextReview nextRvw) {
        this.nextRvw = nextRvw;
    }
}
