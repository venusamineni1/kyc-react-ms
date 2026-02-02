package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class RecordMeta {
    @XmlElement(name = "UniRcrdId", namespace = "http://www.db.com/NLSRequest")
    private String uniRcrdId;

    @XmlElement(name = "RecStat", namespace = "http://www.db.com/NLSRequest")
    private String recStat;

    @XmlElement(name = "ChkSum", namespace = "http://www.db.com/NLSRequest")
    private String chkSum;

    @XmlElement(name = "Type", namespace = "http://www.db.com/NLSRequest")
    private String type;

    public String getUniRcrdId() {
        return uniRcrdId;
    }

    public void setUniRcrdId(String uniRcrdId) {
        this.uniRcrdId = uniRcrdId;
    }

    public String getRecStat() {
        return recStat;
    }

    public void setRecStat(String recStat) {
        this.recStat = recStat;
    }

    public String getChkSum() {
        return chkSum;
    }

    public void setChkSum(String chkSum) {
        this.chkSum = chkSum;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
