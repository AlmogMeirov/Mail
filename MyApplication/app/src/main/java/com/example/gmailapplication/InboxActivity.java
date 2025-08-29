package com.example.gmailapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.stream.Collectors;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import com.example.gmailapplication.database.entities.EmailEntity;
import com.example.gmailapplication.repository.UserRepository;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.gmailapplication.adapters.EmailAdapter;
import com.example.gmailapplication.API.BackendClient;
import com.example.gmailapplication.API.EmailAPI;
import com.example.gmailapplication.repository.EmailRepository;
import com.example.gmailapplication.repository.UserRepository;
import com.example.gmailapplication.shared.*;
import com.example.gmailapplication.utils.EmailRefreshManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
    private UserRepository userRepository;

    private FloatingActionButton fabCompose;

    private EmailAPI emailAPI;
    private String authToken;

    // Repository and refresh manager
    private EmailRepository emailRepository;
    private EmailRefreshManager refreshManager;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Store the refresh listener to remove it later
    private EmailRefreshManager.RefreshListener refreshListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        initViews();
        setupAPI();
        setupRepository();
        setupUserRepository();
        setupAutoRefresh();
        setupSwipeRefresh();
        setupLogoutButton(); // הוסף את זה
        setupRecyclerView();
        setupFab();
        observeInboxEmails();
    }

    private void setupUserRepository() {
        userRepository = new UserRepository(this);
    }
    private void setupLogoutButton() {
        // כפתור התנתקות
        ImageView ivLogout = findViewById(R.id.ivLogout);
        if (ivLogout != null) {
            ivLogout.setOnClickListener(v -> showLogoutConfirmation());
        }

        // כפתור תפריט (לעתיד)
        ImageView ivMenu = findViewById(R.id.ivMenu);
        if (ivMenu != null) {
            ivMenu.setOnClickListener(v -> {
                // TODO: פתח תפריט צד
                Toast.makeText(this, "תפריט צד - בקרוב!", Toast.LENGTH_SHORT).show();
            });
        }

        // כפתור חיפוש
        ImageView ivSearch = findViewById(R.id.ivSearch);
        if (ivSearch != null) {
            ivSearch.setOnClickListener(v -> openSearchActivity());
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.inbox_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_refresh) {
            // רענון ידני
            refreshManager.refreshNow();
            return true;
        } else if (itemId == R.id.action_search) {
            // פתח חיפוש
            openSearchActivity();
            return true;
        } else if (itemId == R.id.action_settings) {
            // פתח הגדרות רענון
            showRefreshSettings();
            return true;
        } else if (itemId == R.id.action_logout) {
            // התנתקות
            showLogoutConfirmation();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("התנתקות")
                .setMessage("האם אתה בטוח שברצונך להתנתק?")
                .setPositiveButton("התנתק", (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton("ביטול", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void performLogout() {
        // הצג התקדמות
        Toast.makeText(this, "מתנתק...", Toast.LENGTH_SHORT).show();

        // עצור רענון אוטומטי
        if (refreshManager != null) {
            refreshManager.stopAutoRefresh();
        }

        // נקה נתונים מקומיים
        if (emailRepository != null) {
            emailRepository.clearAllLocalData();
        }

        // התנתק דרך UserRepository
        if (userRepository != null) {
            userRepository.logout();
        }

        // נקה SharedPreferences
        clearUserSession();

        // חזור למסך הכניסה
        navigateToLogin();
    }

    private void clearUserSession() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        // נקה גם אחסון אחר אם יש
        SharedPreferences draftPrefs = getSharedPreferences("drafts", MODE_PRIVATE);
        SharedPreferences.Editor draftEditor = draftPrefs.edit();
        draftEditor.clear();
        draftEditor.apply();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, MainActivity.class); // או LoginActivity
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        // אנימציית מעבר
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    private void openSearchActivity() {
        // TODO: יישום חיפוש
        Toast.makeText(this, "חיפוש - בקרוב!", Toast.LENGTH_SHORT).show();
    }

    private void showRefreshSettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("הגדרות רענון אוטומטי");

        String[] options = {
                "מהיר (10 שניות)",
                "רגיל (30 שניות)",
                "איטי (60 שניות)",
                "ביטול רענון אוטומטי"
        };

        // בדוק מצב נוכחי
        int currentSelection = getCurrentRefreshSetting();

        builder.setSingleChoiceItems(options, currentSelection, (dialog, which) -> {
            switch (which) {
                case 0: // מהיר
                    refreshManager.setRefreshInterval(10000);
                    saveRefreshSetting(0);
                    Toast.makeText(this, "רענון מהיר הופעל", Toast.LENGTH_SHORT).show();
                    break;
                case 1: // רגיל
                    refreshManager.enableNormalRefresh();
                    saveRefreshSetting(1);
                    Toast.makeText(this, "רענון רגיל הופעל", Toast.LENGTH_SHORT).show();
                    break;
                case 2: // איטי
                    refreshManager.enableSlowRefresh();
                    saveRefreshSetting(2);
                    Toast.makeText(this, "רענון איטי הופעל", Toast.LENGTH_SHORT).show();
                    break;
                case 3: // ביטול
                    refreshManager.stopAutoRefresh();
                    saveRefreshSetting(3);
                    Toast.makeText(this, "רענון אוטומטי בוטל", Toast.LENGTH_SHORT).show();
                    break;
            }
            dialog.dismiss();
        });

        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

    private int getCurrentRefreshSetting() {
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        return prefs.getInt("refresh_setting", 1); // רגיל כברירת מחדל
    }

    private void saveRefreshSetting(int setting) {
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        prefs.edit().putInt("refresh_setting", setting).apply();
    }

    private void initViews() {
        recyclerViewEmails = findViewById(R.id.recyclerViewEmails);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvEmpty = findViewById(R.id.tvEmpty);
        // הסרנו את tabLayout
        fabCompose = findViewById(R.id.fabCompose);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }


    private void setupAPI() {
        emailAPI = BackendClient.get(this).create(EmailAPI.class);

        // Get auth info
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        authToken = prefs.getString("auth_token", "");
        String currentUserEmail = prefs.getString("user_email", "");

        if (tvWelcome != null) {
            tvWelcome.setText("שלום " + (currentUserEmail.isEmpty() ? "משתמש" : currentUserEmail));
        }
    }

    private void setupRepository() {
        Log.d("INBOX_DEBUG", "Setting up EmailRepository");
        emailRepository = EmailRepository.getInstance(this);
        Log.d("INBOX_DEBUG", "EmailRepository instance: " + emailRepository.hashCode());
    }

    private void setupAutoRefresh() {
        refreshManager = EmailRefreshManager.getInstance();

        // Create and store the listener
        refreshListener = new EmailRefreshManager.RefreshListener() {
            @Override
            public void onRefreshRequested() {
                Log.d("INBOX_DEBUG", "InboxActivity: onRefreshRequested");
                emailRepository.fetchEmailsFromServer();
            }

            @Override
            public void onRefreshStarted() {
                Log.d("INBOX_DEBUG", "InboxActivity: onRefreshStarted");
                runOnUiThread(() -> {
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(true);
                    }
                });
            }

            @Override
            public void onRefreshCompleted(boolean success) {
                Log.d("INBOX_DEBUG", "InboxActivity: onRefreshCompleted: " + success);
                runOnUiThread(() -> {
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }

                    if (success) {
                        // אין צורך להציג הודעה
                    } else {
                        Toast.makeText(InboxActivity.this, "שגיאה ברענון מיילים", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };

        // Add the listener
        refreshManager.addRefreshListener(refreshListener);
    }
    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                // רענון ידני על ידי המשתמש
                refreshManager.refreshNow();
            });

            // עיצוב האינדיקטור
            swipeRefreshLayout.setColorSchemeResources(
                    android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light,
                    android.R.color.holo_red_light
            );
        }

        // הוספת לחיצה לכפתור הרענון בheader
        ImageView ivRefresh = findViewById(R.id.ivRefresh);
        if (ivRefresh != null) {
            ivRefresh.setOnClickListener(v -> {
                refreshManager.refreshNow();
            });
        }
    }

    private void updateEmailList(List<Email> emails) {
        if (emailAdapter != null) {
            emailAdapter.updateEmails(emails);
        }

        // הצג/הסתר empty state
        if (emails == null || emails.isEmpty()) {
            if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
            if (recyclerViewEmails != null) recyclerViewEmails.setVisibility(View.GONE);
        } else {
            if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
            if (recyclerViewEmails != null) recyclerViewEmails.setVisibility(View.VISIBLE);
        }
    }

    private void setupRecyclerView() {
        if (recyclerViewEmails != null) {
            recyclerViewEmails.setLayoutManager(new LinearLayoutManager(this));

            // EmailAdapter with click listeners
            emailAdapter = new EmailAdapter(new ArrayList<>(), new EmailAdapter.OnEmailClickListener() {
                @Override
                public void onEmailClick(Email email) {
                    // פתח מייל לצפייה
                    openEmailDetail(email);

                    // סמן כנקרא אם לא נקרא
                    if (email != null && !email.isRead) {
                        emailRepository.markAsRead(email.id);
                    }
                }

                @Override
                public void onEmailLongClick(Email email) {
                    // הצג תפריט פעולות מהירות
                    showQuickActionMenu(email);
                }
            });

            recyclerViewEmails.setAdapter(emailAdapter);
        }
    }

    private void showQuickActionMenu(Email email) {
        if (email == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("פעולות מהירות");

        String[] actions = {
                email.isRead ? "סמן כלא נקרא" : "סמן כנקרא",
                email.isStarred ? "הסר כוכב" : "הוסף כוכב",
                "ארכב",
                "מחק",
                "השב"
        };

        builder.setItems(actions, (dialog, which) -> {
            switch (which) {
                case 0: // סימון קריאה
                    if (email.isRead) {
                        emailRepository.markAsUnread(email.id);
                    } else {
                        emailRepository.markAsRead(email.id);
                    }
                    break;
                case 1: // כוכב
                    emailRepository.toggleStar(email.id);
                    break;
                case 2: // ארכוב
                    emailRepository.archiveEmail(email.id);
                    Toast.makeText(this, "המייל הועבר לארכיון", Toast.LENGTH_SHORT).show();
                    break;
                case 3: // מחיקה
                    emailRepository.deleteEmail(email.id);
                    Toast.makeText(this, "המייל הועבר לפח", Toast.LENGTH_SHORT).show();
                    break;
                case 4: // השב
                    replyToEmail(email);
                    break;
            }
        });

        builder.show();
    }

    private void openEmailDetail(Email email) {
        if (email == null) return;

        // TODO: פתח EmailDetailActivity
        // Intent intent = new Intent(this, EmailDetailActivity.class);
        // intent.putExtra("email_id", email.id);
        // startActivity(intent);

        // בינתיים - הצג toast
        Toast.makeText(this, "פתיחת מייל: " + email.subject, Toast.LENGTH_SHORT).show();
    }

    private void replyToEmail(Email email) {
        if (email == null) return;

        Intent intent = new Intent(this, ComposeActivity.class);
        intent.putExtra("reply_to", email.sender);
        intent.putExtra("reply_subject", "Re: " + (email.subject != null ? email.subject : ""));
        intent.putExtra("reply_content", "\n\n--- הודעה מקורית ---\n" +
                (email.content != null ? email.content : ""));
        startActivity(intent);
    }


    private void setupFab() {
        if (fabCompose != null) {
            fabCompose.setOnClickListener(v -> {
                Intent intent = new Intent(InboxActivity.this, ComposeActivity.class);
                startActivity(intent);
            });
        }
    }

    // החלף את observeInboxEmails() בזה והוסף לוג זמני:

    private void observeInboxEmails() {
        Log.d("INBOX_DEBUG", "=== observeInboxEmails called ===");
        Log.d("INBOX_DEBUG", "EmailRepository instance: " + emailRepository.hashCode());

        emailRepository.getInboxEmails().observe(this, emails -> {
            Log.d("INBOX_DEBUG", "=== RAW DATA FROM REPOSITORY ===");
            Log.d("INBOX_DEBUG", "Raw emails count: " + (emails != null ? emails.size() : "null"));

            if (emails != null) {
                for (int i = 0; i < emails.size(); i++) {
                    EmailEntity entity = emails.get(i);
                    Log.d("INBOX_DEBUG", "Entity[" + i + "]: id=" + entity.id +
                            ", sender=" + entity.sender +
                            ", recipient=" + entity.recipient +
                            ", subject='" + entity.subject + "'");
                }
            }
            Log.d("INBOX_DEBUG", "================================");

            if (emails != null) {
                List<Email> emailList = EmailMapper.toEmails(emails);
                Log.d("INBOX_DEBUG", "After mapping: " + emailList.size() + " emails");

                // קבל את המייל של המשתמש הנוכחי
                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                String currentUserEmail = prefs.getString("user_email", "");
                Log.d("INBOX_DEBUG", "Current user: " + currentUserEmail);

                // *** סינון לדואר נכנס ***
                List<Email> inboxOnlyEmails = emailList.stream()
                        .filter(email -> {
                            if (email == null) return false;

                            Log.d("INBOX_DEBUG", "Checking email: " + email.subject +
                                    " | From: " + email.sender + " | To: " + email.recipient);

                            // לא להציג מיילים מחוקים או בארכיון
                            if (email.isDeleted || email.isArchived) {
                                Log.d("INBOX_DEBUG", "Filtered out: deleted/archived");
                                return false;
                            }

                            // לא להציג טיוטות בדואר נכנס
                            if (email.isDraft()) {
                                Log.d("INBOX_DEBUG", "Filtered out: draft");
                                return false;
                            }

                            // *** הכלל החדש: ***
                            // אם נשלח ממני למישהו אחר - לא להציג בדואר נכנס
                            if (currentUserEmail != null && currentUserEmail.equals(email.sender)) {
                                // אבל אם נשלח ממני לעצמי - כן להציג
                                if (currentUserEmail.equals(email.recipient) ||
                                        (email.recipients != null && email.recipients.contains(currentUserEmail))) {
                                    // זה מייל שנשלח ממני לעצמי - תמיד יופיע בדואר נכנס
                                    Log.d("INBOX_DEBUG", "Included: sent to myself");
                                    return true;
                                } else {
                                    // זה מייל שנשלח ממני למישהו אחר - לא יופיע בדואר נכנס
                                    Log.d("INBOX_DEBUG", "Filtered out: sent to others");
                                    return false;
                                }
                            }

                            // אם יש תוויות, בדוק שיש תווית INBOX
                            if (email.labels != null && !email.labels.isEmpty()) {
                                boolean hasInbox = email.isInbox();
                                Log.d("INBOX_DEBUG", "Has inbox label: " + hasInbox);
                                return hasInbox;
                            }

                            // fallback - מיילים שהתקבלו
                            boolean isReceived = "received".equals(email.direction);
                            Log.d("INBOX_DEBUG", "Fallback - is received: " + isReceived);
                            return isReceived;
                        })
                        .collect(java.util.stream.Collectors.toList());

                Log.d("INBOX_DEBUG", "Final inbox emails: " + inboxOnlyEmails.size());
                updateEmailList(inboxOnlyEmails);
            }
        });

        // שאר הקוד נשאר אותו דבר...
        emailRepository.isLoading().observe(this, isLoading -> {
            if (swipeRefreshLayout != null && !refreshManager.isCurrentlyRefreshing()) {
                swipeRefreshLayout.setRefreshing(isLoading != null ? isLoading : false);
            }
        });

        emailRepository.getLastError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, "שגיאה: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        emailRepository.getUnreadCount().observe(this, count -> {
            if (count != null && count > 0) {
                if (tvWelcome != null) {
                    tvWelcome.setText("דואר נכנס (" + count + " לא נקראו)");
                }
            } else {
                if (tvWelcome != null) {
                    tvWelcome.setText("דואר נכנס");
                }
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (refreshManager != null) {
            refreshManager.resumeAutoRefresh();
            refreshManager.enableNormalRefresh();
        }

        // בדוק אם יש שינויים שצריכים סנכרון
        if (emailRepository != null) {
            emailRepository.syncPendingChanges();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (refreshManager != null) {
            refreshManager.pauseAutoRefresh();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (refreshManager != null && refreshListener != null) {
            refreshManager.removeRefreshListener(refreshListener);
        }
    }
}