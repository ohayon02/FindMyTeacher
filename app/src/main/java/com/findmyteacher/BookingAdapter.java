package com.findmyteacher;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.DateViewHolder> {

    public static class BookingDate {
        private final String date;
        private boolean isBooked;
        private String bookedBy;

        public BookingDate(String date, boolean isBooked, String bookedBy) {
            this.date = date;
            this.isBooked = isBooked;
            this.bookedBy = bookedBy;
        }

        public String getDate() {
            return date;
        }

        public boolean isBooked() {
            return isBooked;
        }

        public String getBookedBy() {
            return bookedBy;
        }

        public void setBooked(boolean booked) {
            isBooked = booked;
        }

        public void setBookedBy(String bookedBy) {
            this.bookedBy = bookedBy;
        }
    }

    private final List<BookingDate> dateList;
    private final OnDateClickListener listener;
    private final String currentUserId;

    public interface OnDateClickListener {
        void onDateClick(int position);
    }

    public BookingAdapter(List<BookingDate> dateList, OnDateClickListener listener) {
        this.dateList = dateList;
        this.listener = listener;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
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
        holder.bind(bookingDate, currentUserId);
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

        public void bind(final BookingDate bookingDate, String currentUserId) {
            tvDate.setText(bookingDate.getDate());

            if (bookingDate.isBooked()) {
                if (currentUserId.equals(bookingDate.getBookedBy())) {
                    itemView.setBackgroundColor(Color.GREEN); // Booked by current user
                    itemView.setClickable(true);
                } else {
                    itemView.setBackgroundColor(Color.RED); // Booked by another user
                    itemView.setClickable(false);
                }
            } else {
                itemView.setBackgroundResource(android.R.drawable.dialog_holo_light_frame); // Available
                itemView.setClickable(true);
            }
        }
    }
}
