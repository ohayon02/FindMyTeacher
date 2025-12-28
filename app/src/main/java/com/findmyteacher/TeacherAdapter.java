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
        holder.tvSubjects.setText("מקצועות: " + teacher.getSubjectsString());
        
        // הצגת פרטים נוספים אם קיימים
        String price = teacher.getHourlyPrice();
        holder.tvPrice.setText("מחיר לשיעור: " + (price != null && !price.isEmpty() ? price + " ₪" : "לא צוין"));
        
        String location = teacher.getLocation();
        holder.tvLocation.setText("מיקום: " + (location != null && !location.isEmpty() ? location : "לא צוין"));
        
        String bio = teacher.getBio();
        holder.tvBio.setText(bio != null && !bio.isEmpty() ? bio : "אין ביוגרפיה זמינה");

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
        TextView tvName, tvSubjects, tvPrice, tvLocation, tvBio;
        Button btnChat;

        public TeacherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTeacherName);
            tvSubjects = itemView.findViewById(R.id.tvTeacherSubjects);
            tvPrice = itemView.findViewById(R.id.tvTeacherPrice);
            tvLocation = itemView.findViewById(R.id.tvTeacherLocation);
            tvBio = itemView.findViewById(R.id.tvTeacherBio);
            btnChat = itemView.findViewById(R.id.btnChatWithTeacher);
        }
    }
}
