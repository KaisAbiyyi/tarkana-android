package com.kaisabiyyistudio.tarkana_android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class LeaderboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);

        RecyclerView rv = view.findViewById(R.id.rv_leaderboard);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new LeaderboardAdapter(DummyData.getLeaderboard()));

        return view;
    }
}
