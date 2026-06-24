package com.kaisabiyyistudio.tarkana_android.model;

public class SessionItem {
    private String name;
    private String date;
    private int points;
    private int percentage;

    public SessionItem(String name, String date, int points, int percentage) {
        this.name = name;
        this.date = date;
        this.points = points;
        this.percentage = percentage;
    }

    public String getName() { return name; }
    public String getDate() { return date; }
    public int getPoints() { return points; }
    public int getPercentage() { return percentage; }
}
