package com.example.gmailapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TextView tvAppName, tvLoading;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MainActivity started");

        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
        }

        bindViews();
        showSplashScreen();
    }

    private void bindViews() {
        tvAppName = findViewById(R.id.tvAppName);
        tvLoading = findViewById(R.id.tvLoading);

        // בדיקה אם הviews נמצאו
        if (tvAppName != null) {
            tvAppName.setText("Gmail Application");
        } else {
            Log.w(TAG, "tvAppName not found in layout");
        }

        if (tvLoading != null) {
            tvLoading.setText("טוען...");
        } else {
            Log.w(TAG, "tvLoading not found in layout");
        }
    }

    private void showSplashScreen() {
        Log.d(TAG, "Starting splash screen");
        // Show splash for 2 seconds, then check authentication
        handler.postDelayed(() -> {
            Log.d(TAG, "Splash screen finished, checking authentication");
            checkAuthenticationAndNavigate();
        }, 2000);
    }

    private void checkAuthenticationAndNavigate() {
        try {
            boolean isLoggedIn = LoginActivity.isUserLoggedIn(this);
            Log.d(TAG, "User logged in: " + isLoggedIn);

            if (isLoggedIn) {
                // User is already logged in, go to inbox
                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                String userId = prefs.getString("user_id", "");
                String userName = prefs.getString("user_name", "");
                String userEmail = prefs.getString("user_email", "");
                String authToken = prefs.getString("auth_token", "");

                Log.d(TAG, "Navigating to InboxActivity for user: " + userEmail);

                Intent intent = new Intent(MainActivity.this, InboxActivity.class);
                intent.putExtra("user_id", userId);
                intent.putExtra("user_name", userName);
                intent.putExtra("user_email", userEmail);
                intent.putExtra("auth_token", authToken);
                intent.putExtra("auto_login", true);

                startActivity(intent);
                finish();
            } else {
                // User not logged in, go to login
                Log.d(TAG, "Navigating to LoginActivity");
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in checkAuthenticationAndNavigate", e);
            // Fallback - מעבר ל-LoginActivity במקרה של שגיאה
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "MainActivity destroyed");
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}