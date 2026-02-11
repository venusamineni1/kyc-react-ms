package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class JuridicalInfo {
    @XmlElement(name = "BU", namespace = "http://www.db.com/NLSRequest")
    private List<BUInfo> bu;

    public List<BUInfo> getBu() {
        return bu;
    }

    public void setBu(List<BUInfo> bu) {
        this.bu = bu;
    }
}
