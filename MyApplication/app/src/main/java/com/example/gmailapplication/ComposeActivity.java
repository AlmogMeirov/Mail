package com.example.gmailapplication;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.gmailapplication.API.BackendClient;
import com.example.gmailapplication.API.EmailAPI;
import com.example.gmailapplication.shared.Email;
import com.example.gmailapplication.shared.Label;
import com.example.gmailapplication.shared.SendEmailRequest;
import com.example.gmailapplication.shared.SendEmailResponse;
import com.example.gmailapplication.utils.EmailRefreshManager;

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
    private EmailRefreshManager refreshManager; // הוספת מנהל הרענון

    private boolean isSending = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
        }

        initViews();
        setupToolbar();
        setupAPI();
        handleReplyData();
        setupBackPressHandler(); // הוספת הטיפול בלחיצת החזור
        setupRefreshManager(); // הוספת מנהל הרענון
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

    /**
     * הגדרת מנהל הרענון האוטומטי
     */
    private void setupRefreshManager() {
        refreshManager = EmailRefreshManager.getInstance();
    }

    /**
     * הגדרת הטיפול בלחיצת החזור עם OnBackPressedDispatcher
     */
    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // אם נמצא בתהליך שליחה - לא לאפשר יציאה
                if (isSending) {
                    Toast.makeText(ComposeActivity.this, "מייל נשלח כעת, אנא המתן...", Toast.LENGTH_SHORT).show();
                    return;
                }

                // בדיקה אם למשתמש יש תוכן שלא נשמר
                if (hasUnsavedContent()) {
                    showExitConfirmationDialog();
                } else {
                    // יציאה רגילה - אין תוכן לא נשמר
                    finishActivity();
                }
            }
        });
    }

    /**
     * בדיקה אם יש תוכן שלא נשמר
     */
    private boolean hasUnsavedContent() {
        String to = etTo.getText().toString().trim();
        String subject = etSubject.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        return !TextUtils.isEmpty(to) || !TextUtils.isEmpty(subject) || !TextUtils.isEmpty(content);
    }

    /**
     * הצגת דיאלוג אישור יציאה עם אפשרות לשמירת טיוטה
     */
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
                    // נשאר באקטיביטי - לא עושים כלום
                })
                .setCancelable(true) // אפשרות לביטול בלחיצה מחוץ לדיאלוג
                .show();
    }

    /**
     * שמירת טיוטה (יש להוסיף את הלוגיקה בהתאם לצרכים)
     */
    private void saveDraft() {
        // TODO: מימוש שמירת טיוטה למסד נתונים או SharedPreferences

        // לדוגמה - שמירה ל-SharedPreferences:
        SharedPreferences draftPrefs = getSharedPreferences("drafts", MODE_PRIVATE);
        SharedPreferences.Editor editor = draftPrefs.edit();

        String timestamp = String.valueOf(System.currentTimeMillis());
        editor.putString("draft_" + timestamp + "_to", etTo.getText().toString().trim());
        editor.putString("draft_" + timestamp + "_subject", etSubject.getText().toString().trim());
        editor.putString("draft_" + timestamp + "_content", etContent.getText().toString().trim());
        editor.apply();

        Toast.makeText(this, "טיוטה נשמרה בהצלחה", Toast.LENGTH_SHORT).show();
    }

    /**
     * יציאה מהאקטיביטי
     */
    private void finishActivity() {
        finish();
    }

    private void handleReplyData() {
        // Check if this is a reply
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
            // Position cursor at the beginning for user to type
            etContent.setSelection(0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.compose_menu, menu);

        // Update send button state
        MenuItem sendItem = menu.findItem(R.id.action_send);
        sendItem.setEnabled(!isSending);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            // השתמש ב-OnBackPressedDispatcher במקום finish() ישירות
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

        // Validate input FIRST
        String to = etTo.getText().toString().trim();
        String subject = etSubject.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (TextUtils.isEmpty(to)) {
            etTo.setError("יש להזין כתובת נמען");
            etTo.requestFocus();
            return;
        }

        // Validate email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(to).matches()) {
            etTo.setError("כתובת מייל לא תקינה");
            etTo.requestFocus();
            return;
        }

        // Start sending
        isSending = true;
        invalidateOptionsMenu(); // Update send button state

        // Create request WITH ALL DATA
        SendEmailRequest request = new SendEmailRequest();
        request.sender = currentUserEmail;

        // Parse recipients
        List<String> recipientsList = parseRecipients(to);

        // Set both recipient (for single) and recipients (for multiple) for backward compatibility
        if (!recipientsList.isEmpty()) {
            request.recipient = recipientsList.get(0); // First recipient for single field
            request.recipients = recipientsList; // All recipients for array field
        }

        request.subject = subject;
        request.content = content;
        request.labels = Arrays.asList("inbox"); // Default label

        // NOW debug with actual data
        System.out.println("=== DETAILED DEBUG ===");
        System.out.println("Current user email: " + currentUserEmail);
        System.out.println("Request sender: " + request.sender);
        System.out.println("Request recipient: " + request.recipient);
        System.out.println("Request subject: " + request.subject);
        System.out.println("Request content length: " + (request.content != null ? request.content.length() : 0));

        // Check TokenManager
        String tokenFromManager = com.example.gmailapplication.shared.TokenManager.get(this);
        System.out.println("Token from TokenManager: " + (tokenFromManager != null ? tokenFromManager.substring(0, Math.min(20, tokenFromManager.length())) + "..." : "NULL"));

        // Check SharedPreferences
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String tokenFromPrefs = prefs.getString("auth_token", "");
        System.out.println("Token from SharedPrefs: " + (tokenFromPrefs.isEmpty() ? "EMPTY" : tokenFromPrefs.substring(0, Math.min(20, tokenFromPrefs.length())) + "..."));
        System.out.println("===================");

        // Send email
        emailAPI.sendEmail(request).enqueue(new Callback<SendEmailResponse>() {
            @Override
            public void onResponse(Call<SendEmailResponse> call, Response<SendEmailResponse> response) {
                isSending = false;
                invalidateOptionsMenu();

                System.out.println("=== SEND EMAIL RESPONSE DEBUG ===");
                System.out.println("Response code: " + response.code());
                System.out.println("Response successful: " + response.isSuccessful());

                // *** התמקדות בקוד התגובה במקום בפענוח JSON ***
                if (response.isSuccessful()) {
                    System.out.println("*** EMAIL SENT SUCCESSFULLY - RESPONSE CODE: " + response.code() + " ***");

                    // הצגת הודעת הצלחה
                    Toast.makeText(ComposeActivity.this, "המייל נשלח בהצלחה!", Toast.LENGTH_SHORT).show();

                    // *** הפעלת רענון מהיר אחרי שליחת מייל ***
                    if (refreshManager != null) {
                        System.out.println("*** ENABLING FAST REFRESH AFTER EMAIL SENT ***");
                        refreshManager.enableFastRefresh();
                    }

                    // חזרה לInboxActivity עם תוצאה חיובית
                    setResult(RESULT_OK);
                    finish();

                    // *** לא מנסים לפענח JSON כלל - סומכים על קוד התגובה ***
                    System.out.println("*** RETURNING TO INBOX ACTIVITY ***");

                } else {
                    // כשלון בשליחה
                    System.out.println("*** EMAIL SEND FAILED - RESPONSE CODE: " + response.code() + " ***");
                    handleSendError(response);
                }

                System.out.println("================================");
            }

            @Override
            public void onFailure(Call<SendEmailResponse> call, Throwable t) {
                isSending = false;
                invalidateOptionsMenu();

                System.out.println("=== SEND EMAIL FAILURE ===");
                System.out.println("Error: " + t.getMessage());
                System.out.println("Error type: " + t.getClass().getSimpleName());
                System.out.println("==========================");

                Toast.makeText(ComposeActivity.this, "שגיאת חיבור: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }); // *** תיקון: הוספת הסוגריים החסרות ***
    } // *** תיקון: הוספת הסוגריים החסרות לסגירת השיטה ***

    private List<String> parseRecipients(String to) {
        // Split by comma and clean up
        String[] recipients = to.split(",");
        return Arrays.stream(recipients)
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }

    private void handleSendError(Response<SendEmailResponse> response) {
        try {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "שגיאה לא ידועה";

            switch (response.code()) {
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
                    Toast.makeText(this, "שליחת המייל נכשלה (קוד " + response.code() + ")", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "שליחת המייל נכשלה", Toast.LENGTH_SHORT).show();
        }
    }
}