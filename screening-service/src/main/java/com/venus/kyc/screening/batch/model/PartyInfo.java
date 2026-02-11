package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class PartyInfo {
    @XmlElement(name = "Ind", namespace = "http://www.db.com/NLSRequest")
    private Individual ind;

    public Individual getInd() {
        return ind;
    }

    public void setInd(Individual ind) {
        this.ind = ind;
    }

    @XmlElement(name = "Acc", namespace = "http://www.db.com/NLSRequest")
    private Account account;

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
