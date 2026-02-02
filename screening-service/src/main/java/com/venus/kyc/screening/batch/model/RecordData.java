package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class RecordData {
    @XmlElement(name = "PrtInfo", namespace = "http://www.db.com/NLSRequest")
    private PartyInfo prtInfo;

    public PartyInfo getPrtInfo() {
        return prtInfo;
    }

    public void setPrtInfo(PartyInfo prtInfo) {
        this.prtInfo = prtInfo;
    }
}
