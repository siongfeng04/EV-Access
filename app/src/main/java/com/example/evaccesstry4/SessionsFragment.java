package com.example.evaccesstry4;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

public class SessionsFragment extends Fragment {

    private TextView textView;
    private UserViewModel userViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_sessions, container, false);
        textView = view.findViewById(R.id.text_sessions);

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        userViewModel.getRole().observe(getViewLifecycleOwner(), role -> {
            if (role == null) return;

            if ("host".equals(role)) loadHostSessions();
            else loadUserSessions();
        });

        return view;
    }

    private void loadHostSessions() {
        textView.setText("Host Sessions: Active charging bookings");
    }

    private void loadUserSessions() {
        textView.setText("User Sessions: Your upcoming bookings");
    }
}