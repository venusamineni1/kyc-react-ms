package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Feedback", namespace = "http://www.db.com/NLSFeedback")
@XmlAccessorType(XmlAccessType.FIELD)
public class Feedback {
    @XmlElement(name = "Meta", namespace = "http://www.db.com/NLSFeedback")
    private FeedbackMeta meta;

    @XmlElement(name = "FbRecs", namespace = "http://www.db.com/NLSFeedback")
    private FbRecs fbRecs;

    public FbRecs getFbRecs() {
        return fbRecs;
    }

    public void setFbRecs(FbRecs fbRecs) {
        this.fbRecs = fbRecs;
    }

    public FeedbackMeta getMeta() {
        return meta;
    }

    public void setMeta(FeedbackMeta meta) {
        this.meta = meta;
    }
}
