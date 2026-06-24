package com.kaisabiyyistudio.tarkana_android.model;

import java.util.List;

public class HistoryItem {
    private String mode;
    private String sessionType;
    private int questionCount;
    private String status;
    private String date;
    private int score;
    private int accuracy;
    private int avgTime;
    private int ratingBefore;
    private int ratingAfter;
    private int ratingChange;
    private List<String> badges;

    public HistoryItem(String mode, String sessionType, int questionCount, String status,
                       String date, int score, int accuracy, int avgTime,
                       int ratingBefore, int ratingAfter, int ratingChange,
                       List<String> badges) {
        this.mode = mode;
        this.sessionType = sessionType;
        this.questionCount = questionCount;
        this.status = status;
        this.date = date;
        this.score = score;
        this.accuracy = accuracy;
        this.avgTime = avgTime;
        this.ratingBefore = ratingBefore;
        this.ratingAfter = ratingAfter;
        this.ratingChange = ratingChange;
        this.badges = badges;
    }

    public String getMode() { return mode; }
    public String getSessionType() { return sessionType; }
    public int getQuestionCount() { return questionCount; }
    public String getStatus() { return status; }
    public String getDate() { return date; }
    public int getScore() { return score; }
    public int getAccuracy() { return accuracy; }
    public int getAvgTime() { return avgTime; }
    public int getRatingBefore() { return ratingBefore; }
    public int getRatingAfter() { return ratingAfter; }
    public int getRatingChange() { return ratingChange; }
    public List<String> getBadges() { return badges; }
}
