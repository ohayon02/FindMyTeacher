package com.findmyteacher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Objects;

public class StudentAdapter extends ListAdapter<Student, StudentAdapter.StudentViewHolder> {

    private final OnStudentClickListener onStudentClickListener;

    public interface OnStudentClickListener {
        void onStudentClick(Student student);
    }

    public StudentAdapter(OnStudentClickListener onStudentClickListener) {
        super(DIFF_CALLBACK);
        this.onStudentClickListener = onStudentClickListener;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new StudentViewHolder(view, onStudentClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = getItem(position);
        holder.bind(student);
    }

    class StudentViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        public StudentViewHolder(@NonNull View itemView, OnStudentClickListener listener) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onStudentClick(getItem(position));
                }
            });
        }

        public void bind(final Student student) {
            textView.setText(student.getFullName());
        }
    }

    private static final DiffUtil.ItemCallback<Student> DIFF_CALLBACK = new DiffUtil.ItemCallback<Student>() {
        @Override
        public boolean areItemsTheSame(@NonNull Student oldItem, @NonNull Student newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Student oldItem, @NonNull Student newItem) {
            return Objects.equals(oldItem.getFullName(), newItem.getFullName());
        }
    };
}
