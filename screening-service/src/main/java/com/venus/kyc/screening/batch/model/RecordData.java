package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class RecordData {
    @XmlElement(name = "PrtInfo", namespace = "http://www.db.com/NLSRequest")
    private PartyInfo prtInfo;

    @XmlElement(name = "JuriInfo", namespace = "http://www.db.com/NLSRequest")
    private JuridicalInfo juriInfo;

    @XmlElement(name = "KYCData", namespace = "http://www.db.com/NLSRequest")
    private KYCData kycData;

    @XmlElement(name = "Comment", namespace = "http://www.db.com/NLSRequest")
    private String comment;

    public PartyInfo getPrtInfo() {
        return prtInfo;
    }

    public void setPrtInfo(PartyInfo prtInfo) {
        this.prtInfo = prtInfo;
    }

    public JuridicalInfo getJuriInfo() {
        return juriInfo;
    }

    public void setJuriInfo(JuridicalInfo juriInfo) {
        this.juriInfo = juriInfo;
    }

    public KYCData getKycData() {
        return kycData;
    }

    public void setKycData(KYCData kycData) {
        this.kycData = kycData;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
