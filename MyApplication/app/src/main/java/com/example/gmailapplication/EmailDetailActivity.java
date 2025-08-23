package com.example.gmailapplication;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.gmailapplication.API.BackendClient;
import com.example.gmailapplication.API.EmailAPI;
import com.example.gmailapplication.shared.Email;
import com.example.gmailapplication.shared.Label;
import com.example.gmailapplication.shared.UpdateLabelsRequest;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmailDetailActivity extends AppCompatActivity {

    private TextView tvSender, tvRecipients, tvSubject, tvTimestamp, tvContent;
    private ImageView ivSpamIndicator;
    private Toolbar toolbar;

    private EmailAPI emailAPI;
    private String authToken;
    private String emailId;
    private Email currentEmail;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_detail);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
        }

        initViews();
        setupToolbar();
        setupAPI();
        loadEmail();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvSender = findViewById(R.id.tvSender);
        tvRecipients = findViewById(R.id.tvRecipients);
        tvSubject = findViewById(R.id.tvSubject);
        tvTimestamp = findViewById(R.id.tvTimestamp);
        tvContent = findViewById(R.id.tvContent);
        ivSpamIndicator = findViewById(R.id.ivSpamIndicator);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("פרטי מייל");
        }
    }

    private void setupAPI() {
        emailAPI = BackendClient.get(this).create(EmailAPI.class);

        // Get data from intent
        emailId = getIntent().getStringExtra("email_id");
        authToken = getIntent().getStringExtra("auth_token");

        if (emailId == null || authToken == null) {
            Toast.makeText(this, "שגיאה: חסרים נתונים", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Add Bearer prefix if needed
        if (!authToken.startsWith("Bearer ")) {
            authToken = "Bearer " + authToken;
        }
    }

// החלף את הפונקציה loadEmail() ב-EmailDetailActivity.java:

    private void loadEmail() {
        emailAPI.getEmailById(emailId).enqueue(new Callback<Email>() {
            @Override
            public void onResponse(Call<Email> call, Response<Email> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentEmail = response.body();
                    displayEmail(currentEmail);
                } else {
                    // *** תיקון: אם ה-API נכשל, השתמש ב-fallback data מה-Intent ***
                    Toast.makeText(EmailDetailActivity.this, "טוען מידע מקומי...", Toast.LENGTH_SHORT).show();
                    loadEmailFromIntent();
                }
            }

            @Override
            public void onFailure(Call<Email> call, Throwable t) {
                // *** תיקון: במקום לסגור, נסה fallback data ***
                Toast.makeText(EmailDetailActivity.this, "טוען מידע מקומי...", Toast.LENGTH_SHORT).show();
                loadEmailFromIntent();
            }
        });
    }

    // *** פונקציה חדשה: טעינה מ-Intent כשה-API נכשל ***
    private void loadEmailFromIntent() {
        // יצירת Email object מהנתונים ב-Intent
        currentEmail = new Email();
        currentEmail.id = emailId;
        currentEmail.subject = getIntent().getStringExtra("subject");
        currentEmail.sender = getIntent().getStringExtra("sender");
        currentEmail.content = getIntent().getStringExtra("content");
        currentEmail.timestamp = getIntent().getStringExtra("timestamp");

        if (currentEmail.subject != null || currentEmail.sender != null) {
            displayEmail(currentEmail);
        } else {
            Toast.makeText(this, "שגיאה: לא ניתן לטעון את המייל", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void displayEmail(Email email) {
        // Sender
        if (email.sender != null) {
            tvSender.setText("מאת: " + email.sender);
        }

        // Recipients
        if (email.recipients != null && !email.recipients.isEmpty()) {
            String recipients = "";
            if (email.recipients != null && !email.recipients.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < email.recipients.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(email.recipients.get(i));
                }
                recipients = sb.toString();
            }
            tvRecipients.setText("אל: " + recipients);
        } else if (email.recipient != null) {
            tvRecipients.setText("אל: " + email.recipient);
        }

        // Subject
        String subject = email.subject;
        if (subject == null || subject.trim().isEmpty()) {
            subject = "(ללא נושא)";
        }
        tvSubject.setText(subject);

        // Timestamp
        tvTimestamp.setText(formatTimestamp(email.timestamp));

        // Content
        String content = email.content;
        if (content == null || content.trim().isEmpty()) {
            content = "(ללא תוכן)";
        }
        tvContent.setText(content);

        // Spam indicator
        if (email.isSpam()) {
            ivSpamIndicator.setVisibility(android.view.View.VISIBLE);
        } else {
            ivSpamIndicator.setVisibility(android.view.View.GONE);
        }

        // Update toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(subject);
        }
    }

    private String formatTimestamp(String timestamp) {
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            Date date = isoFormat.parse(timestamp);

            SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US);
            return displayFormat.format(date);
        } catch (Exception e) {
            return timestamp != null ? timestamp : "לא ידוע";
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.email_detail_menu, menu);

        // Update spam/not spam menu item based on current state
        MenuItem spamItem = menu.findItem(R.id.action_toggle_spam);
        if (currentEmail != null) {
            if (currentEmail.isSpam()) {
                spamItem.setTitle("סמן כלא זבל");
                spamItem.setIcon(android.R.drawable.ic_dialog_info);
            } else {
                spamItem.setTitle("סמן כזבל");
                spamItem.setIcon(android.R.drawable.ic_dialog_alert);
            }
        }

        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_toggle_spam) {
            toggleSpam();
            return true;
        } else if (itemId == R.id.action_delete) {
            deleteEmail();
            return true;
        } else if (itemId == R.id.action_reply) {
            replyToEmail();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void toggleSpam() {
        if (currentEmail == null) return;

        boolean isCurrentlySpam = currentEmail.isSpam();
        String newLabel = isCurrentlySpam ? "inbox" : "spam";

        UpdateLabelsRequest request = new UpdateLabelsRequest(Arrays.asList(newLabel));

        emailAPI.updateEmailLabels(emailId, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Update local email object
                    currentEmail.labels = Arrays.asList(new Label(newLabel));

                    // Update UI
                    displayEmail(currentEmail);
                    invalidateOptionsMenu(); // Refresh menu

                    String message = isCurrentlySpam ? "המייל הועבר לתיבת הדואר" : "המייל הועבר לדואר הזבל";
                    Toast.makeText(EmailDetailActivity.this, message, Toast.LENGTH_SHORT).show();

                    // Set result to refresh inbox
                    setResult(RESULT_OK);
                } else {
                    Toast.makeText(EmailDetailActivity.this, "שגיאה בעדכון המייל", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(EmailDetailActivity.this, "שגיאת חיבור: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteEmail() {
        if (currentEmail == null) return;

        emailAPI.deleteEmail(emailId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EmailDetailActivity.this, "המייל נמחק בהצלחה", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(EmailDetailActivity.this, "שגיאה במחיקת המייל", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(EmailDetailActivity.this, "שגיאת חיבור: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void replyToEmail() {
        if (currentEmail == null) return;

        Intent intent = new Intent(this, ComposeActivity.class);
        intent.putExtra("auth_token", authToken);
        intent.putExtra("reply_to", currentEmail.sender);
        intent.putExtra("reply_subject", "Re: " + currentEmail.subject);
        intent.putExtra("reply_content", "\n\n--- הודעה מקורית ---\n" + currentEmail.content);
        startActivity(intent);
    }
}