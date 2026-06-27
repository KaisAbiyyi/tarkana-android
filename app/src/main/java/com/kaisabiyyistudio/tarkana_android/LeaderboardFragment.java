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

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LeaderboardFragment extends Fragment {

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
        swipeRefresh.setOnChildScrollUpCallback((parent, child) -> {
            if (contentLeaderboard != null && contentLeaderboard.getVisibility() == View.VISIBLE) {
                return contentLeaderboard.canScrollVertically(-1);
            }
            return false;
        });

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
                if (!isAdded()) return;
                String token = AuthSession.accessToken(requireContext());
                if (token == null) return;

                URL url = new URL(BuildConfig.SUPABASE_URL + "/functions/v1/get-leaderboard");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    InputStream is = conn.getInputStream();
                    Scanner scanner = new Scanner(is).useDelimiter("\\A");
                    String response = scanner.hasNext() ? scanner.next() : "";
                    
                    JSONObject resObj = new JSONObject(response);
                    JSONArray arr = resObj.optJSONArray("leaderboard");
                    
                    List<LeaderboardEntry> list = new ArrayList<>();
                    LeaderboardEntry currentUser = null;
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject item = arr.getJSONObject(i);
                            LeaderboardEntry entry = new LeaderboardEntry(
                                item.optInt("position"),
                                item.optString("playerName"),
                                item.optString("rank"),
                                item.optInt("logicRating"),
                                item.optString("accuracy"),
                                item.optInt("completedRounds"),
                                item.optBoolean("isCurrentUser")
                            );
                            list.add(entry);
                            if (entry.isCurrentUser()) {
                                currentUser = entry;
                            }
                        }
                    }

                    final LeaderboardEntry finalCurrentUser = currentUser;
                    handler.post(() -> {
                        if (isAdded()) {
                            rv.setAdapter(new LeaderboardAdapter(list));
                            if (finalCurrentUser != null) {
                                tvYourPosition.setText("Your position: #" + finalCurrentUser.getPosition());
                                
                                String rankStr = finalCurrentUser.getRank();
                                if (rankStr != null && rankStr.equalsIgnoreCase("silver")) {
                                    tvYourRankBadge.setText("SILVER SOLVER");
                                    tvYourRankBadge.setBackgroundResource(R.drawable.bg_badge_rank_silver);
                                    tvYourRankBadge.setTextColor(0xFF000000);
                                } else if (rankStr != null && rankStr.equalsIgnoreCase("bronze")) {
                                    tvYourRankBadge.setText("BRONZE MIND");
                                    tvYourRankBadge.setBackgroundResource(R.drawable.bg_badge_rank_bronze);
                                    tvYourRankBadge.setTextColor(0xFFFFFFFF);
                                } else {
                                    tvYourRankBadge.setText("UNRANKED");
                                    tvYourRankBadge.setBackgroundResource(R.drawable.bg_badge_yellow);
                                    tvYourRankBadge.setTextColor(0xFF000000);
                                }
                                
                                tvYourRating.setText("Logic Rating " + finalCurrentUser.getLogicRating());
                                
                                if (finalCurrentUser.getPosition() > 1) {
                                    int targetRating = 0;
                                    for (LeaderboardEntry e : list) {
                                        if (e.getPosition() == finalCurrentUser.getPosition() - 1) {
                                            targetRating = e.getLogicRating();
                                            break;
                                        }
                                    }
                                    int remaining = targetRating - finalCurrentUser.getLogicRating() + 1;
                                    if (remaining <= 0) remaining = 1;
                                    tvYourNextInfo.setText("Need " + remaining + " rating to reach position " + (finalCurrentUser.getPosition() - 1));
                                } else {
                                    tvYourNextInfo.setText("You are #1! Keep it up!");
                                }
                            }
                        }
                        contentLeaderboard.setVisibility(View.VISIBLE);
                        skeletonLeaderboard.setVisibility(View.GONE);
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    });
                } else {
                    InputStream es = conn.getErrorStream();
                    String errResp = "";
                    if (es != null) {
                        Scanner scanner = new Scanner(es).useDelimiter("\\A");
                        errResp = scanner.hasNext() ? scanner.next() : "";
                        Log.e("LeaderboardFragment", "Failed to load leaderboard: " + responseCode + " - " + errResp);
                    } else {
                        Log.e("LeaderboardFragment", "Failed to load leaderboard: " + responseCode);
                    }
                    if (responseCode == 401) {
                        String newToken = AuthSession.refreshAccessToken(requireContext());
                        if (newToken != null) {
                            handler.post(this::fetchLeaderboard);
                            return;
                        }
                    }
                    handler.post(() -> {
                        contentLeaderboard.setVisibility(View.VISIBLE);
                        skeletonLeaderboard.setVisibility(View.GONE);
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e("LeaderboardFragment", "Error fetching leaderboard", e);
                handler.post(() -> {
                    contentLeaderboard.setVisibility(View.VISIBLE);
                    skeletonLeaderboard.setVisibility(View.GONE);
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
