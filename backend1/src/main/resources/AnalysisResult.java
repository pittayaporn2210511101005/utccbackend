package com.example.backend1.Analysis;

public class AnalysisResult {

    private String text;

    // sentiment
    private String sentimentLabel;
    private Double sentimentScore;

    // faculty (ใหม่)
    private String faculty;

    // ตอนนี้ยังไม่ทำ ABSA เลยใช้ String/null ไปก่อน
    private String absaRaw;

    // ---------- getter / setter ----------

    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }

    public String getSentimentLabel() {
        return sentimentLabel;
    }
    public void setSentimentLabel(String sentimentLabel) {
        this.sentimentLabel = sentimentLabel;
    }

    public Double getSentimentScore() {
        return sentimentScore;
    }
    public void setSentimentScore(Double sentimentScore) {
        this.sentimentScore = sentimentScore;
    }

    public String getAbsaRaw() {
        return absaRaw;
    }
    public void setAbsaRaw(String absaRaw) {
        this.absaRaw = absaRaw;
    }
}
