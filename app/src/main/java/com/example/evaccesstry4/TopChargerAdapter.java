package com.example.evaccesstry4;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class TopChargerAdapter extends RecyclerView.Adapter<TopChargerAdapter.ViewHolder> {

    private List<TopCharger> list;

    public TopChargerAdapter(List<TopCharger> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_top_charger, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        TopCharger item = list.get(position);

        // Rank (Top 1, 2, 3...)
        holder.textRank.setText("#" + (position + 1));

        // Charger name
        holder.textName.setText(item.getName());

        // Bookings
        holder.textBookings.setText("Bookings: " + item.getTotalBookings());

        // Revenue
        holder.textRevenue.setText(
                "RM " + String.format(Locale.getDefault(), "%.2f", item.getTotalRevenue())
        );
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // =========================
    // VIEW HOLDER
    // =========================
    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView textRank, textName, textBookings, textRevenue;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            textRank = itemView.findViewById(R.id.text_rank);
            textName = itemView.findViewById(R.id.text_name);
            textBookings = itemView.findViewById(R.id.text_bookings);
            textRevenue = itemView.findViewById(R.id.text_revenue);
        }
    }
}