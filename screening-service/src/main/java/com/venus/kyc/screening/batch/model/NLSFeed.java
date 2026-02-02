package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "NLSFeed", namespace = "http://www.db.com/NLSFileDefinition")
@XmlAccessorType(XmlAccessType.FIELD)
public class NLSFeed {

    @XmlElement(name = "Request", namespace = "http://www.db.com/NLSFileDefinition")
    private Request request;

    @XmlElement(name = "Notification", namespace = "http://www.db.com/NLSFileDefinition")
    private Notification notification;

    @XmlElement(name = "Feedback", namespace = "http://www.db.com/NLSFileDefinition")
    private Feedback feedback;

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public Notification getNotification() {
        return notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public Feedback getFeedback() {
        return feedback;
    }

    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;
    }
}
