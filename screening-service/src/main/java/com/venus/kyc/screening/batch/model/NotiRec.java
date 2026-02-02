package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class NotiRec {
    @XmlElement(name = "UniRcrdId", namespace = "http://www.db.com/NLSNotification")
    private String uniRcrdId;

    @XmlElement(name = "Err", namespace = "http://www.db.com/NLSNotification")
    private List<NotiErr> errors;

    public String getUniRcrdId() {
        return uniRcrdId;
    }

    public void setUniRcrdId(String uniRcrdId) {
        this.uniRcrdId = uniRcrdId;
    }

    public List<NotiErr> getErrors() {
        return errors;
    }

    public void setErrors(List<NotiErr> errors) {
        this.errors = errors;
    }
}
