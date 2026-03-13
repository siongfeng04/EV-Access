package com.example.evaccesstry4;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder> {

    private final List<Booking> bookingList;
    private OnItemClickListener listener;
    private String currentRole = "user"; // default role

    public interface OnItemClickListener {
        void onClick(Booking booking);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setRole(String role) {
        this.currentRole = role;
    }

    public BookingAdapter(List<Booking> bookingList) {
        this.bookingList = bookingList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking booking = bookingList.get(position);

        // Set basic info
        holder.chargerName.setText(booking.getChargerName());
        holder.startTime.setText("Start Time: " + booking.getStartTime());
        holder.duration.setText("Duration: " + booking.getDuration() + " hour(s)");
        holder.status.setText("Status: " + booking.getStatus());

        // Show estimated or actual cost
        double displayCost = "BOOKED".equalsIgnoreCase(booking.getStatus())
                ? booking.getEstimatedCost()
                : booking.getTotalCost();
        String costLabel = "BOOKED".equalsIgnoreCase(booking.getStatus())
                ? "Estimated Cost: RM "
                : "Total Cost: RM ";
        holder.totalCost.setText(String.format(Locale.getDefault(), "%s%.2f", costLabel, displayCost));

        // Adjust appearance for COMPLETED sessions
        boolean isCompleted = "COMPLETED".equalsIgnoreCase(booking.getStatus());
        float alpha = isCompleted ? 0.5f : 1f;
        int textColor = isCompleted ? holder.itemView.getResources().getColor(android.R.color.darker_gray)
                : holder.itemView.getResources().getColor(android.R.color.black);
        holder.itemView.setAlpha(alpha);
        holder.chargerName.setTextColor(textColor);
        holder.startTime.setTextColor(textColor);
        holder.duration.setTextColor(textColor);
        holder.totalCost.setTextColor(textColor);
        holder.status.setTextColor(isCompleted
                ? textColor
                : holder.itemView.getResources().getColor(android.R.color.holo_green_dark));

        // Handle Start Session button visibility
        if (!isCompleted && "user".equalsIgnoreCase(currentRole)) {
            holder.btnStartSession.setVisibility(View.VISIBLE);
            holder.btnStartSession.setOnClickListener(v -> {
                if (listener != null) listener.onClick(booking);
            });
        } else {
            holder.btnStartSession.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView chargerName, startTime, duration, totalCost, status;
        Button btnStartSession;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            chargerName = itemView.findViewById(R.id.textChargerName);
            startTime = itemView.findViewById(R.id.textStartTime);
            duration = itemView.findViewById(R.id.textDuration);
            totalCost = itemView.findViewById(R.id.textCost);
            status = itemView.findViewById(R.id.textStatus);
            btnStartSession = itemView.findViewById(R.id.btnStartSession);
        }
    }
}