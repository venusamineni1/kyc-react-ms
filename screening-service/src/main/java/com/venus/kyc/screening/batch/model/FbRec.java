package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class FbRec {
    @XmlElement(name = "UniRcrdId", namespace = "http://www.db.com/NLSFeedback")
    private String uniRcrdId;

    @XmlElement(name = "Mat", namespace = "http://www.db.com/NLSFeedback")
    private List<FbMat> matches;

    public String getUniRcrdId() {
        return uniRcrdId;
    }

    public void setUniRcrdId(String uniRcrdId) {
        this.uniRcrdId = uniRcrdId;
    }

    public List<FbMat> getMatches() {
        return matches;
    }

    public void setMatches(List<FbMat> matches) {
        this.matches = matches;
    }
}
