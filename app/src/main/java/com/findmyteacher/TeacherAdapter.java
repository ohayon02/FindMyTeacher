package com.findmyteacher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Optional;

public class TeacherAdapter extends RecyclerView.Adapter<TeacherAdapter.TeacherViewHolder> {

    private final List<Teacher> teacherList;
    private final OnTeacherClickListener listener;

    public interface OnTeacherClickListener {
        void onTeacherClick(Teacher teacher, boolean isChat);
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
        holder.bind(teacherList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return teacherList.size();
    }

    static class TeacherViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName, tvSubjects, tvPrice, tvLocation, tvBio;
        final Button btnChat;

        public TeacherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTeacherName);
            tvSubjects = itemView.findViewById(R.id.tvTeacherSubjects);
            tvPrice = itemView.findViewById(R.id.tvTeacherPrice);
            tvLocation = itemView.findViewById(R.id.tvTeacherLocation);
            tvBio = itemView.findViewById(R.id.tvTeacherBio);
            btnChat = itemView.findViewById(R.id.btnChatWithTeacher);
        }

        public void bind(final Teacher teacher, final OnTeacherClickListener listener) {
            tvName.setText(teacher.getFullName());
            tvSubjects.setText("מקצועות: " + teacher.getSubjectsString());

            String priceText = Optional.ofNullable(teacher.getHourlyPrice())
                    .filter(p -> !p.isEmpty()).map(p -> p + " ₪").orElse("לא צוין");
            tvPrice.setText("מחיר לשיעור: " + priceText);

            String locationText = Optional.ofNullable(teacher.getLocation())
                    .filter(l -> !l.isEmpty()).orElse("לא צוין");
            tvLocation.setText("מיקום: " + locationText);

            tvBio.setText(Optional.ofNullable(teacher.getBio()).filter(b -> !b.isEmpty()).orElse("אין ביוגרפיה זמינה"));

            itemView.setOnClickListener(v -> listener.onTeacherClick(teacher, false));
            btnChat.setOnClickListener(v -> listener.onTeacherClick(teacher, true));
        }
    }
}
