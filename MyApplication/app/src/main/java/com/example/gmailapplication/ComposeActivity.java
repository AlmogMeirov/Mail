package com.example.gmailapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.gmailapplication.API.BackendClient;
import com.example.gmailapplication.API.EmailAPI;
import com.example.gmailapplication.shared.SendEmailRequest;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComposeActivity extends AppCompatActivity {

    private EditText etTo, etSubject, etContent;
    private Toolbar toolbar;

    private EmailAPI emailAPI;
    private String authToken;
    private String currentUserEmail;

    private boolean isSending = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        initViews();
        setupToolbar();
        setupAPI();
        handleReplyData();
        setupBackPressHandler();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etTo = findViewById(R.id.etTo);
        etSubject = findViewById(R.id.etSubject);
        etContent = findViewById(R.id.etContent);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("כתיבת מייל");
        }
    }

    private void setupAPI() {
        emailAPI = BackendClient.get(this).create(EmailAPI.class);

        // Get auth token
        authToken = getIntent().getStringExtra("auth_token");
        if (authToken == null) {
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            authToken = prefs.getString("auth_token", "");
            currentUserEmail = prefs.getString("user_email", "");
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

        // Get current user email if not set
        if (TextUtils.isEmpty(currentUserEmail)) {
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            currentUserEmail = prefs.getString("user_email", "");
        }
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isSending) {
                    Toast.makeText(ComposeActivity.this, "מייל נשלח כעת, אנא המתן...", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (hasUnsavedContent()) {
                    showExitConfirmationDialog();
                } else {
                    finishActivity();
                }
            }
        });
    }

    private boolean hasUnsavedContent() {
        String to = etTo.getText().toString().trim();
        String subject = etSubject.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        return !TextUtils.isEmpty(to) || !TextUtils.isEmpty(subject) || !TextUtils.isEmpty(content);
    }

    private void showExitConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("שמירת טיוטה")
                .setMessage("יש לך תוכן שלא נשמר. מה תרצה לעשות?")
                .setPositiveButton("שמור כטיוטה", (dialog, which) -> {
                    saveDraft();
                    finishActivity();
                })
                .setNegativeButton("יציאה בלי שמירה", (dialog, which) -> {
                    Toast.makeText(ComposeActivity.this, "טיוטה לא נשמרה", Toast.LENGTH_SHORT).show();
                    finishActivity();
                })
                .setNeutralButton("ביטול", (dialog, which) -> {
                    // נשאר באקטיביטי
                })
                .setCancelable(true)
                .show();
    }

    private void saveDraft() {
        SharedPreferences draftPrefs = getSharedPreferences("drafts", MODE_PRIVATE);
        SharedPreferences.Editor editor = draftPrefs.edit();

        String timestamp = String.valueOf(System.currentTimeMillis());
        editor.putString("draft_" + timestamp + "_to", etTo.getText().toString().trim());
        editor.putString("draft_" + timestamp + "_subject", etSubject.getText().toString().trim());
        editor.putString("draft_" + timestamp + "_content", etContent.getText().toString().trim());
        editor.apply();

        Toast.makeText(this, "טיוטה נשמרה בהצלחה", Toast.LENGTH_SHORT).show();
    }

    private void finishActivity() {
        finish();
    }

    private void handleReplyData() {
        String replyTo = getIntent().getStringExtra("reply_to");
        String replySubject = getIntent().getStringExtra("reply_subject");
        String replyContent = getIntent().getStringExtra("reply_content");

        if (!TextUtils.isEmpty(replyTo)) {
            etTo.setText(replyTo);
        }

        if (!TextUtils.isEmpty(replySubject)) {
            etSubject.setText(replySubject);
        }

        if (!TextUtils.isEmpty(replyContent)) {
            etContent.setText(replyContent);
            etContent.setSelection(0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.compose_menu, menu);

        MenuItem sendItem = menu.findItem(R.id.action_send);
        sendItem.setEnabled(!isSending);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        } else if (itemId == R.id.action_send) {
            sendEmail();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void sendEmail() {
        if (isSending) return;

        String to = etTo.getText().toString().trim();
        String subject = etSubject.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (TextUtils.isEmpty(to)) {
            etTo.setError("יש להזין כתובת נמען");
            etTo.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(to).matches()) {
            etTo.setError("כתובת מייל לא תקינה");
            etTo.requestFocus();
            return;
        }

        isSending = true;
        invalidateOptionsMenu();

        SendEmailRequest request = new SendEmailRequest();
        request.sender = currentUserEmail;

        List<String> recipientsList = parseRecipients(to);

        if (!recipientsList.isEmpty()) {
            request.recipient = recipientsList.get(0);
            request.recipients = recipientsList;
        }

        request.subject = subject;
        request.content = content;
        request.labels = Arrays.asList("inbox");

        System.out.println("=== DETAILED DEBUG ===");
        System.out.println("Current user email: " + currentUserEmail);
        System.out.println("Request sender: " + request.sender);
        System.out.println("Request recipient: " + request.recipient);
        System.out.println("Request subject: " + request.subject);
        System.out.println("Request content length: " + (request.content != null ? request.content.length() : 0));
        System.out.println("===================");

        emailAPI.sendEmail(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                isSending = false;
                invalidateOptionsMenu();

                System.out.println("=== SEND EMAIL RESPONSE DEBUG ===");
                System.out.println("Response code: " + response.code());
                System.out.println("Response successful: " + response.isSuccessful());

                if (response.isSuccessful()) {
                    System.out.println("*** EMAIL SENT SUCCESSFULLY - RESPONSE CODE: " + response.code() + " ***");
                    Toast.makeText(ComposeActivity.this, "המייל נשלח בהצלחה!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                    System.out.println("*** RETURNING TO INBOX ACTIVITY ***");
                } else {
                    System.out.println("*** EMAIL SEND FAILED - RESPONSE CODE: " + response.code() + " ***");
                    handleSendError(response.code());
                }

                System.out.println("================================");
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                isSending = false;
                invalidateOptionsMenu();

                System.out.println("=== SEND EMAIL FAILURE ===");
                System.out.println("Error: " + t.getMessage());
                System.out.println("Error type: " + t.getClass().getSimpleName());
                System.out.println("==========================");

                Toast.makeText(ComposeActivity.this, "שגיאת חיבור: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<String> parseRecipients(String to) {
        String[] recipients = to.split(",");
        return Arrays.stream(recipients)
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }

    private void handleSendError(int responseCode) {
        switch (responseCode) {
            case 400:
                Toast.makeText(this, "נתונים לא תקינים - בדוק את כתובות הנמענים", Toast.LENGTH_SHORT).show();
                break;
            case 403:
                Toast.makeText(this, "אין הרשאה לשלוח מכתובת זו", Toast.LENGTH_SHORT).show();
                break;
            case 500:
                Toast.makeText(this, "שגיאת שרת - נסה שוב מאוחר יותר", Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(this, "שליחת המייל נכשלה (קוד " + responseCode + ")", Toast.LENGTH_SHORT).show();
        }
    }
}