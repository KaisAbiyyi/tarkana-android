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

public class DashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        RecyclerView rv = view.findViewById(R.id.rv_recent_sessions);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        
        java.util.List<com.kaisabiyyistudio.tarkana_android.model.SessionItem> recentSessions = DummyData.getRecentSessions();
        if (recentSessions.size() > 3) {
            recentSessions = recentSessions.subList(0, 3);
        }
        rv.setAdapter(new SessionAdapter(recentSessions));

        // Start Challenge button navigation
        view.findViewById(R.id.btn_start_challenge).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                com.google.android.material.bottomnavigation.BottomNavigationView nav = 
                    getActivity().findViewById(R.id.bottom_nav);
                if (nav != null) {
                    nav.setSelectedItemId(R.id.nav_challenge);
                }
            }
        });

        // View Full History button navigation
        view.findViewById(R.id.tv_view_history).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                com.google.android.material.bottomnavigation.BottomNavigationView nav = 
                    getActivity().findViewById(R.id.bottom_nav);
                if (nav != null) {
                    nav.setSelectedItemId(R.id.nav_history);
                }
            }
        });

        return view;
    }
}
