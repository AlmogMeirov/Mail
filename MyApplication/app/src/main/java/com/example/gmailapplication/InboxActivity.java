package com.example.gmailapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.navigation.NavigationView;


import com.example.gmailapplication.repository.UserRepository;


import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
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
import com.google.android.material.navigation.NavigationView;
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
    private UserRepository userRepository;

    private TabLayout tabLayout;
    private FloatingActionButton fabCompose;

    private EmailAPI emailAPI;
    private String authToken;
    private String currentFilter = "inbox"; // "inbox", "spam", "sent"

    // Repository and refresh manager
    private EmailRepository emailRepository;
    private EmailRefreshManager refreshManager;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Store the refresh listener to remove it later
    private EmailRefreshManager.RefreshListener refreshListener;

    private DrawerLayout drawer_layout;
    private NavigationView nav_view;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_inbox);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
        }



        initViews();
        setupAPI();
        setupRepository();
        setupUserRepository();
        setupAutoRefresh();
        setupSwipeRefresh();
        setupLogoutButton(); // הוסף את זה
        setupRecyclerView();
        setupTabs();
        setupFab();
        observeEmails();
        setupDrawer();
        if (refreshManager != null) {
            refreshManager.refreshNow(); // רענון מיידי!
        }
    }

    // הוסף את זה ל-onCreate
    private void setupDrawer() {
        // הגדר את הToolbar
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer_layout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawer_layout.addDrawerListener(toggle);
        toggle.syncState();

        // הוסף listener לNavigation View
        nav_view.setNavigationItemSelectedListener(menuItem -> {
            handleNavigationClick(menuItem.getItemId());
            drawer_layout.closeDrawers();
            return true;
        });
    }

    private void handleNavigationClick(int itemId) {
        if (itemId == R.id.nav_inbox) {
            currentFilter = "inbox";
            observeInboxEmails();
        } else if (itemId == R.id.nav_sent) {
            currentFilter = "sent";
            observeSentEmails();
        } else if (itemId == R.id.nav_drafts) {
            currentFilter = "drafts";
            observeDrafts();
        } else if (itemId == R.id.nav_starred) {
            currentFilter = "starred";
            observeStarredEmails();
        } else if (itemId == R.id.nav_manage_labels) {
            openLabelsActivity();
        } else if (itemId == R.id.nav_create_label) {
            // פתח דיאלוג יצירת תווית
            // TODO: implement create label dialog
        }
    }

    private void setupUserRepository() {
        userRepository = new UserRepository(this);
    }
    private void setupLogoutButton() {
        // אם הוספת כפתור אייקון
        ImageView ivLogout = findViewById(R.id.ivLogout);
        if (ivLogout != null) {
            ivLogout.setOnClickListener(v -> showLogoutConfirmation());
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.inbox_menu, menu);
        return true;
    }
    // *** הוסף את הפונקציה הזאת ***
    private void openLabelsActivity() {
        Intent intent = new Intent(this, LabelsActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_refresh) {
            refreshManager.refreshNow();
            return true;
        } else if (itemId == R.id.action_search) {
            openSearchActivity();
            return true;
        } else if (itemId == R.id.action_settings) {
            showRefreshSettings();
            return true;
        } else if (itemId == R.id.action_labels) { // *** הוסף את זה ***
            openLabelsActivity();
            return true;
        } else if (itemId == R.id.action_logout) {
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

    // החלף את הפונקציה openSearchActivity() ב-InboxActivity.java:

    private void openSearchActivity() {
        // יצירת dialog עם שדה חיפוש
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("חיפוש מיילים");

        // יצירת EditText לחיפוש
        final EditText searchInput = new EditText(this);
        searchInput.setHint("הקלד מילות חיפוש...");
        searchInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        builder.setView(searchInput);

        builder.setPositiveButton("חפש", (dialog, which) -> {
            String searchQuery = searchInput.getText().toString().trim();
            if (!searchQuery.isEmpty()) {
                performSearch(searchQuery);
            }
        });

        builder.setNegativeButton("ביטול", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        // פתח מקלדת אוטומטית
        searchInput.requestFocus();
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(searchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
    }

    // פונקציה חדשה לביצוע חיפוש
    // פונקציה חדשה לביצוע חיפוש
    private void performSearch(String query) {
        Toast.makeText(this, "מחפש: " + query, Toast.LENGTH_SHORT).show();

        // בדוק אם יש auth token
        if (authToken == null || authToken.isEmpty()) {
            Toast.makeText(this, "שגיאה: לא מחובר למערכת", Toast.LENGTH_SHORT).show();
            return;
        }

        // חיפוש ב-API עם authentication
        emailAPI.searchEmails("Bearer " + authToken, query).enqueue(new Callback<List<Email>>() {
            @Override
            public void onResponse(Call<List<Email>> call, Response<List<Email>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Email> searchResults = response.body();
                    updateEmailList(searchResults);
                    Toast.makeText(InboxActivity.this,
                            "נמצאו " + searchResults.size() + " תוצאות", Toast.LENGTH_SHORT).show();
                } else {
                    // הוסף לוג מפורט
                    android.util.Log.e("Search", "Response code: " + response.code());
                    if (response.errorBody() != null) {
                        try {
                            android.util.Log.e("Search", "Error: " + response.errorBody().string());
                        } catch (Exception e) {
                            android.util.Log.e("Search", "Error reading error body", e);
                        }
                    }

                    // נסה גם בלי authentication כ-fallback
                    if (response.code() == 401) {
                        searchWithoutAuth(query);
                    } else {
                        Toast.makeText(InboxActivity.this, "לא נמצאו תוצאות", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Email>> call, Throwable t) {
                android.util.Log.e("Search", "Search failed", t);

                // נסה חיפוש בלי authentication כ-fallback
                searchWithoutAuth(query);
            }
        });
    }

    // פונקציה עזר לחיפוש בלי authentication
    private void searchWithoutAuth(String query) {
        emailAPI.searchEmails(query).enqueue(new Callback<List<Email>>() {
            @Override
            public void onResponse(Call<List<Email>> call, Response<List<Email>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Email> searchResults = response.body();
                    updateEmailList(searchResults);
                    Toast.makeText(InboxActivity.this,
                            "נמצאו " + searchResults.size() + " תוצאות", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(InboxActivity.this, "לא נמצאו תוצאות", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Email>> call, Throwable t) {
                Toast.makeText(InboxActivity.this, "שגיאה בחיפוש: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
        tabLayout = findViewById(R.id.tabLayout);
        fabCompose = findViewById(R.id.fabCompose);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        drawer_layout = findViewById(R.id.drawer_layout);
        nav_view = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
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
        emailRepository = EmailRepository.getInstance(this);
    }

    private void setupAutoRefresh() {
        refreshManager = EmailRefreshManager.getInstance();

        // Create and store the listener
        refreshListener = new EmailRefreshManager.RefreshListener() {
            @Override
            public void onRefreshRequested() {
                // הרענון מתבצע דרך Repository
                emailRepository.fetchEmailsFromServer();
            }

            @Override
            public void onRefreshStarted() {
                runOnUiThread(() -> {
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(true);
                    }
                });
            }

            @Override
            public void onRefreshCompleted(boolean success) {
                runOnUiThread(() -> {
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }

                    if (success) {
                        // אין צורך להציג הודעה - הנתונים מתעדכנים אוטומטית דרך LiveData
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

        // *** הוסף את השורה הזאת! ***
        // הוספת לחיצה לכפתור החיפוש בheader
        ImageView ivSearch = findViewById(R.id.ivSearch); // או המזהה הנכון
        if (ivSearch != null) {
            ivSearch.setOnClickListener(v -> {
                openSearchActivity();
            });
        }
    }

    private void observeEmails() {
        // צפייה במיילים בזמן אמת דרך LiveData
        emailRepository.getInboxEmails().observe(this, emails -> {
            if (emails != null) {
                // המר EmailEntity ל-Email עבור האדפטר הקיים
                List<Email> emailList = EmailMapper.toEmails(emails);
                updateEmailList(emailList);
            }
        });

        // צפייה בסטטוס טעינה
        emailRepository.isLoading().observe(this, isLoading -> {
            if (swipeRefreshLayout != null && !refreshManager.isCurrentlyRefreshing()) {
                swipeRefreshLayout.setRefreshing(isLoading != null ? isLoading : false);
            }
        });

        // צפייה בשגיאות
        emailRepository.getLastError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, "שגיאה: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        // צפייה במספר מיילים לא נקראים
        emailRepository.getUnreadCount().observe(this, count -> {
            if (count != null && count > 0) {
                // עדכן את התצוגה או הtitle עם מספר המיילים הלא נקראים
                if (tvWelcome != null) {
                    tvWelcome.setText("תיבת דואר (" + count + " לא נקראו)");
                }
            } else {
                if (tvWelcome != null) {
                    tvWelcome.setText("תיבת דואר");
                }
            }
        });
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

        // *** תיקון: הסרת ה-TODO והוספת הקוד האמיתי ***
        Intent intent = new Intent(this, EmailDetailActivity.class);
        intent.putExtra("email_id", email.id);
        intent.putExtra("auth_token", authToken);

        // הוספת נתונים נוספים כ-fallback
        intent.putExtra("subject", email.subject != null ? email.subject : "");
        intent.putExtra("sender", email.sender != null ? email.sender : "");
        intent.putExtra("content", email.content != null ? email.content : "");
        intent.putExtra("timestamp", email.timestamp != null ? email.timestamp : "");

        startActivity(intent);
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

    private void setupTabs() {
        if (tabLayout != null) {
            tabLayout.addTab(tabLayout.newTab().setText("דואר נכנס"));
            tabLayout.addTab(tabLayout.newTab().setText("נשלח"));
            tabLayout.addTab(tabLayout.newTab().setText("טיוטות"));
            tabLayout.addTab(tabLayout.newTab().setText("כוכב"));

            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    switch (tab.getPosition()) {
                        case 0: // דואר נכנס
                            observeInboxEmails();
                            currentFilter = "inbox";
                            break;
                        case 1: // נשלח
                            observeSentEmails();
                            currentFilter = "sent";
                            break;
                        case 2: // טיוטות
                            observeDrafts();
                            currentFilter = "drafts";
                            break;
                        case 3: // כוכב
                            observeStarredEmails();
                            currentFilter = "starred";
                            break;
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}

                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });
        }
    }

    private void setupFab() {
        if (fabCompose != null) {
            fabCompose.setOnClickListener(v -> {
                Intent intent = new Intent(InboxActivity.this, ComposeActivity.class);
                startActivity(intent);
            });
        }
    }

    private void observeInboxEmails() {
        emailRepository.getInboxEmails().observe(this, emails -> {
            if (emails != null) {
                List<Email> emailList = EmailMapper.toEmails(emails);
                updateEmailList(emailList);
            }
        });
    }

    private void observeSentEmails() {
        emailRepository.getSentEmails().observe(this, emails -> {
            if (emails != null) {
                List<Email> emailList = EmailMapper.toEmails(emails);
                updateEmailList(emailList);
            }
        });
    }

    private void observeDrafts() {
        emailRepository.getDrafts().observe(this, emails -> {
            if (emails != null) {
                List<Email> emailList = EmailMapper.toEmails(emails);
                updateEmailList(emailList);
            }
        });
    }

    private void observeStarredEmails() {
        emailRepository.getStarredEmails().observe(this, emails -> {
            if (emails != null) {
                List<Email> emailList = EmailMapper.toEmails(emails);
                updateEmailList(emailList);
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