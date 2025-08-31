package com.example.gmailapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gmailapplication.API.BackendClient;
import com.example.gmailapplication.API.LabelAPI;
import com.example.gmailapplication.adapters.EmailAdapter;
import com.example.gmailapplication.shared.CreateLabelRequest;
import com.example.gmailapplication.shared.Label;
import com.example.gmailapplication.shared.TokenManager;
import com.example.gmailapplication.viewmodels.InboxViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InboxActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private RecyclerView recyclerView;
    private EmailAdapter adapter;
    private InboxViewModel viewModel;

    // Navigation Drawer components
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);
        System.out.println("=== INBOX ACTIVITY STARTED ===");

        // Init views
        initViews();

        // Init ViewModel
        viewModel = new ViewModelProvider(this).get(InboxViewModel.class);

        setupRecyclerView();
        setupObservers();
        setupNavigationDrawer();
        setupHeader();

        // Load emails
        viewModel.loadEmails();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewEmails);
        FloatingActionButton fab = findViewById(R.id.fabCompose);

        // Navigation Drawer
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        setupFab(fab);
    }

    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_inbox);

        setupNavigationHeader();
        loadCustomLabels(); // טעינת תוויות מותאמות
    }

    private void setupNavigationHeader() {
        // Get header view
        View headerView = navigationView.getHeaderView(0);

        // Find views in header
        TextView tvUserEmail = headerView.findViewById(R.id.tvUserEmail);
        Button btnCreateNewLabel = headerView.findViewById(R.id.btnCreateNewLabel);

        // Set current user email
        String currentUserEmail = TokenManager.getCurrentUserEmail(this);
        if (currentUserEmail != null) {
            tvUserEmail.setText(currentUserEmail);
        }

        // Setup create label button
        btnCreateNewLabel.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            showCreateLabelDialog();
        });
    }

    private void loadCustomLabels() {
        LabelAPI labelAPI = BackendClient.get(this).create(LabelAPI.class);

        labelAPI.getAllLabels().enqueue(new Callback<List<Label>>() {
            @Override
            public void onResponse(Call<List<Label>> call, Response<List<Label>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    addCustomLabelsToMenu(response.body());
                } else {
                    System.out.println("Failed to load labels: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Label>> call, Throwable t) {
                System.out.println("Error loading labels: " + t.getMessage());
            }
        });
    }

    private void addCustomLabelsToMenu(List<Label> allLabels) {
        // מסנן רק תוויות מותאמות (לא מערכת)
        List<Label> customLabels = new ArrayList<>();
        for (Label label : allLabels) {
            if (!label.isSystem) {
                customLabels.add(label);
            }
        }

        if (customLabels.isEmpty()) {
            return; // אין תוויות מותאמות
        }

        // מקבל את התפריט של הNavigation View
        Menu menu = navigationView.getMenu();

        // מנקה תוויות קיימות בקבוצה המותאמת
        menu.removeGroup(R.id.group_custom_labels);

        // מוסיף כותרת לתוויות מותאמות
        menu.add(R.id.group_custom_labels, Menu.NONE, Menu.NONE, "תוויות אישיות")
                .setEnabled(false);

        // מוסיף את התוויות החדשות
        for (int i = 0; i < customLabels.size(); i++) {
            Label label = customLabels.get(i);
            MenuItem item = menu.add(R.id.group_custom_labels,
                    View.generateViewId(), i + 1, label.name);
            item.setIcon(android.R.drawable.ic_menu_mylocation);
        }
    }

    private void refreshNavigationMenu() {
        // עכשיו מממש את הפונקציה - רענון תוויות אחרי יצירת תווית חדשה
        loadCustomLabels();
    }

    private void setupRecyclerView() {
        String currentUserEmail = TokenManager.getCurrentUserEmail(this);
        System.out.println("Current user email from token: " + currentUserEmail);

        adapter = new EmailAdapter(email -> {
            // העבר למסך פרטי המייל
            Intent intent = new Intent(this, EmailDetailActivity.class);
            intent.putExtra("email_id", email.id);
            intent.putExtra("sender", email.sender);
            intent.putExtra("subject", email.subject);
            intent.putExtra("content", email.content);
            intent.putExtra("timestamp", email.timestamp);
            startActivity(intent);
        }, currentUserEmail);

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

        // Observer חדש לעדכון כותרת המסך
        viewModel.getCurrentFilterDisplayName().observe(this, displayName -> {
            TextView tvWelcome = findViewById(R.id.tvWelcome);
            if (tvWelcome != null && displayName != null) {
                tvWelcome.setText(displayName);
            }
        });

        System.out.println("Observer setup complete");
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refreshCurrentFilter(); // רענון הסינון הנוכחי במקום loadEmails()
    }

    private void setupFab(FloatingActionButton fab) {
        fab.setOnClickListener(v -> {
            startActivity(new Intent(this, ComposeActivity.class));
        });
    }

    private void setupHeader() {
        ImageView ivMenu = findViewById(R.id.ivMenu);
        ImageView ivLogout = findViewById(R.id.ivLogout);
        ImageView ivRefresh = findViewById(R.id.ivRefresh);
        ImageView ivSearch = findViewById(R.id.ivSearch);

        // כפתור המבורגר - פתיחת Navigation Drawer
        ivMenu.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        ivLogout.setOnClickListener(v -> showLogoutConfirmation());

        ivRefresh.setOnClickListener(v -> {
            viewModel.refreshCurrentFilter(); // רענון הסינון הנוכחי
            Toast.makeText(this, "מרענן...", Toast.LENGTH_SHORT).show();
        });

        // ivSearch - לעתיד
    }

    private void showCreateLabelDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("יצירת תווית חדשה");

        final EditText input = new EditText(this);
        input.setHint("שם התווית");
        input.setPadding(32, 32, 32, 32);
        builder.setView(input);

        builder.setPositiveButton("צור", (dialog, which) -> {
            String labelName = input.getText().toString().trim();
            if (!labelName.isEmpty()) {
                createLabel(labelName);
            } else {
                Toast.makeText(InboxActivity.this,
                        "יש להזין שם תווית", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("ביטול", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Focus on input
        input.requestFocus();
    }

    private void createLabel(String labelName) {
        LabelAPI labelAPI = BackendClient.get(this).create(LabelAPI.class);
        CreateLabelRequest request = new CreateLabelRequest(labelName);

        labelAPI.createLabel(request).enqueue(new Callback<Label>() {
            @Override
            public void onResponse(Call<Label> call, Response<Label> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Label newLabel = response.body();
                    Toast.makeText(InboxActivity.this,
                            "תווית '" + newLabel.name + "' נוצרה בהצלחה", Toast.LENGTH_SHORT).show();

                    // רענון התפריט כדי להציג את התווית החדשה
                    refreshNavigationMenu();

                } else {
                    Toast.makeText(InboxActivity.this,
                            "שגיאה ביצירת תווית: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Label> call, Throwable t) {
                Toast.makeText(InboxActivity.this,
                        "שגיאה בחיבור לשרת: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Close drawer
        drawerLayout.closeDrawer(GravityCompat.START);

        int itemId = item.getItemId();
        int groupId = item.getGroupId();

        // תוויות מערכת (הקוד הקיים)
        if (itemId == R.id.nav_inbox) {
            viewModel.loadEmails();
            return true;
        } else if (itemId == R.id.nav_sent) {
            viewModel.loadEmailsByLabel("sent", "נשלח");
            return true;
        } else if (itemId == R.id.nav_drafts) {
            viewModel.loadEmailsByLabel("drafts", "טיוטות");
            return true;
        } else if (itemId == R.id.nav_starred) {
            viewModel.loadStarredEmails();
            return true;
        } else if (itemId == R.id.nav_spam) {
            viewModel.loadSpamEmails();
            return true;
        } else if (itemId == R.id.nav_trash) {
            viewModel.loadEmailsByLabel("trash", "אשפה");
            return true;
        }

        // תוויות מותאמות אישית
        else if (groupId == R.id.group_custom_labels) {
            String labelName = item.getTitle().toString();
            // דילוג על הכותרת "תוויות אישיות"
            if (!labelName.equals("תוויות אישיות")) {
                viewModel.loadEmailsByLabel(labelName.toLowerCase(), labelName);
            }
            return true;
        }

        return false;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
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