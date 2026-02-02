package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Request {

    @XmlElement(name = "Meta", namespace = "http://www.db.com/NLSRequest")
    private RequestMeta meta;

    @XmlElement(name = "Recs", namespace = "http://www.db.com/NLSRequest")
    private Records records;

    public RequestMeta getMeta() {
        return meta;
    }

    public void setMeta(RequestMeta meta) {
        this.meta = meta;
    }

    public Records getRecords() {
        return records;
    }

    public void setRecords(Records records) {
        this.records = records;
    }
}
