package com.kaisabiyyistudio.tarkana_android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kaisabiyyistudio.tarkana_android.model.LeaderboardEntry;

import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private final List<LeaderboardEntry> items;

    public LeaderboardAdapter(List<LeaderboardEntry> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardEntry entry = items.get(position);

        holder.tvPosition.setText(String.valueOf(entry.getPosition()));
        holder.tvPlayerName.setText(entry.getPlayerName());
        holder.tvLogicRating.setText(String.valueOf(entry.getLogicRating()));
        holder.tvAccuracy.setText(entry.getAccuracy());
        holder.tvRounds.setText(String.valueOf(entry.getCompletedRounds()));

        // Position background
        int posBg;
        switch (entry.getPosition()) {
            case 1: posBg = R.drawable.bg_leaderboard_position_gold; break;
            case 2: posBg = R.drawable.bg_leaderboard_position_silver; break;
            case 3: posBg = R.drawable.bg_leaderboard_position_bronze; break;
            default: posBg = R.drawable.bg_leaderboard_position_default; break;
        }
        holder.flPosition.setBackgroundResource(posBg);

        // Rank badge text, background & text color
        String rank = entry.getRank();
        if (rank != null) {
            String rankUpper = rank.toUpperCase();
            if (rankUpper.contains("SILVER")) {
                holder.tvRank.setText("SILVER SOLVER");
                holder.tvRank.setBackgroundResource(R.drawable.bg_badge_rank_silver);
                holder.tvRank.setTextColor(0xFF000000);
            } else if (rankUpper.contains("BRONZE")) {
                holder.tvRank.setText("BRONZE MIND");
                holder.tvRank.setBackgroundResource(R.drawable.bg_badge_rank_bronze);
                holder.tvRank.setTextColor(0xFFFFFFFF);
            } else {
                holder.tvRank.setText("UNRANKED");
                holder.tvRank.setBackgroundResource(R.drawable.bg_badge_yellow);
                holder.tvRank.setTextColor(0xFF000000);
            }
        } else {
            holder.tvRank.setText("UNRANKED");
            holder.tvRank.setBackgroundResource(R.drawable.bg_badge_yellow);
            holder.tvRank.setTextColor(0xFF000000);
        }

        // Current user highlight
        if (entry.isCurrentUser()) {
            holder.layoutRowRoot.setBackgroundResource(R.drawable.bg_card_yellow);
            holder.tvYouBadge.setVisibility(View.VISIBLE);
        } else {
            holder.layoutRowRoot.setBackgroundResource(R.drawable.bg_card_white);
            holder.tvYouBadge.setVisibility(View.GONE);
        }
        int padding = holder.itemView.getContext().getResources().getDimensionPixelSize(R.dimen.leaderboard_card_padding);
        if (entry.isCurrentUser()) {
            int extraBottomPadding = (int) (6 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
            holder.layoutRowRoot.setPadding(padding, padding, padding, padding + extraBottomPadding);
        } else {
            holder.layoutRowRoot.setPadding(padding, padding, padding, padding);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPosition, tvPlayerName, tvYouBadge, tvRank;
        TextView tvLogicRating, tvAccuracy, tvRounds;
        View layoutRowRoot;
        View flPosition;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPosition = itemView.findViewById(R.id.tv_lb_position);
            flPosition = itemView.findViewById(R.id.fl_position);
            tvPlayerName = itemView.findViewById(R.id.tv_lb_player_name);
            tvYouBadge = itemView.findViewById(R.id.tv_lb_you_badge);
            tvRank = itemView.findViewById(R.id.tv_lb_rank_badge);
            tvLogicRating = itemView.findViewById(R.id.tv_lb_rating);
            tvAccuracy = itemView.findViewById(R.id.tv_lb_accuracy);
            tvRounds = itemView.findViewById(R.id.tv_lb_rounds);
            layoutRowRoot = itemView.findViewById(R.id.leaderboard_row_root);
        }
    }
}
