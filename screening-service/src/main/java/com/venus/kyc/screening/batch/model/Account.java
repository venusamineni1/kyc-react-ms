package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Account {
    @XmlElement(name = "Type", namespace = "http://www.db.com/NLSRequest")
    private String type;

    @XmlElement(name = "Nr", namespace = "http://www.db.com/NLSRequest")
    private String nr;

    @XmlElement(name = "AI", namespace = "http://www.db.com/NLSRequest")
    private String ai;

    @XmlElement(name = "SoW", namespace = "http://www.db.com/NLSRequest")
    private String sow;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNr() {
        return nr;
    }

    public void setNr(String nr) {
        this.nr = nr;
    }

    public String getAi() {
        return ai;
    }

    public void setAi(String ai) {
        this.ai = ai;
    }

    public String getSow() {
        return sow;
    }

    public void setSow(String sow) {
        this.sow = sow;
    }
}
