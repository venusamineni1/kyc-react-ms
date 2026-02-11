package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class BUInfo {
    @XmlElement(name = "RelSrcId", namespace = "http://www.db.com/NLSRequest")
    private String relSrcId;

    @XmlElement(name = "RecCntrOrg", namespace = "http://www.db.com/NLSRequest")
    private String recCntrOrg;

    @XmlElement(name = "RecBD", namespace = "http://www.db.com/NLSRequest")
    private String recBD;

    @XmlElement(name = "DBLE", namespace = "http://www.db.com/NLSRequest")
    private String dble;

    @XmlElement(name = "DBLELoc", namespace = "http://www.db.com/NLSRequest")
    private String dbleLoc;

    @XmlElement(name = "LBJ", namespace = "http://www.db.com/NLSRequest")
    private String lbj;

    @XmlElement(name = "LAFCJ", namespace = "http://www.db.com/NLSRequest")
    private String lafcj;

    @XmlElement(name = "BSRL", namespace = "http://www.db.com/NLSRequest")
    private String bsrl;

    @XmlElement(name = "RR", namespace = "http://www.db.com/NLSRequest")
    private String rr;

    @XmlElement(name = "HRPI", namespace = "http://www.db.com/NLSRequest")
    private String hrpi;

    public String getRelSrcId() {
        return relSrcId;
    }

    public void setRelSrcId(String relSrcId) {
        this.relSrcId = relSrcId;
    }

    public String getRecCntrOrg() {
        return recCntrOrg;
    }

    public void setRecCntrOrg(String recCntrOrg) {
        this.recCntrOrg = recCntrOrg;
    }

    public String getRecBD() {
        return recBD;
    }

    public void setRecBD(String recBD) {
        this.recBD = recBD;
    }

    public String getDble() {
        return dble;
    }

    public void setDble(String dble) {
        this.dble = dble;
    }

    public String getDbleLoc() {
        return dbleLoc;
    }

    public void setDbleLoc(String dbleLoc) {
        this.dbleLoc = dbleLoc;
    }

    public String getLbj() {
        return lbj;
    }

    public void setLbj(String lbj) {
        this.lbj = lbj;
    }

    public String getLafcj() {
        return lafcj;
    }

    public void setLafcj(String lafcj) {
        this.lafcj = lafcj;
    }

    public String getBsrl() {
        return bsrl;
    }

    public void setBsrl(String bsrl) {
        this.bsrl = bsrl;
    }

    public String getRr() {
        return rr;
    }

    public void setRr(String rr) {
        this.rr = rr;
    }

    public String getHrpi() {
        return hrpi;
    }

    public void setHrpi(String hrpi) {
        this.hrpi = hrpi;
    }
}
