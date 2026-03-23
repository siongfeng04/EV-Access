package com.example.evaccesstry4;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class HostServiceAdapter extends RecyclerView.Adapter<HostServiceAdapter.ViewHolder> {

    private Context context;
    private List<Charger> services;
    private OnServiceActionListener listener;

    // Interface to handle edit/delete actions
    public interface OnServiceActionListener {
        void onEdit(Charger service);
        void onDelete(Charger service);
    }

    public HostServiceAdapter(Context context, List<Charger> services, OnServiceActionListener listener) {
        this.context = context;
        this.services = services;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_host_service, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Charger service = services.get(position);

        holder.name.setText(service.getName());
        holder.price.setText(service.getPrice());

        // Show distance safely
        if (service.getDistance() >= 0) {
            holder.distance.setText(
                    String.format(Locale.getDefault(), "%.2f km", service.getDistance())
            );
        } else {
            holder.distance.setText("N/A");
        }

    }

    @Override
    public int getItemCount() {
        return services.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView name, price, distance;
        ImageButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.text_service_name);
            price = itemView.findViewById(R.id.text_service_price);
            distance = itemView.findViewById(R.id.text_service_distance);

            //btnEdit = itemView.findViewById(R.id.btn_edit);
            //btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }

    // Update list dynamically
    public void updateServices(List<Charger> newServices) {
        services.clear();
        services.addAll(newServices);
        notifyDataSetChanged();
    }
}