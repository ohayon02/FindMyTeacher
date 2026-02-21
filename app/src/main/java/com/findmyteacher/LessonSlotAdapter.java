package com.findmyteacher;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class LessonSlotAdapter extends RecyclerView.Adapter<LessonSlotAdapter.SlotViewHolder> {

    private final List<LessonSlot> slots;
    private final OnSlotClickListener listener;

    public interface OnSlotClickListener {
        void onSlotClick(int position);
    }

    public LessonSlotAdapter(List<LessonSlot> slots, OnSlotClickListener listener) {
        this.slots = slots;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lesson_slot, parent, false);
        return new SlotViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull SlotViewHolder holder, int position) {
        LessonSlot slot = slots.get(position);
        holder.tvDate.setText(slot.getDate());
        holder.tvTime.setText(slot.getTime());

        if (slot.isAvailable()) {
            holder.itemView.setBackgroundColor(Color.parseColor("#A5D6A7")); // Light Green
        } else {
            holder.itemView.setBackgroundColor(Color.parseColor("#EF9A9A")); // Light Red
        }
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    static class SlotViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView tvDate, tvTime;
        OnSlotClickListener onSlotClickListener;

        public SlotViewHolder(@NonNull View itemView, OnSlotClickListener onSlotClickListener) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            this.onSlotClickListener = onSlotClickListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onSlotClickListener.onSlotClick(getAdapterPosition());
        }
    }
}
