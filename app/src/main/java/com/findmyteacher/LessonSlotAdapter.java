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
        holder.tvTime.setText(slot.getTime());
        
        if (slot.isAvailable()) {
            holder.tvStatus.setText("פנוי");
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
            holder.tvStudentName.setText("אין תלמיד רשום");
        } else {
            holder.tvStatus.setText("קבוע");
            holder.tvStatus.setTextColor(Color.parseColor("#F44336"));
            String displayName = slot.getStudentName();
            if (displayName == null || displayName.isEmpty()) {
                displayName = "טוען שם...";
            }
            holder.tvStudentName.setText("תלמיד: " + displayName);
        }
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    static class SlotViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView tvTime, tvStatus, tvStudentName;
        OnSlotClickListener onSlotClickListener;

        public SlotViewHolder(@NonNull View itemView, OnSlotClickListener onSlotClickListener) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            this.onSlotClickListener = onSlotClickListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onSlotClickListener.onSlotClick(getAdapterPosition());
        }
    }
}
