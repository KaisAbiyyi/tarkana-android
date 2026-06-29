package com.kaisabiyyistudio.tarkana_android;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

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
        swipeRefresh.setOnChildScrollUpCallback((parent, child) ->
                contentProfile != null
                        && contentProfile.getVisibility() == View.VISIBLE
                        && contentProfile.canScrollVertically(-1));

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

        setEmptyProfileState();

        etDisplayName.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(android.text.Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSaveChangesButtonState();
            }
        });

        btnSaveChanges.setOnClickListener(v -> saveProfile());

        Button btnLogout = view.findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> logout());

        fetchProfile();
        return view;
    }

    private void setEmptyProfileState() {
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
    }

    private void fetchProfile() {
        if (!swipeRefresh.isRefreshing()) {
            contentProfile.setVisibility(View.GONE);
            skeletonProfile.setVisibility(View.VISIBLE);
        }

        executor.execute(() -> {
            try {
                android.content.Context context = getContext();
                if (context == null) return;
                ApiClient.ApiResponse profileResponse = ApiClient.getFunction(context, "/get-profile");
                profileResponse.requireSuccess();
                JSONObject profile = profileResponse.json();

                ApiClient.ApiResponse dashboardResponse = ApiClient.getFunction(context, "/get-dashboard");
                dashboardResponse.requireSuccess();
                JSONObject dashboard = dashboardResponse.json();

                handler.post(() -> renderProfile(profile, dashboard));
            } catch (ApiClient.AuthException e) {
                Log.e(TAG, "Auth error", e);
                handler.post(() -> showError(e.getMessage()));
            } catch (Exception e) {
                if (ApiClient.isCancellation(e)) return;
                Log.e(TAG, "Load error", e);
                handler.post(() -> showError("Failed to load profile."));
            }
        });
    }

    private void renderProfile(JSONObject profile, JSONObject dashboard) {
        if (!isAdded()) return;

        String displayName = profile.optString("displayName", "Unknown Player");
        String email = profile.optString("email", "unknown@email.com");
        int rating = profile.optInt("rating", 0);
        String rank = profile.optString("rank", "Bronze Mind");

        originalDisplayName = displayName;
        tvProfileName.setText(displayName);
        tvSummaryName.setText(displayName);
        etDisplayName.setText(displayName);
        tvEmail.setText(email);
        tvLogicRating.setText(String.valueOf(rating));
        updateRankUI(rating, rank);
        updateSaveChangesButtonState();

        double averageAccuracy = dashboard.optDouble("averageAccuracy", 0.0);
        int totalCompleted = dashboard.optInt("totalCompleted", 0);
        tvAccuracy.setText(String.format(java.util.Locale.US, "%.1f%%", averageAccuracy));
        tvCompletedRounds.setText(String.valueOf(totalCompleted));

        showContent();
    }

    private void updateRankUI(int rating, String rank) {
        if (!isAdded()) return;

        String normalizedRank = rank == null ? "" : rank;
        boolean isSilver = normalizedRank.equalsIgnoreCase("Silver Solver")
                || normalizedRank.equalsIgnoreCase("silver");
        String rankStr = isSilver
                ? getString(R.string.rank_silver_solver)
                : getString(R.string.rank_bronze_mind);
        int badgeDrawableRes = isSilver ? R.drawable.bg_badge_rank_silver : R.drawable.bg_badge_rank_bronze;
        int nextTarget = isSilver ? 1000 : 500;
        String nextRankStr = isSilver ? "Gold Solver" : getString(R.string.rank_silver_solver);

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
            if (tvStatusNoChanges != null) tvStatusNoChanges.setVisibility(View.GONE);
        } else {
            btnSaveChanges.setBackgroundResource(R.drawable.bg_button_disabled);
            btnSaveChanges.setTextColor(getResources().getColor(R.color.color_text_secondary));
            if (tvStatusNoChanges != null) tvStatusNoChanges.setVisibility(View.VISIBLE);
        }
    }

    private void saveProfile() {
        String newName = etDisplayName.getText().toString().trim();
        if (newName.isEmpty()) return;

        btnSaveChanges.setEnabled(false);
        btnSaveChanges.setText("SAVING...");

        executor.execute(() -> {
            try {
                android.content.Context context = getContext();
                if (context == null) return;
                JSONObject body = new JSONObject();
                body.put("displayName", newName);

                ApiClient.ApiResponse response = ApiClient.postFunction(context, "/update-profile", body);
                response.requireSuccess();

                handler.post(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                    originalDisplayName = newName;
                    tvProfileName.setText(newName);
                    tvSummaryName.setText(newName);
                    btnSaveChanges.setText(R.string.profile_save_changes);
                    updateSaveChangesButtonState();
                });
            } catch (ApiClient.AuthException e) {
                Log.e(TAG, "Auth error", e);
                handler.post(() -> showSaveError(e.getMessage()));
            } catch (Exception e) {
                if (ApiClient.isCancellation(e)) return;
                Log.e(TAG, "Save error", e);
                handler.post(() -> showSaveError("Failed to update profile."));
            }
        });
    }

    private void showSaveError(String message) {
        if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        btnSaveChanges.setText(R.string.profile_save_changes);
        updateSaveChangesButtonState();
    }

    private void logout() {
        AuthSession.clear(requireContext());
        Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showError(String message) {
        if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        showContent();
    }

    private void showContent() {
        contentProfile.setVisibility(View.VISIBLE);
        skeletonProfile.setVisibility(View.GONE);
        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
    }

    @Override
    public void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
