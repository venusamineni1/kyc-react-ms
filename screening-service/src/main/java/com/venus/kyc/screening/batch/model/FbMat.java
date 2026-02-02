package com.venus.kyc.screening.batch.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class FbMat {
    @XmlElement(name = "MatchId", namespace = "http://www.db.com/NLSFeedback")
    private String matchId;

    @XmlElement(name = "MatchName", namespace = "http://www.db.com/NLSFeedback")
    private String matchName;

    @XmlElement(name = "Score", namespace = "http://www.db.com/NLSFeedback")
    private String score;

    @XmlElement(name = "Stat", namespace = "http://www.db.com/NLSFeedback")
    private String stat;

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getMatchName() {
        return matchName;
    }

    public void setMatchName(String matchName) {
        this.matchName = matchName;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getStat() {
        return stat;
    }

    public void setStat(String stat) {
        this.stat = stat;
    }
}
