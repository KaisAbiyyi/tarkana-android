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
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.kaisabiyyistudio.tarkana_android.model.HistoryItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.TimeZone;

public class HistoryFragment extends Fragment {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private View contentHistory, skeletonHistory, layoutHistoryEmpty;
    
    private TextView tvSessions, tvAccuracy, tvBestScore, tvRatingChange;
    private RecyclerView rv;
    private HistoryAdapter adapter;
    private List<HistoryItem> historyItems = new ArrayList<>();
    
    private View lastSelectedChip;
    private String currentTypeFilter = "All";
    
    private int currentOffset = 0;
    private final int LIMIT = 10;
    private boolean isLoading = false;
    private boolean hasMoreData = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        contentHistory = view.findViewById(R.id.content_history);
        skeletonHistory = view.findViewById(R.id.skeleton_history);
        layoutHistoryEmpty = view.findViewById(R.id.layout_history_empty);

        swipeRefresh = view.findViewById(R.id.swipe_refresh_history);
        swipeRefresh.setOnRefreshListener(() -> {
            currentOffset = 0;
            hasMoreData = true;
            historyItems.clear();
            if (adapter != null) adapter.notifyDataSetChanged();
            fetchData();
        });
        swipeRefresh.setOnChildScrollUpCallback((parent, child) -> {
            if (contentHistory != null && contentHistory.getVisibility() == View.VISIBLE) {
                return contentHistory.canScrollVertically(-1);
            }
            return false;
        });

        tvSessions = view.findViewById(R.id.tv_hist_sessions);
        tvAccuracy = view.findViewById(R.id.tv_hist_accuracy);
        tvBestScore = view.findViewById(R.id.tv_hist_best_score);
        tvRatingChange = view.findViewById(R.id.tv_hist_rating_change);

        rv = view.findViewById(R.id.rv_history_sessions);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        rv.setLayoutManager(layoutManager);
        adapter = new HistoryAdapter(historyItems);
        rv.setAdapter(adapter);

        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && !isLoading && hasMoreData) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        currentOffset += LIMIT;
                        fetchHistory(false);
                    }
                }
            }
        });

        // Filter chips
        int[] chipIds = {R.id.chip_all, R.id.chip_mixed, R.id.chip_number,
                R.id.chip_symbol, R.id.chip_deduction, R.id.chip_memory};
        String[] chipTypes = {"All", "mixed", "number", "symbol", "deduction", "memory"};

        View chipAll = view.findViewById(R.id.chip_all);
        chipAll.setBackgroundResource(R.drawable.bg_chip_selected);
        lastSelectedChip = chipAll;

        for (int i = 0; i < chipIds.length; i++) {
            final int index = i;
            View chip = view.findViewById(chipIds[i]);
            chip.setOnClickListener(v -> {
                if (lastSelectedChip != null) {
                    lastSelectedChip.setBackgroundResource(R.drawable.bg_chip_outline);
                }
                v.setBackgroundResource(R.drawable.bg_chip_selected);
                lastSelectedChip = v;
                currentTypeFilter = chipTypes[index];
                
                currentOffset = 0;
                hasMoreData = true;
                historyItems.clear();
                adapter.notifyDataSetChanged();
                fetchHistory(true);
            });
        }

        View btnPlayNext = view.findViewById(R.id.btn_play_next);
        if (btnPlayNext != null) {
            btnPlayNext.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    com.google.android.material.bottomnavigation.BottomNavigationView nav = 
                        getActivity().findViewById(R.id.bottom_nav);
                    if (nav != null) nav.setSelectedItemId(R.id.nav_challenge);
                }
            });
        }

        fetchData();

        return view;
    }

    private void fetchData() {
        if (!swipeRefresh.isRefreshing()) {
            contentHistory.setVisibility(View.GONE);
            skeletonHistory.setVisibility(View.VISIBLE);
        }
        
        executor.execute(() -> {
            try {
                if (!isAdded()) return;
                MasterKey masterKey = new MasterKey.Builder(requireContext())
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();
                android.content.SharedPreferences prefs = EncryptedSharedPreferences.create(
                        requireContext(), "auth_prefs", masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
                String token = prefs.getString("access_token", null);
                if (token == null) {
                    handler.post(() -> {
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    });
                    return;
                }
                
                // Fetch Dashboard stats
                URL urlDash = new URL(BuildConfig.SUPABASE_URL + "/functions/v1/get-dashboard");
                HttpURLConnection connDash = (HttpURLConnection) urlDash.openConnection();
                connDash.setRequestMethod("GET");
                connDash.setUseCaches(false);
                connDash.setRequestProperty("Authorization", "Bearer " + token);
                connDash.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY);

                int dashCode = connDash.getResponseCode();
                if (dashCode == 405) {
                    connDash.disconnect();
                    connDash = (HttpURLConnection) urlDash.openConnection();
                    connDash.setRequestMethod("POST");
                    connDash.setUseCaches(false);
                    connDash.setRequestProperty("Authorization", "Bearer " + token);
                    connDash.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY);
                    connDash.setRequestProperty("Content-Type", "application/json");
                    connDash.setDoOutput(true);
                    try (java.io.OutputStream os = connDash.getOutputStream()) {
                        os.write("{}".getBytes());
                        os.flush();
                    }
                    dashCode = connDash.getResponseCode();
                }

                if (dashCode == 200) {
                    Scanner scanner = new Scanner(connDash.getInputStream()).useDelimiter("\\A");
                    String resp = scanner.hasNext() ? scanner.next() : "";
                    JSONObject json = new JSONObject(resp);

                    final int totalCompleted = json.optInt("totalCompleted", 0);
                    final double avgAccuracy = json.optDouble("averageAccuracy", 0.0);
                    final int bestScore = json.optInt("bestScore", 0);
                    
                    // We don't have total rating change directly, just use what we have or 0
                    
                    handler.post(() -> {
                        tvSessions.setText(String.valueOf(totalCompleted));
                        tvAccuracy.setText(String.format(Locale.getDefault(), "%.1f%%", avgAccuracy));
                        tvBestScore.setText(String.valueOf(bestScore));
                        tvRatingChange.setText("+0");
                    });
                }
                connDash.disconnect();
                
            } catch (Exception e) {
                Log.e("HistoryFragment", "Error fetching dashboard", e);
            }
            
            // Now fetch history
            handler.post(() -> fetchHistory(false));
        });
    }

    private void fetchHistory(boolean showSkeleton) {
        if (isLoading || !hasMoreData) return;
        isLoading = true;

        if (showSkeleton && !swipeRefresh.isRefreshing()) {
            contentHistory.setVisibility(View.GONE);
            skeletonHistory.setVisibility(View.VISIBLE);
        }

        executor.execute(() -> {
            try {
                if (!isAdded()) return;
                MasterKey masterKey = new MasterKey.Builder(requireContext())
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();
                android.content.SharedPreferences prefs = EncryptedSharedPreferences.create(
                        requireContext(), "auth_prefs", masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
                String token = prefs.getString("access_token", null);
                if (token == null) {
                    isLoading = false;
                    return;
                }

                String urlStr = BuildConfig.SUPABASE_URL + "/functions/v1/get-history?limit=" + LIMIT + "&offset=" + currentOffset;
                if (!currentTypeFilter.equalsIgnoreCase("All")) {
                    urlStr += "&type=" + currentTypeFilter;
                }

                URL url = new URL(urlStr);
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
                    try (java.io.OutputStream os = conn.getOutputStream()) {
                        os.write("{}".getBytes());
                        os.flush();
                    }
                    responseCode = conn.getResponseCode();
                }

                if (responseCode == 200) {
                    Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String resp = scanner.hasNext() ? scanner.next() : "";
                    JSONObject json = new JSONObject(resp);

                    JSONArray items = json.optJSONArray("items");
                    int total = json.optInt("total", 0);
                    
                    List<HistoryItem> newItems = new ArrayList<>();
                    if (items != null) {
                        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                        parser.setTimeZone(TimeZone.getTimeZone("UTC"));
                        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.US);

                        for (int i = 0; i < items.length(); i++) {
                            JSONObject obj = items.getJSONObject(i);
                            String cType = obj.optString("challengeType", "mixed");
                            String mode = cType.toUpperCase() + " MODE";
                            String status = obj.optString("status", "completed").toUpperCase();
                            int score = obj.optInt("totalScore", 0);
                            int acc = obj.optInt("accuracy", 0);
                            int ratingDelta = obj.optInt("ratingDelta", 0);
                            
                            String dateStr = obj.optString("createdAt");
                            String formattedDate = dateStr;
                            try {
                                Date date = parser.parse(dateStr);
                                if (date != null) formattedDate = formatter.format(date);
                            } catch (Exception ignored) {}

                            newItems.add(new HistoryItem(
                                mode, "Ranked Session", 0, status, formattedDate,
                                score, acc, 0, 0, 0, ratingDelta, new ArrayList<>()
                            ));
                        }
                    }

                    handler.post(() -> {
                        historyItems.addAll(newItems);
                        adapter.notifyDataSetChanged();

                        int totalRatingChange = 0;
                        for (HistoryItem item : historyItems) {
                            totalRatingChange += item.getRatingChange();
                        }
                        
                        if (totalRatingChange >= 0) {
                            tvRatingChange.setText("+" + totalRatingChange);
                        } else {
                            tvRatingChange.setText(String.valueOf(totalRatingChange));
                        }

                        updateEmptyStateVisibility();

                        if (currentOffset + LIMIT >= total || items == null || items.length() == 0) {
                            hasMoreData = false;
                        }
                        
                        contentHistory.setVisibility(View.VISIBLE);
                        skeletonHistory.setVisibility(View.GONE);
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                        isLoading = false;
                    });

                } else {
                    String errorBody = "";
                    try (Scanner s = new Scanner(conn.getErrorStream()).useDelimiter("\\A")) {
                        errorBody = s.hasNext() ? s.next() : "";
                    } catch (Exception ignored) {}
                    
                    final String err = errorBody;
                    final int code = responseCode;
                    handler.post(() -> {
                        android.widget.Toast.makeText(requireContext(), "Error " + code + ": " + err, android.widget.Toast.LENGTH_LONG).show();
                        updateEmptyStateVisibility();
                        contentHistory.setVisibility(View.VISIBLE);
                        skeletonHistory.setVisibility(View.GONE);
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                        isLoading = false;
                    });
                }
                conn.disconnect();

            } catch (Exception e) {
                Log.e("HistoryFragment", "Error fetching history", e);
                handler.post(() -> {
                    updateEmptyStateVisibility();
                    contentHistory.setVisibility(View.VISIBLE);
                    skeletonHistory.setVisibility(View.GONE);
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    isLoading = false;
                });
            }
        });
    }

    private void updateEmptyStateVisibility() {
        if (layoutHistoryEmpty != null && rv != null) {
            if (historyItems.isEmpty()) {
                layoutHistoryEmpty.setVisibility(View.VISIBLE);
                rv.setVisibility(View.GONE);
            } else {
                layoutHistoryEmpty.setVisibility(View.GONE);
                rv.setVisibility(View.VISIBLE);
            }
        }
    }
}
