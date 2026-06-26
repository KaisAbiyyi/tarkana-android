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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.kaisabiyyistudio.tarkana_android.model.SessionItem;

public class DashboardFragment extends Fragment {

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
        swipeRefresh.setOnChildScrollUpCallback((parent, child) -> {
            if (contentDashboard != null && contentDashboard.getVisibility() == View.VISIBLE) {
                return contentDashboard.canScrollVertically(-1);
            }
            return false;
        });

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

        // Start Challenge button navigation
        view.findViewById(R.id.btn_start_challenge).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                com.google.android.material.bottomnavigation.BottomNavigationView nav = 
                    getActivity().findViewById(R.id.bottom_nav);
                if (nav != null) {
                    nav.setSelectedItemId(R.id.nav_challenge);
                }
            }
        });

        // View Full History button navigation
        view.findViewById(R.id.tv_view_history).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                com.google.android.material.bottomnavigation.BottomNavigationView nav = 
                    getActivity().findViewById(R.id.bottom_nav);
                if (nav != null) {
                    nav.setSelectedItemId(R.id.nav_history);
                }
            }
        });

        fetchDashboard();

        return view;
    }

    private void fetchDashboard() {
        if (!swipeRefresh.isRefreshing()) {
            contentDashboard.setVisibility(View.GONE);
            skeletonDashboard.setVisibility(View.VISIBLE);
        }
        
        executor.execute(() -> {
            try {
                if (!isAdded()) return;
                String token = AuthSession.accessToken(requireContext());
                if (token == null) {
                    handler.post(() -> {
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    });
                    return;
                }

                URL url = new URL(BuildConfig.SUPABASE_URL + "/functions/v1/get-dashboard");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setUseCaches(false);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY);

                int responseCode = conn.getResponseCode();
                if (responseCode == 405) {
                    conn.disconnect();
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setUseCaches(false);
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write("{}".getBytes());
                        os.flush();
                    }
                    responseCode = conn.getResponseCode();
                }

                if (responseCode == 200) {
                    InputStream is = conn.getInputStream();
                    Scanner scanner = new Scanner(is).useDelimiter("\\A");
                    String response = scanner.hasNext() ? scanner.next() : "";
                    
                    JSONObject res = new JSONObject(response);
                    String rank = res.optString("currentRank", "bronze");
                    int rating = res.optInt("logicRating", 0);
                    int totalCompleted = res.optInt("totalCompleted", 0);
                    int bestScore = res.optInt("bestScore", 0);
                    double avgAccuracy = res.optDouble("averageAccuracy", 0.0);
                    double avgSolveTime = res.optDouble("averageSolveTimeSeconds", 0.0);

                    JSONArray recentArr = res.optJSONArray("recentSessions");
                    List<SessionItem> recentList = new ArrayList<>();
                    if (recentArr != null) {
                        for (int i = 0; i < recentArr.length(); i++) {
                            JSONObject obj = recentArr.getJSONObject(i);
                            String dateStr = obj.optString("createdAt", "");
                            String type = obj.optString("challengeType", "N/A");
                            int score = obj.optInt("totalScore", 0);
                            int accInt = (int) Math.round(obj.optDouble("accuracy", 0.0));
                            recentList.add(new SessionItem(type, dateStr.substring(0, Math.min(dateStr.length(), 10)), score, accInt));
                        }
                    }

                    handler.post(() -> {
                        if (isAdded()) {
                            tvLogicRating.setText(String.valueOf(rating));
                            tvSessionsCount.setText(String.valueOf(totalCompleted));
                            tvBestScore.setText(String.valueOf(bestScore));
                            tvAvgAccuracy.setText(String.format(java.util.Locale.US, "%.1f%%", avgAccuracy));
                            
                            int min = (int) (avgSolveTime / 60);
                            int sec = (int) (avgSolveTime % 60);
                            tvAvgTime.setText(String.format(java.util.Locale.US, "%dm %ds", min, sec));

                            int nextTarget = rank.equalsIgnoreCase("silver") ? 1000 : 500;
                            int progress = (int) ((rating / (float) nextTarget) * 100);
                            if (progress > 100) progress = 100;
                            pbRankProgress.setProgress(progress);
                            tvRankPct.setText(progress + "%");
                            
                            int remaining = nextTarget - rating;
                            if (remaining <= 0) {
                                tvRankRemaining.setText("You have reached the next rank!");
                            } else {
                                tvRankRemaining.setText(remaining + " rating remaining to rank up");
                            }

                            if (rank.equalsIgnoreCase("silver")) {
                                tvRankBadge.setText("SILVER SOLVER");
                                ((View)tvRankBadge.getParent()).setBackgroundResource(R.drawable.bg_badge_rank_silver);
                            } else {
                                tvRankBadge.setText("BRONZE MIND");
                                ((View)tvRankBadge.getParent()).setBackgroundResource(R.drawable.bg_badge_rank_bronze);
                            }

                            if (recentList.isEmpty()) {
                                rvRecentSessions.setVisibility(View.GONE);
                                layoutRecentEmpty.setVisibility(View.VISIBLE);
                            } else {
                                rvRecentSessions.setVisibility(View.VISIBLE);
                                layoutRecentEmpty.setVisibility(View.GONE);
                                rvRecentSessions.setAdapter(new SessionAdapter(recentList));
                            }
                        }
                        contentDashboard.setVisibility(View.VISIBLE);
                        skeletonDashboard.setVisibility(View.GONE);
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    });
                } else {
                    InputStream es = conn.getErrorStream();
                    String errResp = "";
                    if (es != null) {
                        Scanner scanner = new Scanner(es).useDelimiter("\\A");
                        errResp = scanner.hasNext() ? scanner.next() : "";
                        Log.e("DashboardFragment", "Failed: " + responseCode + " - " + errResp);
                    }
                    if (responseCode == 401) {
                        String newToken = AuthSession.refreshAccessToken(requireContext());
                        if (newToken != null) {
                            handler.post(this::fetchDashboard);
                            return;
                        }
                    }
                    handler.post(() -> {
                        contentDashboard.setVisibility(View.VISIBLE);
                        skeletonDashboard.setVisibility(View.GONE);
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e("DashboardFragment", "Error fetching dashboard", e);
                handler.post(() -> {
                    contentDashboard.setVisibility(View.VISIBLE);
                    skeletonDashboard.setVisibility(View.GONE);
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                });
            }
        });
    }
}
