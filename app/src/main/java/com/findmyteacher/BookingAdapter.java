package com.findmyteacher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.DateViewHolder> {

    private final List<String> dateList;
    private final OnDateClickListener listener;

    public interface OnDateClickListener {
        void onDateClick(String date);
    }

    public BookingAdapter(List<String> dateList, OnDateClickListener listener) {
        this.dateList = dateList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking_date, parent, false);
        return new DateViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull DateViewHolder holder, int position) {
        holder.bind(dateList.get(position));
    }

    @Override
    public int getItemCount() {
        return dateList.size();
    }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDate;

        public DateViewHolder(@NonNull View itemView, final OnDateClickListener listener) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDateClick(tvDate.getText().toString());
                }
            });
        }

        public void bind(final String date) {
            tvDate.setText(date);
        }
    }
}
