package com.kaisabiyyistudio.tarkana_android;

import com.kaisabiyyistudio.tarkana_android.model.HistoryItem;
import com.kaisabiyyistudio.tarkana_android.model.LeaderboardEntry;
import com.kaisabiyyistudio.tarkana_android.model.SessionItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DummyData {

    public static List<SessionItem> getRecentSessions() {
        List<SessionItem> list = new ArrayList<>();
        list.add(new SessionItem("Quick Session", "Jun 23, 2026, 08:21 PM", 0, 0));
        list.add(new SessionItem("Long Session", "Jun 22, 2026, 07:30 PM", 0, 0));
        list.add(new SessionItem("Long Session", "Jun 22, 2026, 05:42 PM", 0, 0));
        list.add(new SessionItem("Long Session", "Jun 22, 2026, 05:42 PM", 0, 0));
        list.add(new SessionItem("Long Session", "Jun 22, 2026, 05:22 PM", 0, 0));
        return list;
    }

    public static List<HistoryItem> getHistorySessions() {
        List<HistoryItem> list = new ArrayList<>();
        list.add(new HistoryItem("MIXED MODE", "Standard Round", 10, "COMPLETED",
                "Jun 20, 2026, 07:10 PM", 2230, 90, 6, 65, 105, 40,
                Arrays.asList("BEST IN THIS FORMAT", "IMPROVED FROM PREVIOUS")));
        list.add(new HistoryItem("MIXED MODE", "Long Session", 20, "COMPLETED",
                "Jun 20, 2026, 07:07 PM", 4091, 80, 7, 40, 65, 25,
                new ArrayList<>()));
        list.add(new HistoryItem("MIXED MODE", "Standard Round", 10, "COMPLETED",
                "Jun 20, 2026, 07:05 PM", 2136, 90, 7, 0, 40, 40,
                Arrays.asList("BEST ACCURACY")));
        return list;
    }

    public static List<LeaderboardEntry> getLeaderboard() {
        List<LeaderboardEntry> list = new ArrayList<>();
        list.add(new LeaderboardEntry(1, "Codex Dummy QA", "SILVER SOLVER", 520, "100%", 13, false));
        list.add(new LeaderboardEntry(2, "Nazar Muhammad Fikri Fadillah", "SILVER SOLVER", 500, "89%", 15, false));
        list.add(new LeaderboardEntry(3, "Jarwo Eddy Wicaksono", "BRONZE MIND", 170, "86.7%", 6, false));
        list.add(new LeaderboardEntry(4, "Kais Abiyyi", "BRONZE MIND", 105, "86.7%", 3, true));
        list.add(new LeaderboardEntry(5, "Kais Abiyyi", "BRONZE MIND", 40, "50%", 2, false));
        return list;
    }
}
