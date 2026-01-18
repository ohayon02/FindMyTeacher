package com.findmyteacher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AvailabilityAdapter extends RecyclerView.Adapter<AvailabilityAdapter.ViewHolder> {

    private List<AvailabilityDate> dates;
    private OnAvailabilityChangedListener listener;

    public interface OnAvailabilityChangedListener {
        void onAvailabilityChanged(AvailabilityDate date, boolean isAvailable);
    }

    public AvailabilityAdapter(List<AvailabilityDate> dates, OnAvailabilityChangedListener listener) {
        this.dates = dates;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_availability_date, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AvailabilityDate availabilityDate = dates.get(position);
        holder.bind(availabilityDate, listener);
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDate;
        private SwitchMaterial switchAvailability;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            switchAvailability = itemView.findViewById(R.id.switchAvailability);
        }

        public void bind(final AvailabilityDate availabilityDate, final OnAvailabilityChangedListener listener) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
            tvDate.setText(sdf.format(availabilityDate.getDate()));
            switchAvailability.setChecked(availabilityDate.isAvailable());

            switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
                availabilityDate.setAvailable(isChecked);
                listener.onAvailabilityChanged(availabilityDate, isChecked);
            });
        }
    }
}
