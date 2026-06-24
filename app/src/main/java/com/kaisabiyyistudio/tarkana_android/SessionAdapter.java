package com.kaisabiyyistudio.tarkana_android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kaisabiyyistudio.tarkana_android.model.SessionItem;

import java.util.List;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.ViewHolder> {

    private final List<SessionItem> items;

    public SessionAdapter(List<SessionItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SessionItem item = items.get(position);
        holder.tvName.setText(item.getName());
        holder.tvDate.setText(item.getDate());
        holder.tvPts.setText(item.getPoints() + " PTS");
        holder.tvPct.setText(item.getPercentage() + "%");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDate, tvPts, tvPct;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_session_name);
            tvDate = itemView.findViewById(R.id.tv_session_date);
            tvPts = itemView.findViewById(R.id.tv_session_pts);
            tvPct = itemView.findViewById(R.id.tv_session_pct);
        }
    }
}
