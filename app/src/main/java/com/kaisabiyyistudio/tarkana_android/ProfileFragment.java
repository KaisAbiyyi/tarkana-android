package com.kaisabiyyistudio.tarkana_android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import android.util.Log;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    private TextView tvProfileName;
    private TextView tvSummaryName;
    private EditText etDisplayName;
    private TextView tvLogicRating;
    private TextView tvAccuracy;
    private TextView tvCompletedRounds;
    private TextView tvEmail;
    private Button btnSaveChanges;
    private TextView tvStatusNoChanges;
    
    private String originalDisplayName = "";
    
    private TextView tvBadgeRank1;
    private TextView tvBadgeRank2;
    private TextView tvRatingRemaining;
    private ProgressBar pbRatingProgress;
    private TextView tvRatingToward;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private View contentProfile, skeletonProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        contentProfile = view.findViewById(R.id.content_profile);
        skeletonProfile = view.findViewById(R.id.skeleton_profile);
        
        swipeRefresh = view.findViewById(R.id.swipe_refresh_profile);
        swipeRefresh.setOnRefreshListener(this::fetchProfile);
        swipeRefresh.setOnChildScrollUpCallback((parent, child) -> {
            if (contentProfile != null && contentProfile.getVisibility() == View.VISIBLE) {
                return contentProfile.canScrollVertically(-1);
            }
            return false;
        });
        
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvSummaryName = view.findViewById(R.id.tv_summary_name);
        etDisplayName = view.findViewById(R.id.et_display_name);
        tvLogicRating = view.findViewById(R.id.tv_logic_rating);
        tvAccuracy = view.findViewById(R.id.tv_accuracy);
        tvCompletedRounds = view.findViewById(R.id.tv_completed_rounds);
        tvEmail = view.findViewById(R.id.tv_email);
        btnSaveChanges = view.findViewById(R.id.btn_save_changes);
        tvStatusNoChanges = view.findViewById(R.id.tv_status_no_changes);
        
        tvBadgeRank1 = view.findViewById(R.id.tv_badge_rank_1);
        tvBadgeRank2 = view.findViewById(R.id.tv_badge_rank_2);
        tvRatingRemaining = view.findViewById(R.id.tv_rating_remaining);
        pbRatingProgress = view.findViewById(R.id.pb_rating_progress);
        tvRatingToward = view.findViewById(R.id.tv_rating_toward);
        
        // Clear dummy data (Set clean default values instead of "...")
        tvProfileName.setText("");
        tvSummaryName.setText("");
        etDisplayName.setText("");
        tvLogicRating.setText("0");
        tvAccuracy.setText("0.0%");
        tvCompletedRounds.setText("0");
        tvEmail.setText("");
        tvBadgeRank1.setText("BRONZE MIND");
        tvBadgeRank2.setText("BRONZE MIND");
        tvBadgeRank1.setBackgroundResource(R.drawable.bg_badge_rank_bronze);
        tvBadgeRank2.setBackgroundResource(R.drawable.bg_badge_rank_bronze);
        tvRatingRemaining.setText("0%");
        pbRatingProgress.setMax(500);
        pbRatingProgress.setProgress(0);
        tvRatingToward.setText("Toward Silver Solver");
        
        etDisplayName.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSaveChangesButtonState();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        
        btnSaveChanges.setOnClickListener(v -> saveProfile());
        
        Button btnLogout = view.findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> logout());
        
        fetchProfile();
        
        return view;
    }
    
    private String getToken() {
        try {
            return AuthSession.accessToken(requireContext());
        } catch (Exception e) {
            Log.e("ProfileFragment", "Error getting token", e);
            return null;
        }
    }

    private void fetchProfile() {
        if (!swipeRefresh.isRefreshing()) {
            contentProfile.setVisibility(View.GONE);
            skeletonProfile.setVisibility(View.VISIBLE);
        }

        Log.d("ProfileFragment", "fetchProfile() started");
        String token = getToken();
        if (token == null) {
            Log.e("ProfileFragment", "Token is null!");
            handler.post(() -> Toast.makeText(requireContext(), "Auth token not found!", Toast.LENGTH_SHORT).show());
            return;
        }
        Log.d("ProfileFragment", "Token found: " + token.substring(0, Math.min(10, token.length())) + "...");
        
        executor.execute(() -> {
            try {
                // 1. Fetch Profile
                URL url = new URL(BuildConfig.SUPABASE_URL + "/functions/v1/get-profile");
                Log.d("ProfileFragment", "Connecting to: " + url.toString());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setUseCaches(false);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY);

                int responseCode = conn.getResponseCode();
                Log.d("ProfileFragment", "GET response code: " + responseCode);
                if (responseCode == 405) {
                    conn.disconnect();
                    url = new URL(BuildConfig.SUPABASE_URL + "/functions/v1/get-profile");
                    Log.d("ProfileFragment", "405 received. Retrying with POST: " + url.toString());
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
                    Log.d("ProfileFragment", "GET response: " + response);
                    
                    JSONObject res = new JSONObject(response);
                    String displayName = res.optString("displayName", "Unknown Player");
                    String email = res.optString("email", "unknown@email.com");
                    int rating = res.optInt("rating", 0);
                    String rank = res.optString("rank", "bronze");
                    
                    handler.post(() -> {
                        originalDisplayName = displayName;
                        tvProfileName.setText(displayName);
                        tvSummaryName.setText(displayName);
                        etDisplayName.setText(displayName);
                        tvEmail.setText(email);
                        tvLogicRating.setText(String.valueOf(rating));
                        
                        updateRankUI(rating, rank);
                        updateSaveChangesButtonState();
                        
                        // 2. Fetch Dashboard stats
                        fetchDashboardStats(token);
                    });
                } else {
                    InputStream es = conn.getErrorStream();
                    String errResp = "";
                    if (es != null) {
                        Scanner scanner = new Scanner(es).useDelimiter("\\A");
                        errResp = scanner.hasNext() ? scanner.next() : "";
                        Log.e("ProfileFragment", "Failed to fetch profile: " + responseCode + " - " + errResp);
                    } else {
                        Log.e("ProfileFragment", "Failed to fetch profile: " + responseCode);
                    }
                    
                    if (responseCode == 401) {
                        String newToken = AuthSession.refreshAccessToken(requireContext());
                        if (newToken != null) handler.post(this::fetchProfile);
                        return;
                    }
                }

                // 2. Fetch Dashboard stats
                URL dashUrl = new URL(BuildConfig.SUPABASE_URL + "/functions/v1/get-dashboard");
                Log.d("ProfileFragment", "Connecting to dashboard: " + dashUrl.toString());
                HttpURLConnection dashConn = (HttpURLConnection) dashUrl.openConnection();
                dashConn.setRequestMethod("GET");
                dashConn.setUseCaches(false);
                dashConn.setRequestProperty("Authorization", "Bearer " + token);
                dashConn.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY);

                int dashResponseCode = dashConn.getResponseCode();
                Log.d("ProfileFragment", "Dashboard response code: " + dashResponseCode);
                if (dashResponseCode == 405) {
                    dashConn.disconnect();
                    dashConn = (HttpURLConnection) dashUrl.openConnection();
                    dashConn.setRequestMethod("POST");
                    dashConn.setUseCaches(false);
                    dashConn.setRequestProperty("Authorization", "Bearer " + token);
                    dashConn.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY);
                    dashConn.setRequestProperty("Content-Type", "application/json");
                    dashConn.setDoOutput(true);
                    try (OutputStream os = dashConn.getOutputStream()) {
                        os.write("{}".getBytes());
                        os.flush();
                    }
                    dashResponseCode = dashConn.getResponseCode();
                }

                if (dashResponseCode == 200) {
                    InputStream is = dashConn.getInputStream();
                    Scanner scanner = new Scanner(is).useDelimiter("\\A");
                    String response = scanner.hasNext() ? scanner.next() : "";
                    Log.d("ProfileFragment", "Dashboard response: " + response);
                    
                    JSONObject res = new JSONObject(response);
                    double averageAccuracy = res.optDouble("averageAccuracy", 0.0);
                    int totalCompleted = res.optInt("totalCompleted", 0);
                    
                    handler.post(() -> {
                        tvAccuracy.setText(String.format(java.util.Locale.US, "%.1f%%", averageAccuracy));
                        tvCompletedRounds.setText(String.valueOf(totalCompleted));
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    });
                } else {
                    Log.e("ProfileFragment", "Dashboard request failed with code: " + dashResponseCode);
                    handler.post(() -> {
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    });
                }
            } catch (Exception e) {
                Log.e("ProfileFragment", "Error fetching profile", e);
                handler.post(() -> {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                });
            }
        });
    }

    private void fetchDashboardStats(String token) {
        executor.execute(() -> {
            try {
                URL dashUrl = new URL(BuildConfig.SUPABASE_URL + "/functions/v1/get-dashboard");
                HttpURLConnection dashConn = (HttpURLConnection) dashUrl.openConnection();
                dashConn.setRequestMethod("GET");
                dashConn.setUseCaches(false);
                dashConn.setRequestProperty("Authorization", "Bearer " + token);
                dashConn.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY);

                int dashResponseCode = dashConn.getResponseCode();
                if (dashResponseCode == 405) {
                    dashConn.disconnect();
                    dashConn = (HttpURLConnection) dashUrl.openConnection();
                    dashConn.setRequestMethod("POST");
                    dashConn.setUseCaches(false);
                    dashConn.setRequestProperty("Authorization", "Bearer " + token);
                    dashConn.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY);
                    dashConn.setRequestProperty("Content-Type", "application/json");
                    dashConn.setDoOutput(true);
                    try (OutputStream os = dashConn.getOutputStream()) {
                        os.write("{}".getBytes());
                        os.flush();
                    }
                    dashResponseCode = dashConn.getResponseCode();
                }

                if (dashResponseCode == 200) {
                    InputStream is = dashConn.getInputStream();
                    Scanner scanner = new Scanner(is).useDelimiter("\\A");
                    String response = scanner.hasNext() ? scanner.next() : "";
                    
                    JSONObject res = new JSONObject(response);
                    double averageAccuracy = res.optDouble("averageAccuracy", 0.0);
                    int totalCompleted = res.optInt("totalCompleted", 0);
                    
                    handler.post(() -> {
                        if (isAdded()) {
                            tvAccuracy.setText(String.format(java.util.Locale.US, "%.1f%%", averageAccuracy));
                            tvCompletedRounds.setText(String.valueOf(totalCompleted));
                        }
                        contentProfile.setVisibility(View.VISIBLE);
                        skeletonProfile.setVisibility(View.GONE);
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    });
                } else {
                    handler.post(() -> {
                        contentProfile.setVisibility(View.VISIBLE);
                        skeletonProfile.setVisibility(View.GONE);
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    });
                }
                dashConn.disconnect();
            } catch (Exception e) {
                handler.post(() -> {
                    contentProfile.setVisibility(View.VISIBLE);
                    skeletonProfile.setVisibility(View.GONE);
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                });
            }
        });
    }

    private void updateRankUI(int rating, String rank) {
        if (!isAdded()) return;
        String rankStr;
        int nextTarget;
        String nextRankStr;
        int badgeDrawableRes;

        if (rank != null && rank.equalsIgnoreCase("silver")) {
            rankStr = getString(R.string.rank_silver_solver);
            badgeDrawableRes = R.drawable.bg_badge_rank_silver;
            nextTarget = 1000;
            nextRankStr = "Gold Solver";
        } else {
            rankStr = getString(R.string.rank_bronze_mind);
            badgeDrawableRes = R.drawable.bg_badge_rank_bronze;
            nextTarget = 500;
            nextRankStr = getString(R.string.rank_silver_solver);
        }

        tvBadgeRank1.setText(rankStr);
        tvBadgeRank1.setBackgroundResource(badgeDrawableRes);
        tvBadgeRank2.setText(rankStr);
        tvBadgeRank2.setBackgroundResource(badgeDrawableRes);

        int percent = nextTarget > 0 ? (rating * 100) / nextTarget : 0;
        tvRatingRemaining.setText(percent + "%");

        pbRatingProgress.setMax(nextTarget);
        pbRatingProgress.setProgress(rating);

        tvRatingToward.setText("Toward " + nextRankStr);
    }

    private void updateSaveChangesButtonState() {
        if (!isAdded()) return;
        String currentName = etDisplayName.getText().toString().trim();
        boolean hasChanges = !currentName.equals(originalDisplayName) && !currentName.isEmpty();
        
        btnSaveChanges.setEnabled(hasChanges);
        if (hasChanges) {
            btnSaveChanges.setBackgroundResource(R.drawable.bg_button_primary);
            btnSaveChanges.setTextColor(getResources().getColor(R.color.color_text_primary));
            if (tvStatusNoChanges != null) {
                tvStatusNoChanges.setVisibility(View.GONE);
            }
        } else {
            btnSaveChanges.setBackgroundResource(R.drawable.bg_button_disabled);
            btnSaveChanges.setTextColor(getResources().getColor(R.color.color_text_secondary));
            if (tvStatusNoChanges != null) {
                tvStatusNoChanges.setVisibility(View.VISIBLE);
            }
        }
    }

    private void saveProfile() {
        String token = getToken();
        if (token == null) return;
        
        String newName = etDisplayName.getText().toString().trim();
        if (newName.isEmpty()) return;
        
        btnSaveChanges.setEnabled(false);
        btnSaveChanges.setText("SAVING...");
        
        executor.execute(() -> {
            try {
                URL url = new URL(BuildConfig.SUPABASE_URL + "/functions/v1/update-profile");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                
                JSONObject body = new JSONObject();
                body.put("displayName", newName);
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes());
                    os.flush();
                }
 
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    handler.post(() -> {
                        Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                        originalDisplayName = newName;
                        tvProfileName.setText(newName);
                        tvSummaryName.setText(newName);
                        btnSaveChanges.setText(R.string.profile_save_changes);
                        updateSaveChangesButtonState();
                    });
                } else {
                    InputStream es = conn.getErrorStream();
                    if (es != null) {
                        Scanner scanner = new Scanner(es).useDelimiter("\\A");
                        String response = scanner.hasNext() ? scanner.next() : "";
                        Log.e("ProfileFragment", "Update profile failed: " + response);
                    }
                    handler.post(() -> {
                        Toast.makeText(requireContext(), "Failed to update", Toast.LENGTH_SHORT).show();
                        btnSaveChanges.setText(R.string.profile_save_changes);
                        updateSaveChangesButtonState();
                    });
                }
            } catch (Exception e) {
                handler.post(() -> {
                    Toast.makeText(requireContext(), "Network Error", Toast.LENGTH_SHORT).show();
                    btnSaveChanges.setText(R.string.profile_save_changes);
                    updateSaveChangesButtonState();
                });
            }
        });
    }

    private void logout() {
        try {
            AuthSession.clear(requireContext());
        } catch (Exception e) {
            Log.e("ProfileFragment", "Error clearing token", e);
        }
        
        Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
