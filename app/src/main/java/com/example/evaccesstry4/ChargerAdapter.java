package com.example.evaccesstry4;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Locale;

public class ChargerAdapter extends RecyclerView.Adapter<ChargerAdapter.VH> {

    private final List<Charger> items;
    private final Context context;

    private double userLat;
    private double userLng;

    // Constructor without user location
    public ChargerAdapter(Context context, List<Charger> items) {
        this.context = context;
        this.items = items;
    }

    // Constructor with user location
    public ChargerAdapter(Context context, List<Charger> items, double userLat, double userLng) {
        this.context = context;
        this.items = items;
        this.userLat = userLat;
        this.userLng = userLng;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_charger, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {

        Charger c = items.get(position);

        // Set basic info
        holder.name.setText(c.getName());
        holder.price.setText(c.getPrice());

        // ⭐ Set rating
        holder.textRating.setText("⭐ " + String.format("%.1f", c.getRating()));

        // Show distance if available
        if (c.getDistance() >= 0) {
            holder.distance.setText(String.format(Locale.getDefault(), "%.2f km", c.getDistance()));
        } else {
            holder.distance.setText("N/A");
        }

        // Load image
        Glide.with(context)
                .load(c.getImageUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.thumbnail);

        // Click listener for detail page
        holder.itemView.setOnClickListener(v -> {
            Intent i = new Intent(context, ChargerDetailActivity.class);

            i.putExtra(ChargerDetailActivity.EXTRA_NAME, c.getName());
            i.putExtra(ChargerDetailActivity.EXTRA_DISTANCE,
                    String.format(Locale.getDefault(), "%.2f km", c.getDistance()));
            i.putExtra(ChargerDetailActivity.EXTRA_PRICE, c.getPrice());
            i.putExtra("extra_lat", c.getLat());
            i.putExtra("extra_lng", c.getLng());
            i.putExtra("extra_host_id", c.getHostId());
            i.putExtra("extra_id", c.getId());
            i.putExtra("extra_power", c.getChargerPower());


            context.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ==========================
    // ViewHolder class
    // ==========================
    static class VH extends RecyclerView.ViewHolder {

        TextView name, distance, price, textRating;
        ImageView thumbnail;

        public VH(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.charger_name);
            distance = itemView.findViewById(R.id.charger_distance);
            price = itemView.findViewById(R.id.charger_price);
            textRating = itemView.findViewById(R.id.charger_rating); // ⭐ ADDED
            thumbnail = itemView.findViewById(R.id.ivThumbnail);
        }
    }
}