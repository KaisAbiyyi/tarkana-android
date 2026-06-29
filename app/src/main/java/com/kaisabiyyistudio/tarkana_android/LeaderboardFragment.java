package com.kaisabiyyistudio.tarkana_android;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kaisabiyyistudio.tarkana_android.model.LeaderboardEntry;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LeaderboardFragment extends Fragment {
    private static final String TAG = "LeaderboardFragment";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private RecyclerView rv;
    private android.widget.TextView tvYourPosition;
    private android.widget.TextView tvYourRankBadge;
    private android.widget.TextView tvYourRating;
    private android.widget.TextView tvYourNextInfo;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private View contentLeaderboard, skeletonLeaderboard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);

        contentLeaderboard = view.findViewById(R.id.content_leaderboard);
        skeletonLeaderboard = view.findViewById(R.id.skeleton_leaderboard);

        swipeRefresh = view.findViewById(R.id.swipe_refresh_leaderboard);
        swipeRefresh.setOnRefreshListener(this::fetchLeaderboard);
        swipeRefresh.setOnChildScrollUpCallback((parent, child) ->
                contentLeaderboard != null
                        && contentLeaderboard.getVisibility() == View.VISIBLE
                        && contentLeaderboard.canScrollVertically(-1));

        rv = view.findViewById(R.id.rv_leaderboard);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new LeaderboardAdapter(new ArrayList<>()));

        tvYourPosition = view.findViewById(R.id.tv_your_position);
        tvYourRankBadge = view.findViewById(R.id.tv_your_rank_badge);
        tvYourRating = view.findViewById(R.id.tv_your_rating);
        tvYourNextInfo = view.findViewById(R.id.tv_your_next_info);

        fetchLeaderboard();
        return view;
    }

    private void fetchLeaderboard() {
        if (!swipeRefresh.isRefreshing()) {
            contentLeaderboard.setVisibility(View.GONE);
            skeletonLeaderboard.setVisibility(View.VISIBLE);
        }

        executor.execute(() -> {
            try {
                android.content.Context context = getContext();
                if (context == null) return;
                ApiClient.ApiResponse response = ApiClient.getFunction(context, "/get-leaderboard");
                response.requireSuccess();

                JSONArray arr = response.json().optJSONArray("leaderboard");
                List<LeaderboardEntry> list = new ArrayList<>();
                LeaderboardEntry currentUser = null;
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject item = arr.getJSONObject(i);
                        LeaderboardEntry entry = new LeaderboardEntry(
                                item.optInt("position"),
                                item.optString("playerName", item.optString("displayName")),
                                item.optString("rank"),
                                item.optInt("logicRating", item.optInt("rating")),
                                item.optString("accuracy", "0.0%"),
                                item.optInt("completedRounds"),
                                item.optBoolean("isCurrentUser")
                        );
                        list.add(entry);
                        if (entry.isCurrentUser()) currentUser = entry;
                    }
                }

                LeaderboardEntry finalCurrentUser = currentUser;
                handler.post(() -> renderLeaderboard(list, finalCurrentUser));
            } catch (ApiClient.AuthException e) {
                Log.e(TAG, "Auth error", e);
                handler.post(() -> showError(e.getMessage()));
            } catch (Exception e) {
                if (ApiClient.isCancellation(e)) return;
                Log.e(TAG, "Load error", e);
                handler.post(() -> showError("Failed to load leaderboard."));
            }
        });
    }

    private void renderLeaderboard(List<LeaderboardEntry> list, LeaderboardEntry currentUser) {
        if (!isAdded()) return;

        rv.setAdapter(new LeaderboardAdapter(list));
        if (currentUser != null) {
            tvYourPosition.setText("Your position: #" + currentUser.getPosition());
            renderCurrentRank(currentUser.getRank());
            tvYourRating.setText("Logic Rating " + currentUser.getLogicRating());

            if (currentUser.getPosition() > 1) {
                int targetRating = 0;
                for (LeaderboardEntry e : list) {
                    if (e.getPosition() == currentUser.getPosition() - 1) {
                        targetRating = e.getLogicRating();
                        break;
                    }
                }
                int remaining = Math.max(1, targetRating - currentUser.getLogicRating() + 1);
                tvYourNextInfo.setText("Need " + remaining + " rating to reach position " +
                        (currentUser.getPosition() - 1));
            } else {
                tvYourNextInfo.setText("You are #1! Keep it up!");
            }
        }
        showContent();
    }

    private void renderCurrentRank(String rank) {
        if (rank != null && rank.equalsIgnoreCase("Silver Solver")) {
            tvYourRankBadge.setText("SILVER SOLVER");
            tvYourRankBadge.setBackgroundResource(R.drawable.bg_badge_rank_silver);
            tvYourRankBadge.setTextColor(0xFF000000);
        } else if (rank != null && rank.equalsIgnoreCase("Bronze Mind")) {
            tvYourRankBadge.setText("BRONZE MIND");
            tvYourRankBadge.setBackgroundResource(R.drawable.bg_badge_rank_bronze);
            tvYourRankBadge.setTextColor(0xFFFFFFFF);
        } else {
            tvYourRankBadge.setText("UNRANKED");
            tvYourRankBadge.setBackgroundResource(R.drawable.bg_badge_yellow);
            tvYourRankBadge.setTextColor(0xFF000000);
        }
    }

    private void showError(String message) {
        if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        showContent();
    }

    private void showContent() {
        contentLeaderboard.setVisibility(View.VISIBLE);
        skeletonLeaderboard.setVisibility(View.GONE);
        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
    }

    @Override
    public void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
