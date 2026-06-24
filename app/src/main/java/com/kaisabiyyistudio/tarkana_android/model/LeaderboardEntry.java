package com.kaisabiyyistudio.tarkana_android.model;

public class LeaderboardEntry {
    private int position;
    private String playerName;
    private String rank;
    private int logicRating;
    private String accuracy;
    private int completedRounds;
    private boolean isCurrentUser;

    public LeaderboardEntry(int position, String playerName, String rank,
                            int logicRating, String accuracy, int completedRounds,
                            boolean isCurrentUser) {
        this.position = position;
        this.playerName = playerName;
        this.rank = rank;
        this.logicRating = logicRating;
        this.accuracy = accuracy;
        this.completedRounds = completedRounds;
        this.isCurrentUser = isCurrentUser;
    }

    public int getPosition() { return position; }
    public String getPlayerName() { return playerName; }
    public String getRank() { return rank; }
    public int getLogicRating() { return logicRating; }
    public String getAccuracy() { return accuracy; }
    public int getCompletedRounds() { return completedRounds; }
    public boolean isCurrentUser() { return isCurrentUser; }
}
