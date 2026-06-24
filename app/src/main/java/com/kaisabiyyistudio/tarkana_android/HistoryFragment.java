package com.kaisabiyyistudio.tarkana_android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class HistoryFragment extends Fragment {

    private View lastSelectedChip;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        // RecyclerView
        RecyclerView rv = view.findViewById(R.id.rv_history_sessions);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new HistoryAdapter(DummyData.getHistorySessions()));

        // Filter chips
        int[] chipIds = {R.id.chip_all, R.id.chip_mixed, R.id.chip_number,
                R.id.chip_symbol, R.id.chip_deduction, R.id.chip_memory};

        View chipAll = view.findViewById(R.id.chip_all);
        chipAll.setBackgroundResource(R.drawable.bg_chip_selected);
        lastSelectedChip = chipAll;

        for (int chipId : chipIds) {
            View chip = view.findViewById(chipId);
            chip.setOnClickListener(v -> {
                // Reset previous
                if (lastSelectedChip != null) {
                    lastSelectedChip.setBackgroundResource(R.drawable.bg_chip_outline);
                }
                // Select current
                v.setBackgroundResource(R.drawable.bg_chip_selected);
                lastSelectedChip = v;
            });
        }

        // Play next button
        View btnPlayNext = view.findViewById(R.id.btn_play_next);
        if (btnPlayNext != null) {
            btnPlayNext.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Starting next round...", Toast.LENGTH_SHORT).show());
        }

        return view;
    }
}
