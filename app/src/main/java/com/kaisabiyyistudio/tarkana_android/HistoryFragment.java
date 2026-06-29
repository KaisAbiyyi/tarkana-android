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

import com.kaisabiyyistudio.tarkana_android.model.HistoryItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryFragment extends Fragment {
    private static final String TAG = "HistoryFragment";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private View contentHistory, skeletonHistory, layoutHistoryEmpty;
    private TextView tvSessions, tvAccuracy, tvBestScore, tvRatingChange;
    private RecyclerView rv;
    private HistoryAdapter adapter;
    private final List<HistoryItem> historyItems = new ArrayList<>();

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
            resetHistory();
            fetchData();
        });
        swipeRefresh.setOnChildScrollUpCallback((parent, child) ->
                contentHistory != null
                        && contentHistory.getVisibility() == View.VISIBLE
                        && contentHistory.canScrollVertically(-1));

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
                if (dy <= 0 || isLoading || !hasMoreData) return;
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0) {
                    currentOffset += LIMIT;
                    fetchHistory(false);
                }
            }
        });

        setupFilters(view);

        View btnPlayNext = view.findViewById(R.id.btn_play_next);
        if (btnPlayNext != null) {
            btnPlayNext.setOnClickListener(v -> {
                if (!(getActivity() instanceof MainActivity)) return;
                com.google.android.material.bottomnavigation.BottomNavigationView nav =
                        getActivity().findViewById(R.id.bottom_nav);
                if (nav != null) nav.setSelectedItemId(R.id.nav_challenge);
            });
        }

        fetchData();
        return view;
    }

    private void setupFilters(View view) {
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
                resetHistory();
                fetchHistory(true);
            });
        }
    }

    private void resetHistory() {
        currentOffset = 0;
        hasMoreData = true;
        historyItems.clear();
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void fetchData() {
        if (!swipeRefresh.isRefreshing()) {
            contentHistory.setVisibility(View.GONE);
            skeletonHistory.setVisibility(View.VISIBLE);
        }

        executor.execute(() -> {
            try {
                android.content.Context context = getContext();
                if (context == null) return;
                ApiClient.ApiResponse response = ApiClient.getFunction(context, "/get-dashboard");
                response.requireSuccess();
                JSONObject json = response.json();
                int totalCompleted = json.optInt("totalCompleted", 0);
                double avgAccuracy = json.optDouble("averageAccuracy", 0.0);
                int bestScore = json.optInt("bestScore", 0);
                int totalRatingDelta = json.optInt("totalRatingDelta", 0);

                handler.post(() -> {
                    tvSessions.setText(String.valueOf(totalCompleted));
                    tvAccuracy.setText(String.format(Locale.getDefault(), "%.1f%%", avgAccuracy));
                    tvBestScore.setText(String.valueOf(bestScore));
                    tvRatingChange.setText(totalRatingDelta >= 0
                            ? "+" + totalRatingDelta
                            : String.valueOf(totalRatingDelta));
                    fetchHistory(false);
                });
            } catch (ApiClient.AuthException e) {
                Log.e(TAG, "Auth error", e);
                handler.post(() -> showError(e.getMessage()));
            } catch (Exception e) {
                if (ApiClient.isCancellation(e)) return;
                Log.e(TAG, "Stats load error", e);
                handler.post(() -> fetchHistory(false));
            }
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
                android.content.Context context = getContext();
                if (context == null) return;
                String path = "/get-history?limit=" + LIMIT + "&offset=" + currentOffset;
                if (!currentTypeFilter.equalsIgnoreCase("All")) {
                    path += "&type=" + currentTypeFilter;
                }
                ApiClient.ApiResponse response = ApiClient.getFunction(context, path);
                response.requireSuccess();
                JSONObject json = response.json();

                JSONArray items = json.optJSONArray("items");
                int total = json.optInt("total", 0);
                List<HistoryItem> newItems = parseHistoryItems(items);

                handler.post(() -> {
                    historyItems.addAll(newItems);
                    adapter.notifyDataSetChanged();
                    updateTotalRatingChange();
                    updateEmptyStateVisibility();

                    if (currentOffset + LIMIT >= total || items == null || items.length() == 0) {
                        hasMoreData = false;
                    }
                    isLoading = false;
                    showContent();
                });
            } catch (ApiClient.AuthException e) {
                Log.e(TAG, "Auth error", e);
                handler.post(() -> showError(e.getMessage()));
            } catch (Exception e) {
                if (ApiClient.isCancellation(e)) return;
                Log.e(TAG, "History load error", e);
                handler.post(() -> showError("Failed to load history."));
            }
        });
    }

    private List<HistoryItem> parseHistoryItems(JSONArray items) throws Exception {
        List<HistoryItem> newItems = new ArrayList<>();
        if (items == null) return newItems;

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
        return newItems;
    }

    private void updateTotalRatingChange() {
        int totalRatingChange = 0;
        for (HistoryItem item : historyItems) {
            totalRatingChange += item.getRatingChange();
        }
        tvRatingChange.setText(totalRatingChange >= 0
                ? "+" + totalRatingChange
                : String.valueOf(totalRatingChange));
    }

    private void updateEmptyStateVisibility() {
        if (layoutHistoryEmpty == null || rv == null) return;
        layoutHistoryEmpty.setVisibility(historyItems.isEmpty() ? View.VISIBLE : View.GONE);
        rv.setVisibility(historyItems.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        isLoading = false;
        updateEmptyStateVisibility();
        showContent();
    }

    private void showContent() {
        contentHistory.setVisibility(View.VISIBLE);
        skeletonHistory.setVisibility(View.GONE);
        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
    }

    @Override
    public void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
