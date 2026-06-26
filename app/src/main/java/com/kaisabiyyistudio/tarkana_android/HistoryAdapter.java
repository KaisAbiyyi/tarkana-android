package com.kaisabiyyistudio.tarkana_android;

import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kaisabiyyistudio.tarkana_android.model.HistoryItem;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<HistoryItem> items;

    public HistoryAdapter(List<HistoryItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_session, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = items.get(position);

        holder.tvMode.setText(item.getMode());
        holder.tvSessionInfo.setText(item.getSessionType() + " · " + item.getQuestionCount() + " Questions");
        holder.tvStatus.setText(item.getStatus());
        holder.tvDate.setText(item.getDate());
        holder.tvScoreValue.setText(String.valueOf(item.getScore()));
        holder.tvAccuracyValue.setText(item.getAccuracy() + "%");
        holder.tvAvgValue.setText(item.getAvgTime() + "s");

        // Rating
        String ratingText;
        if (item.getRatingBefore() > 0 || item.getRatingAfter() > 0) {
            ratingText = item.getRatingBefore() + " → " + item.getRatingAfter()
                    + " (+" + item.getRatingChange() + ")";
        } else {
            ratingText = (item.getRatingChange() >= 0 ? "+" : "") + item.getRatingChange();
        }
        holder.tvRatingValue.setText(ratingText);

        // Badges
        holder.layoutBadges.removeAllViews();
        List<String> badges = item.getBadges();
        if (badges != null && !badges.isEmpty()) {
            holder.layoutBadges.setVisibility(View.VISIBLE);
            for (String badge : badges) {
                TextView chip = new TextView(holder.itemView.getContext());
                chip.setText(badge);
                chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
                chip.setTypeface(null, Typeface.BOLD);
                chip.setTextColor(0xFFFFFFFF);
                chip.setBackgroundResource(R.drawable.bg_chip_green);
                int hPad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                        holder.itemView.getResources().getDisplayMetrics());
                int vPad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4,
                        holder.itemView.getResources().getDisplayMetrics());
                chip.setPadding(hPad, vPad, hPad, vPad);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4,
                        holder.itemView.getResources().getDisplayMetrics());
                params.setMargins(0, 0, margin, margin);
                chip.setLayoutParams(params);

                holder.layoutBadges.addView(chip);
            }
        } else {
            holder.layoutBadges.setVisibility(View.GONE);
        }

        // View Result button
        holder.btnViewResult.setOnClickListener(v ->
                Toast.makeText(v.getContext(), "Viewing result...", Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMode, tvSessionInfo, tvStatus, tvDate;
        TextView tvScoreValue, tvAccuracyValue, tvAvgValue, tvRatingValue;
        LinearLayout layoutBadges;
        View btnViewResult;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMode = itemView.findViewById(R.id.tv_hist_mode);
            tvSessionInfo = itemView.findViewById(R.id.tv_hist_session_type);
            tvStatus = itemView.findViewById(R.id.tv_hist_status);
            tvDate = itemView.findViewById(R.id.tv_hist_date);
            tvScoreValue = itemView.findViewById(R.id.tv_hist_score);
            tvAccuracyValue = itemView.findViewById(R.id.tv_hist_item_accuracy);
            tvAvgValue = itemView.findViewById(R.id.tv_hist_avg_time);
            tvRatingValue = itemView.findViewById(R.id.tv_hist_rating);
            layoutBadges = itemView.findViewById(R.id.layout_badges);
            btnViewResult = itemView.findViewById(R.id.btn_view_result);
        }
    }
}
