package com.example.gmailapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvAppName, tvLoading;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        // Set app name and loading text
        tvAppName.setText("Gmail Application");
        tvLoading.setText("טוען...");
    }

    private void showSplashScreen() {
        // Show splash for 2 seconds, then check authentication
        handler.postDelayed(() -> {
            checkAuthenticationAndNavigate();
        }, 2000);
    }

    private void checkAuthenticationAndNavigate() {
        if (LoginActivity.isUserLoggedIn(this)) {
            // User is already logged in, go to inbox
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            String userId = prefs.getString("user_id", "");
            String userName = prefs.getString("user_name", "");
            String userEmail = prefs.getString("user_email", "");
            String authToken = prefs.getString("auth_token", "");

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
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}

// activity_main.xml layout suggestion:
/*
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@android:color/white"
    android:gravity="center">

    <LinearLayout
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:gravity="center">

        <ImageView
            android:layout_width="120dp"
            android:layout_height="120dp"
            android:src="@drawable/ic_email"
            android:layout_marginBottom="24dp" />

        <TextView
            android:id="@+id/tvAppName"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Gmail Application"
            android:textSize="24sp"
            android:textStyle="bold"
            android:textColor="@android:color/black"
            android:layout_marginBottom="16dp" />

        <TextView
            android:id="@+id/tvLoading"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="טוען..."
            android:textSize="16sp"
            android:textColor="@android:color/darker_gray" />

        <ProgressBar
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            style="?android:attr/progressBarStyle" />

    </LinearLayout>

</RelativeLayout>
*/