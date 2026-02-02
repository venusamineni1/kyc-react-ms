package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Records {
    @XmlElement(name = "Rec", namespace = "http://www.db.com/NLSRequest")
    private List<Record> recList;

    public List<Record> getRecList() {
        return recList;
    }

    public void setRecList(List<Record> recList) {
        this.recList = recList;
    }
}
