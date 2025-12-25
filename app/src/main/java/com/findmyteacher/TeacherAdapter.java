package com.findmyteacher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TeacherAdapter extends RecyclerView.Adapter<TeacherAdapter.TeacherViewHolder> {

    private List<Teacher> teacherList;
    private OnTeacherClickListener listener;

    public interface OnTeacherClickListener {
        void onChatClick(Teacher teacher);
    }

    public TeacherAdapter(List<Teacher> teacherList, OnTeacherClickListener listener) {
        this.teacherList = teacherList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TeacherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_teacher, parent, false);
        return new TeacherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeacherViewHolder holder, int position) {
        Teacher teacher = teacherList.get(position);
        holder.tvName.setText(teacher.getFullName());
        holder.tvSubjects.setText(teacher.getSubjectsString());
        holder.btnChat.setOnClickListener(v -> listener.onChatClick(teacher));
    }

    @Override
    public int getItemCount() {
        return teacherList.size();
    }

    public void updateList(List<Teacher> newList) {
        this.teacherList = newList;
        notifyDataSetChanged();
    }

    static class TeacherViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSubjects;
        Button btnChat;

        public TeacherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTeacherName);
            tvSubjects = itemView.findViewById(R.id.tvTeacherSubjects);
            btnChat = itemView.findViewById(R.id.btnChatWithTeacher);
        }
    }
}
