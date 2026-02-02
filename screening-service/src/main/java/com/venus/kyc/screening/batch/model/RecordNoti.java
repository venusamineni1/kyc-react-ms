package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class RecordNoti {
    @XmlElement(name = "Rec", namespace = "http://www.db.com/NLSNotification")
    private List<NotiRec> recList;

    public List<NotiRec> getRecList() {
        return recList;
    }

    public void setRecList(List<NotiRec> recList) {
        this.recList = recList;
    }
}
