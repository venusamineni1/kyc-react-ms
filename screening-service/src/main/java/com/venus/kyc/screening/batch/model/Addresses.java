package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Addresses {
    @XmlElement(name = "Addr", namespace = "http://www.db.com/NLSPartyInformation")
    private java.util.List<Address> addrList;

    public java.util.List<Address> getAddrList() {
        return addrList;
    }

    public void setAddrList(java.util.List<Address> addrList) {
        this.addrList = addrList;
    }
}
