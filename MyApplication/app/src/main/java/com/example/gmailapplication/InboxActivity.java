package com.example.gmailapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class InboxActivity extends AppCompatActivity {

    private TextView tvWelcome, tvUserEmail;
    private Button btnLogout;

    private String userId, userName, userEmail, authToken;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        getUserDataFromIntent();
        bindViews();
        setupUI();
        wireLogout();
    }

    private void getUserDataFromIntent() {
        Intent intent = getIntent();
        userId = intent.getStringExtra("user_id");
        userName = intent.getStringExtra("user_name");
        userEmail = intent.getStringExtra("user_email");
        authToken = intent.getStringExtra("auth_token");
        boolean autoLogin = intent.getBooleanExtra("auto_login", false);

        if (autoLogin) {
            showToast("התחברת אוטומטית");
        }
    }

    private void bindViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupUI() {
        tvWelcome.setText("ברוך הבא, " + userName + "!");
        tvUserEmail.setText("מחובר כ: " + userEmail);
    }

    private void wireLogout() {
        btnLogout.setOnClickListener(v -> {
            // Clear login data
            LoginActivity.clearLoginData(this);

            // Go back to login
            Intent intent = new Intent(InboxActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void showToast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }
}