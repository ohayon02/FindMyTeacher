package com.findmyteacher;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.DateViewHolder> {

    public static class BookingDate {
        private final String date;
        private boolean isBooked;

        public BookingDate(String date, boolean isBooked) {
            this.date = date;
            this.isBooked = isBooked;
        }

        public String getDate() {
            return date;
        }

        public boolean isBooked() {
            return isBooked;
        }

        public void setBooked(boolean booked) {
            isBooked = booked;
        }
    }

    private final List<BookingDate> dateList;
    private final OnDateClickListener listener;

    public interface OnDateClickListener {
        void onDateClick(int position);
    }

    public BookingAdapter(List<BookingDate> dateList, OnDateClickListener listener) {
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
        BookingDate bookingDate = dateList.get(position);
        holder.bind(bookingDate);
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
                    listener.onDateClick(position);
                }
            });
        }

        public void bind(final BookingDate bookingDate) {
            tvDate.setText(bookingDate.getDate());

            if (bookingDate.isBooked()) {
                itemView.setBackgroundColor(Color.GREEN);
                itemView.setClickable(false);
            } else {
                itemView.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
                itemView.setClickable(true);
            }
        }
    }
}
