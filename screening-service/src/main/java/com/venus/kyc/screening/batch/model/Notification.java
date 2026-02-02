package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Notification", namespace = "http://www.db.com/NLSNotification")
@XmlAccessorType(XmlAccessType.FIELD)
public class Notification {
    @XmlElement(name = "Meta", namespace = "http://www.db.com/NLSNotification")
    private NotificationMeta meta;

    @XmlElement(name = "RecordNoti", namespace = "http://www.db.com/NLSNotification")
    private RecordNoti recordNoti;

    public RecordNoti getRecordNoti() {
        return recordNoti;
    }

    public void setRecordNoti(RecordNoti recordNoti) {
        this.recordNoti = recordNoti;
    }

    public NotificationMeta getMeta() {
        return meta;
    }

    public void setMeta(NotificationMeta meta) {
        this.meta = meta;
    }
}
