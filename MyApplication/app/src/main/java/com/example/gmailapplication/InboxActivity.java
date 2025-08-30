package com.example.gmailapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gmailapplication.adapters.EmailAdapter;
import com.example.gmailapplication.viewmodels.InboxViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.gmailapplication.shared.*;

public class InboxActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EmailAdapter adapter;
    private InboxViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);
        System.out.println("=== INBOX ACTIVITY STARTED ===");

        // Init views
        recyclerView = findViewById(R.id.recyclerViewEmails);
        FloatingActionButton fab = findViewById(R.id.fabCompose);

        // Init ViewModel
        viewModel = new ViewModelProvider(this).get(InboxViewModel.class);

        setupRecyclerView();
        setupObservers();
        setupFab(fab);
        setupHeader();

        // Load emails
        viewModel.loadEmails();
    }

    private void setupRecyclerView() {
        String currentUserEmail = TokenManager.getCurrentUserEmail(this);
        System.out.println("Current user email from token: " + currentUserEmail);
        adapter = new EmailAdapter(email -> {
            Toast.makeText(this, "נלחץ מייל: " + email.subject, Toast.LENGTH_SHORT).show();
        }, currentUserEmail != null ? currentUserEmail : "tomer@gmail.com");

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupObservers() {
        System.out.println("=== SETTING UP OBSERVERS ===");

        viewModel.getEmails().observe(this, emails -> {
            System.out.println("=== ADAPTER DEBUG ===");
            System.out.println("updateEmails called with emails: " + (emails != null ? emails.size() : "null"));

            if (emails != null && adapter != null) {
                adapter.updateEmails(emails);
                System.out.println("Final adapter size: " + adapter.getItemCount());
            }
            System.out.println("==================");
        });

        System.out.println("Observer setup complete");
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadEmails();
    }

    private void setupFab(FloatingActionButton fab) {
        fab.setOnClickListener(v -> {
            startActivity(new Intent(this, ComposeActivity.class));
        });
    }
    private void setupHeader() {
        ImageView ivLogout = findViewById(R.id.ivLogout);
        ImageView ivRefresh = findViewById(R.id.ivRefresh);
        ImageView ivSearch = findViewById(R.id.ivSearch);

        ivLogout.setOnClickListener(v -> showLogoutConfirmation());

        ivRefresh.setOnClickListener(v -> {
            viewModel.loadEmails();
            Toast.makeText(this, "מרענן...", Toast.LENGTH_SHORT).show();
        });

        // ivSearch - לעתיד
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("התנתקות")
                .setMessage("האם אתה בטוח שברצונך להתנתק?")
                .setPositiveButton("התנתק", (dialog, which) -> logout())
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void logout() {
        System.out.println("=== LOGOUT PROCESS STARTED ===");

        // נקה את הטוכן מכל המקומות
        TokenManager.clear(this);

        android.content.SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        prefs.edit().clear().apply();

        System.out.println("=== USER DATA CLEARED ===");

        // חזור למסך ההתחברות ונקה את כל ה-stack
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "התנתקת בהצלחה", Toast.LENGTH_SHORT).show();
        System.out.println("=== LOGOUT COMPLETED ===");
    }
}