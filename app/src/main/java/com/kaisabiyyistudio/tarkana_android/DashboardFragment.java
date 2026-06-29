package com.kaisabiyyistudio.tarkana_android;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kaisabiyyistudio.tarkana_android.model.SessionItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardFragment extends Fragment {
    private static final String TAG = "DashboardFragment";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private TextView tvLogicRating, tvRankBadge, tvSessionsCount, tvBestScore;
    private TextView tvAvgAccuracy, tvAvgTime;
    private android.widget.ProgressBar pbRankProgress;
    private TextView tvRankPct, tvRankRemaining;
    private View layoutRecentEmpty;
    private RecyclerView rvRecentSessions;
    private View contentDashboard, skeletonDashboard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        contentDashboard = view.findViewById(R.id.content_dashboard);
        skeletonDashboard = view.findViewById(R.id.skeleton_dashboard);

        swipeRefresh = view.findViewById(R.id.swipe_refresh_dashboard);
        swipeRefresh.setOnRefreshListener(this::fetchDashboard);
        swipeRefresh.setOnChildScrollUpCallback((parent, child) ->
                contentDashboard != null
                        && contentDashboard.getVisibility() == View.VISIBLE
                        && contentDashboard.canScrollVertically(-1));

        tvLogicRating = view.findViewById(R.id.tv_logic_rating);
        tvRankBadge = view.findViewById(R.id.tv_rank_badge);
        tvSessionsCount = view.findViewById(R.id.tv_sessions_count);
        tvBestScore = view.findViewById(R.id.tv_best_score);
        tvAvgAccuracy = view.findViewById(R.id.tv_avg_accuracy);
        tvAvgTime = view.findViewById(R.id.tv_avg_time);
        pbRankProgress = view.findViewById(R.id.pb_rank_progress);
        tvRankPct = view.findViewById(R.id.tv_rank_pct);
        tvRankRemaining = view.findViewById(R.id.tv_rank_remaining);
        layoutRecentEmpty = view.findViewById(R.id.layout_recent_empty);

        rvRecentSessions = view.findViewById(R.id.rv_recent_sessions);
        rvRecentSessions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecentSessions.setAdapter(new SessionAdapter(new ArrayList<>()));

        view.findViewById(R.id.btn_start_challenge).setOnClickListener(v -> selectNav(R.id.nav_challenge));
        view.findViewById(R.id.tv_view_history).setOnClickListener(v -> selectNav(R.id.nav_history));

        fetchDashboard();
        return view;
    }

    private void selectNav(int id) {
        if (!(getActivity() instanceof MainActivity)) return;
        com.google.android.material.bottomnavigation.BottomNavigationView nav =
                getActivity().findViewById(R.id.bottom_nav);
        if (nav != null) nav.setSelectedItemId(id);
    }

    private void fetchDashboard() {
        if (!swipeRefresh.isRefreshing()) {
            contentDashboard.setVisibility(View.GONE);
            skeletonDashboard.setVisibility(View.VISIBLE);
        }

        executor.execute(() -> {
            try {
                android.content.Context context = getContext();
                if (context == null) return;
                ApiClient.ApiResponse response = ApiClient.getFunction(context, "/get-dashboard");
                response.requireSuccess();
                JSONObject res = response.json();

                String rank = res.optString("currentRank", "bronze");
                int rating = res.optInt("logicRating", 0);
                int totalCompleted = res.optInt("totalCompleted", 0);
                int bestScore = res.optInt("bestScore", 0);
                double avgAccuracy = res.optDouble("averageAccuracy", 0.0);
                double avgSolveTime = res.optDouble("averageSolveTimeSeconds", 0.0);
                List<SessionItem> recentList = parseRecentSessions(res.optJSONArray("recentSessions"));

                handler.post(() -> renderDashboard(rank, rating, totalCompleted, bestScore,
                        avgAccuracy, avgSolveTime, recentList));
            } catch (ApiClient.AuthException e) {
                Log.e(TAG, "Auth error", e);
                handler.post(() -> showError(e.getMessage()));
            } catch (Exception e) {
                if (ApiClient.isCancellation(e)) return;
                Log.e(TAG, "Load error", e);
                handler.post(() -> showError("Failed to load dashboard."));
            }
        });
    }

    private List<SessionItem> parseRecentSessions(JSONArray recentArr) throws Exception {
        List<SessionItem> recentList = new ArrayList<>();
        if (recentArr == null) return recentList;

        for (int i = 0; i < recentArr.length(); i++) {
            JSONObject obj = recentArr.getJSONObject(i);
            String dateStr = obj.optString("createdAt", "");
            String type = obj.optString("challengeType", "N/A");
            int score = obj.optInt("totalScore", 0);
            int accInt = (int) Math.round(obj.optDouble("accuracy", 0.0));
            recentList.add(new SessionItem(
                    type,
                    dateStr.substring(0, Math.min(dateStr.length(), 10)),
                    score,
                    accInt
            ));
        }
        return recentList;
    }

    private void renderDashboard(String rank, int rating, int totalCompleted, int bestScore,
                                 double avgAccuracy, double avgSolveTime,
                                 List<SessionItem> recentList) {
        if (!isAdded()) return;

        tvLogicRating.setText(String.valueOf(rating));
        tvSessionsCount.setText(String.valueOf(totalCompleted));
        tvBestScore.setText(String.valueOf(bestScore));
        tvAvgAccuracy.setText(String.format(java.util.Locale.US, "%.1f%%", avgAccuracy));

        int min = (int) (avgSolveTime / 60);
        int sec = (int) (avgSolveTime % 60);
        tvAvgTime.setText(String.format(java.util.Locale.US, "%dm %ds", min, sec));

        int nextTarget = rank.equalsIgnoreCase("silver") ? 1000 : 500;
        int progress = Math.min(100, (int) ((rating / (float) nextTarget) * 100));
        pbRankProgress.setProgress(progress);
        tvRankPct.setText(progress + "%");

        int remaining = nextTarget - rating;
        tvRankRemaining.setText(remaining <= 0
                ? "You have reached the next rank!"
                : remaining + " rating remaining to rank up");

        if (rank.equalsIgnoreCase("silver")) {
            tvRankBadge.setText("SILVER SOLVER");
            ((View) tvRankBadge.getParent()).setBackgroundResource(R.drawable.bg_badge_rank_silver);
        } else {
            tvRankBadge.setText("BRONZE MIND");
            ((View) tvRankBadge.getParent()).setBackgroundResource(R.drawable.bg_badge_rank_bronze);
        }

        if (recentList.isEmpty()) {
            rvRecentSessions.setVisibility(View.GONE);
            layoutRecentEmpty.setVisibility(View.VISIBLE);
        } else {
            rvRecentSessions.setVisibility(View.VISIBLE);
            layoutRecentEmpty.setVisibility(View.GONE);
            rvRecentSessions.setAdapter(new SessionAdapter(recentList));
        }

        showContent();
    }

    private void showError(String message) {
        if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        showContent();
    }

    private void showContent() {
        contentDashboard.setVisibility(View.VISIBLE);
        skeletonDashboard.setVisibility(View.GONE);
        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
    }

    @Override
    public void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
