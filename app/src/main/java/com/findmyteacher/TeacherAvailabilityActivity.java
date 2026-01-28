package com.findmyteacher;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class TeacherAvailabilityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_availability);

        Button btnExit = findViewById(R.id.btnExit);
        btnExit.setOnClickListener(v -> finish());
    }
}
