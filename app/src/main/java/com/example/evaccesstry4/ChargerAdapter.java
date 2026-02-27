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

import java.util.List;

public class ChargerAdapter extends RecyclerView.Adapter<ChargerAdapter.VH> {

    private final List<Charger> items;
    private final Context context;

    public ChargerAdapter(Context context, List<Charger> items) {
        this.context = context;
        this.items = items;
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
        holder.name.setText(c.name);
        holder.distance.setText(c.distance);
        holder.price.setText(c.price);
        holder.thumbnail.setImageResource(android.R.drawable.ic_menu_compass);
        holder.itemView.setOnClickListener(v -> {
            Intent i = new Intent(context, ChargerDetailActivity.class);
            i.putExtra(ChargerDetailActivity.EXTRA_NAME, c.name);
            i.putExtra(ChargerDetailActivity.EXTRA_DISTANCE, c.distance);
            i.putExtra(ChargerDetailActivity.EXTRA_PRICE, c.price);
            i.putExtra("extra_lat", c.lat);
            i.putExtra("extra_lng", c.lng);
            context.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, distance, price;
        ImageView thumbnail;

        public VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.charger_name);
            distance = itemView.findViewById(R.id.charger_distance);
            price = itemView.findViewById(R.id.charger_price);
            thumbnail = itemView.findViewById(R.id.ivThumbnail);
        }
    }
}
