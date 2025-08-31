package com.example.gmailapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.gmailapplication.API.BackendClient;
import com.example.gmailapplication.API.EmailAPI;
import com.example.gmailapplication.shared.Email;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmailDetailActivity extends AppCompatActivity {

    private static final int MANAGE_LABELS_REQUEST_CODE = 100;

    private TextView tvSender, tvSubject, tvContent, tvTimestamp, tvRecipients, tvLabels;
    private LinearLayout layoutLabels;
    private ImageView ivSpamIndicator;
    private Toolbar toolbar;

    private String emailId;
    private String sender;
    private String subject;
    private String content;
    private String timestamp;
    private List<String> currentLabels = new ArrayList<>();

    private EmailAPI emailAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_detail);

        System.out.println("=== EMAIL DETAIL ACTIVITY STARTED ===");

        initViews();
        setupToolbar();
        initAPI();
        loadEmailData();
        loadFullEmailFromServer(); // טען את המייל המלא כולל תוויות
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvSender = findViewById(R.id.tvSender);
        tvSubject = findViewById(R.id.tvSubject);
        tvContent = findViewById(R.id.tvContent);
        tvTimestamp = findViewById(R.id.tvTimestamp);
        tvRecipients = findViewById(R.id.tvRecipients);
        tvLabels = findViewById(R.id.tvLabels);
        layoutLabels = findViewById(R.id.layoutLabels);
        ivSpamIndicator = findViewById(R.id.ivSpamIndicator);

        Button btnReply = findViewById(R.id.btnReply);
        btnReply.setOnClickListener(v -> openReply());

        Button btnManageLabels = findViewById(R.id.btnManageLabels);
        btnManageLabels.setOnClickListener(v -> openManageLabels());
    }

    private void initAPI() {
        emailAPI = BackendClient.get(this).create(EmailAPI.class);
    }

    private void openReply() {
        Intent intent = new Intent(this, ComposeActivity.class);
        intent.putExtra("reply_to", sender);
        intent.putExtra("reply_subject", "Re: " + subject);
        intent.putExtra("reply_content", "\n\n--- תגובה למייל מקורי ---\n" + content);
        startActivity(intent);
    }

    private void openManageLabels() {
        if (emailId != null && !emailId.isEmpty()) {
            ManageLabelsActivity.start(this, emailId, currentLabels);
        } else {
            Toast.makeText(this, "לא ניתן לנהל תוויות - חסר מזהה מייל", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("פרטי המייל");
        }
    }

    private void loadEmailData() {
        // קבל את הנתונים מה-Intent (נתונים בסיסיים)
        emailId = getIntent().getStringExtra("email_id");
        sender = getIntent().getStringExtra("sender");
        subject = getIntent().getStringExtra("subject");
        content = getIntent().getStringExtra("content");
        timestamp = getIntent().getStringExtra("timestamp");

        System.out.println("=== LOADED EMAIL DATA ===");
        System.out.println("ID: " + emailId);
        System.out.println("Sender: " + sender);
        System.out.println("Subject: " + subject);
        System.out.println("Content length: " + (content != null ? content.length() : 0));
        System.out.println("========================");

        // הצג את הנתונים הבסיסיים מיד
        displayBasicEmailData();
    }

    private void loadFullEmailFromServer() {
        if (emailId == null || emailId.isEmpty()) {
            System.out.println("No email ID - cannot load full email data");
            return;
        }

        emailAPI.getEmailById(emailId).enqueue(new Callback<Email>() {
            @Override
            public void onResponse(Call<Email> call, Response<Email> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Email fullEmail = response.body();
                    currentLabels = fullEmail.labels != null ? fullEmail.labels : new ArrayList<>();

                    System.out.println("=== LOADED FULL EMAIL DATA ===");
                    System.out.println("Labels: " + currentLabels);
                    System.out.println("==============================");

                    // עדכן את התצוגה עם הנתונים המלאים
                    displayFullEmailData(fullEmail);
                } else {
                    System.err.println("Failed to load full email data: " + response.code());
                    // המשך עם הנתונים הבסיסיים שכבר יש
                }
            }

            @Override
            public void onFailure(Call<Email> call, Throwable t) {
                System.err.println("Error loading full email data: " + t.getMessage());
                // המשך עם הנתונים הבסיסיים שכבר יש
            }
        });
    }

    private void displayBasicEmailData() {
        // הצג את הנתונים הבסיסיים בUI
        tvSender.setText("מאת: " + (sender != null ? sender : "לא ידוע"));
        tvSubject.setText(subject != null ? subject : "(ללא נושא)");
        tvContent.setText(content != null ? content : "(ללא תוכן)");

        // פורמט זמן פשוט (לעכשיו)
        if (timestamp != null && timestamp.length() > 10) {
            String timeOnly = timestamp.substring(11, 16); // HH:MM
            String dateOnly = timestamp.substring(0, 10);  // YYYY-MM-DD
            tvTimestamp.setText(dateOnly + " " + timeOnly);
        } else {
            tvTimestamp.setText("זמן לא ידוע");
        }
    }

    private void displayFullEmailData(Email email) {
        // עדכן נתונים שעלולים להיות שונים מהנתונים שהועברו ב-Intent
        if (email.subject != null) {
            tvSubject.setText(email.subject);
        }
        if (email.content != null) {
            tvContent.setText(email.content);
        }
        if (email.sender != null) {
            tvSender.setText("מאת: " + email.sender);
        }

        // הצג recipients אם יש
        if (email.recipients != null && !email.recipients.isEmpty()) {
            StringBuilder recipientsStr = new StringBuilder("אל: ");
            for (int i = 0; i < email.recipients.size(); i++) {
                if (i > 0) recipientsStr.append(", ");
                recipientsStr.append(email.recipients.get(i));
            }
            tvRecipients.setText(recipientsStr.toString());
            tvRecipients.setVisibility(View.VISIBLE);
        } else {
            tvRecipients.setVisibility(View.GONE);
        }

        // הצג תוויות
        displayLabels(currentLabels);

        // בדוק אם יש ספאם
        checkForSpamIndicator(currentLabels);
    }

    private void displayLabels(List<String> labels) {
        if (labels != null && !labels.isEmpty()) {
            StringBuilder labelsStr = new StringBuilder();
            for (int i = 0; i < labels.size(); i++) {
                if (i > 0) labelsStr.append(", ");
                labelsStr.append(labels.get(i));
            }
            tvLabels.setText(labelsStr.toString());
            layoutLabels.setVisibility(View.VISIBLE);
        } else {
            layoutLabels.setVisibility(View.GONE);
        }
    }

    private void checkForSpamIndicator(List<String> labels) {
        boolean isSpam = labels != null && labels.contains("spam");
        ivSpamIndicator.setVisibility(isSpam ? View.VISIBLE : View.GONE);

        if (isSpam) {
            // אולי נרצה לשנות את צבע הכותרת או להוסיף התראה
            tvSubject.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MANAGE_LABELS_REQUEST_CODE && resultCode == RESULT_OK) {
            // תוויות עודכנו - רענן את התצוגה
            if (data != null) {
                ArrayList<String> updatedLabels = data.getStringArrayListExtra("updated_labels");
                if (updatedLabels != null) {
                    currentLabels = updatedLabels;
                    displayLabels(currentLabels);
                    checkForSpamIndicator(currentLabels);

                    Toast.makeText(this, "תוויות עודכנו בהצלחה", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        // חזרה למסך הקודם כשלוחצים על החץ
        onBackPressed();
        return true;
    }
}