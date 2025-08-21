package com.example.gmailapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gmailapplication.adapters.EmailAdapter;
import com.example.gmailapplication.API.BackendClient;
import com.example.gmailapplication.API.EmailAPI;
import com.example.gmailapplication.shared.Email;
import com.example.gmailapplication.shared.EmailResponse;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InboxActivity extends AppCompatActivity {

    private RecyclerView recyclerViewEmails;
    private EmailAdapter emailAdapter;
    private List<Email> emailList = new ArrayList<>();
    private List<Email> allEmails = new ArrayList<>();

    private TextView tvWelcome, tvEmpty;
    private TabLayout tabLayout;
    private FloatingActionButton fabCompose;

    private EmailAPI emailAPI;
    private String authToken;
    private String currentFilter = "inbox"; // "inbox", "spam", "sent"

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        initViews();
        setupAPI();
        setupRecyclerView();
        setupTabs();
        setupFAB();

        loadEmails();
    }

    private void initViews() {
        recyclerViewEmails = findViewById(R.id.recyclerViewEmails);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvEmpty = findViewById(R.id.tvEmpty);
        tabLayout = findViewById(R.id.tabLayout);
        fabCompose = findViewById(R.id.fabCompose);

        // Get user data from intent or SharedPreferences
        String userName = getIntent().getStringExtra("user_name");
        if (userName == null) {
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            userName = prefs.getString("user_name", "משתמש");
        }

        tvWelcome.setText("שלום " + userName + "!");
    }

    private void setupAPI() {
        emailAPI = BackendClient.get(this).create(EmailAPI.class);

        // Get auth token
        authToken = getIntent().getStringExtra("auth_token");
        if (authToken == null) {
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            authToken = prefs.getString("auth_token", "");
        }

        if (authToken.isEmpty()) {
            Toast.makeText(this, "שגיאה: לא נמצא token", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Add Bearer prefix if needed
        if (!authToken.startsWith("Bearer ")) {
            authToken = "Bearer " + authToken;
        }
    }

    private void setupRecyclerView() {
        emailAdapter = new EmailAdapter(emailList, new EmailAdapter.OnEmailClickListener() {
            @Override
            public void onEmailClick(Email email) {
                openEmailDetail(email);
            }

            @Override
            public void onEmailLongClick(Email email) {
                showEmailActions(email);
            }
        });

        recyclerViewEmails.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewEmails.setAdapter(emailAdapter);
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("תיבת דואר"));
        tabLayout.addTab(tabLayout.newTab().setText("דואר זבל"));
        tabLayout.addTab(tabLayout.newTab().setText("נשלח"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        filterEmails("inbox");
                        break;
                    case 1:
                        filterEmails("spam");
                        break;
                    case 2:
                        filterEmails("sent");
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupFAB() {
        fabCompose.setOnClickListener(v -> {
            Intent intent = new Intent(InboxActivity.this, ComposeActivity.class);
            intent.putExtra("auth_token", authToken);
            startActivityForResult(intent, 100); // Request code for refresh
        });
    }

    private void loadEmails() {
        showLoading(true);

        emailAPI.getEmails().enqueue(new Callback<EmailResponse>() {
            @Override
            public void onResponse(Call<EmailResponse> call, Response<EmailResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    EmailResponse emailResponse = response.body();

                    allEmails.clear();

                    // Add recent emails (this includes both inbox and sent)
                    if (emailResponse.recent_mails != null) {
                        allEmails.addAll(emailResponse.recent_mails);
                    }

                    // Also add inbox and sent if not in recent
                    if (emailResponse.inbox != null) {
                        for (Email email : emailResponse.inbox) {
                            if (!allEmails.contains(email)) {
                                allEmails.add(email);
                            }
                        }
                    }

                    if (emailResponse.sent != null) {
                        for (Email email : emailResponse.sent) {
                            if (!allEmails.contains(email)) {
                                email.direction = "sent";
                                allEmails.add(email);
                            }
                        }
                    }

                    // Apply current filter
                    filterEmails(currentFilter);

                } else {
                    Toast.makeText(InboxActivity.this, "שגיאה בטעינת מיילים", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<EmailResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(InboxActivity.this, "שגיאת חיבור: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterEmails(String filter) {
        currentFilter = filter;
        emailList.clear();

        for (Email email : allEmails) {
            switch (filter) {
                case "inbox":
                    if (email.isInbox() && !"sent".equals(email.direction)) {
                        emailList.add(email);
                    }
                    break;
                case "spam":
                    if (email.isSpam()) {
                        emailList.add(email);
                    }
                    break;
                case "sent":
                    if ("sent".equals(email.direction)) {
                        emailList.add(email);
                    }
                    break;
            }
        }

        emailAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (emailList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerViewEmails.setVisibility(View.GONE);

            switch (currentFilter) {
                case "inbox":
                    tvEmpty.setText("אין מיילים בתיבת הדואר");
                    break;
                case "spam":
                    tvEmpty.setText("אין מיילים בדואר הזבל");
                    break;
                case "sent":
                    tvEmpty.setText("לא נשלחו מיילים עדיין");
                    break;
            }
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerViewEmails.setVisibility(View.VISIBLE);
        }
    }

    private void openEmailDetail(Email email) {
        Intent intent = new Intent(this, EmailDetailActivity.class);
        intent.putExtra("email_id", email.id);
        intent.putExtra("auth_token", authToken);
        startActivity(intent);
    }

    private void showEmailActions(Email email) {
        // TODO: Show bottom sheet or dialog with actions:
        // Mark as spam/not spam, delete, etc.
        Toast.makeText(this, "הפעולות יתווספו בקרוב", Toast.LENGTH_SHORT).show();
    }

    private void showLoading(boolean show) {
        // TODO: Add loading indicator
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // Refresh emails after sending
            loadEmails();
        }
    }
}