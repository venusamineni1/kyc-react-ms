package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class FbRecs {
    @XmlElement(name = "FbRec", namespace = "http://www.db.com/NLSFeedback")
    private List<FbRec> fbRecList;

    public List<FbRec> getFbRecList() {
        return fbRecList;
    }

    public void setFbRecList(List<FbRec> fbRecList) {
        this.fbRecList = fbRecList;
    }
}
