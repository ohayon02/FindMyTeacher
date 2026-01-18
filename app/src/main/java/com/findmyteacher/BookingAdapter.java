package com.findmyteacher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder> {

    private List<LessonSlot> slots;
    private OnSlotClickListener listener;

    public interface OnSlotClickListener {
        void onSlotClick(LessonSlot slot);
    }

    public BookingAdapter(List<LessonSlot> slots, OnSlotClickListener listener) {
        this.slots = slots;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking_slot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LessonSlot slot = slots.get(position);
        holder.bind(slot, listener);
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSlotDate, tvSlotTime;
        private Button btnBookSlot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSlotDate = itemView.findViewById(R.id.tvSlotDate);
            tvSlotTime = itemView.findViewById(R.id.tvSlotTime);
            btnBookSlot = itemView.findViewById(R.id.btnBookSlot);
        }

        public void bind(final LessonSlot slot, final OnSlotClickListener listener) {
            // Format the date for display
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.ENGLISH);
            try {
                Date date = inputFormat.parse(slot.getDate());
                tvSlotDate.setText(outputFormat.format(date));
            } catch (ParseException e) {
                tvSlotDate.setText(slot.getDate()); // Fallback to raw date
            }

            tvSlotTime.setText(String.format("%s - %s", slot.getStartTime(), slot.getEndTime()));

            itemView.findViewById(R.id.btnBookSlot).setOnClickListener(v -> {
                listener.onSlotClick(slot);
            });
        }
    }
}
