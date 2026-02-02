package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class FeedbackMeta {
    @XmlElement(name = "SrcId", namespace = "http://www.db.com/NLSFeedback")
    private String srcId;

    @XmlElement(name = "Nor", namespace = "http://www.db.com/NLSFeedback")
    private int nor;

    public String getSrcId() {
        return srcId;
    }

    public void setSrcId(String srcId) {
        this.srcId = srcId;
    }

    public int getNor() {
        return nor;
    }

    public void setNor(int nor) {
        this.nor = nor;
    }
}
