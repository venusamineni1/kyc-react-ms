package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Record {
    @XmlElement(name = "Meta", namespace = "http://www.db.com/NLSRequest")
    private RecordMeta meta;

    @XmlElement(name = "Data", namespace = "http://www.db.com/NLSRequest")
    private RecordData data;

    public RecordMeta getMeta() {
        return meta;
    }

    public void setMeta(RecordMeta meta) {
        this.meta = meta;
    }

    public RecordData getData() {
        return data;
    }

    public void setData(RecordData data) {
        this.data = data;
    }
}
