package com.example.gmailapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gmailapplication.API.BackendClient;
import com.example.gmailapplication.API.LabelAPI;
import com.example.gmailapplication.API.UserAPI;
import com.example.gmailapplication.adapters.EmailAdapter;
import com.example.gmailapplication.shared.CreateLabelRequest;
import com.example.gmailapplication.shared.Email;
import com.example.gmailapplication.shared.Label;
import com.example.gmailapplication.shared.TokenManager;
import com.example.gmailapplication.shared.UpdateLabelRequest;
import com.example.gmailapplication.viewmodels.InboxViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
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

    // מצב נוכחי פשוט
    private String currentFilter = "inbox";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadThemePreference();
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

    private void loadThemePreference() {
        SharedPreferences prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        int nightMode = prefs.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(nightMode);
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
        ImageView ivProfileImage = headerView.findViewById(R.id.ivProfileImage); // הוסף את זה
        Button btnCreateNewLabel = headerView.findViewById(R.id.btnCreateNewLabel);



        // Set current user email
        String currentUserEmail = TokenManager.getCurrentUserEmail(this);
        String currentUserId = TokenManager.getCurrentUserId(this); // הוסף את זה

        System.out.println("=== NAVIGATION HEADER DEBUG ===");
        System.out.println("User email: " + currentUserEmail);
        System.out.println("User ID: " + currentUserId);

        if (currentUserEmail != null) {
            tvUserEmail.setText(currentUserEmail);

            // הוסף את זה:
            if (currentUserId != null && ivProfileImage != null) {
                loadProfileImage(currentUserId, ivProfileImage);
            } else {
                System.out.println("User ID is null or ImageView not found - cannot load avatar");
            }
        }

        // Setup create label button
        btnCreateNewLabel.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            showCreateLabelDialog();
        });
    }

    // הוסף את זה ל-InboxActivity.java

    private void loadProfileImage(String userId, ImageView imageView) {
        System.out.println("=== LOADING AVATAR FOR USER: " + userId + " ===");
        System.out.println("ImageView: " + imageView.toString());

        UserAPI userAPI = BackendClient.get(this).create(UserAPI.class);

        userAPI.getAvatar(userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                System.out.println("=== AVATAR API RESPONSE ===");
                System.out.println("Response code: " + response.code());
                System.out.println("Response successful: " + response.isSuccessful());
                System.out.println("Response body exists: " + (response.body() != null));

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        byte[] imageBytes = response.body().bytes();
                        System.out.println("✓ Image loaded successfully, size: " + imageBytes.length + " bytes");

                        // בדיקת magic bytes - האם זה באמת PNG?
                        if (imageBytes.length >= 8) {
                            String header = String.format("%02X%02X%02X%02X%02X%02X%02X%02X",
                                    imageBytes[0], imageBytes[1], imageBytes[2], imageBytes[3],
                                    imageBytes[4], imageBytes[5], imageBytes[6], imageBytes[7]);
                            System.out.println("Image header (first 8 bytes): " + header);

                            // PNG header should be: 89504E470D0A1A0A
                            boolean isPNG = header.equals("89504E470D0A1A0A");
                            System.out.println("Is valid PNG: " + isPNG);
                        }

                        // נסה פענוח עם BitmapFactory options
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true; // רק מידע, לא פענוח
                        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);

                        System.out.println("Image format: " + options.outMimeType);
                        System.out.println("Image dimensions: " + options.outWidth + "x" + options.outHeight);

                        // עכשיו פענוח אמיתי
                        options.inJustDecodeBounds = false;
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);

                        System.out.println("✓ Bitmap decoded successfully: " + (bitmap != null));

                        if (bitmap != null) {
                            System.out.println("Bitmap config: " + bitmap.getConfig());
                            System.out.println("Bitmap size: " + bitmap.getWidth() + "x" + bitmap.getHeight());

                            runOnUiThread(() -> {
                                System.out.println("=== SETTING IMAGE ON UI THREAD ===");
                                System.out.println("ImageView before: " + imageView.getDrawable());

                                // שים תמונה עגולה
                                Bitmap circularBitmap = createCircularBitmap(bitmap);
                                imageView.setImageBitmap(circularBitmap);

                                System.out.println("✓ ImageView updated successfully");
                                System.out.println("ImageView after: " + imageView.getDrawable());
                            });
                        } else {
                            System.out.println("✗ Bitmap decoding failed - using default image");
                            runOnUiThread(() -> {
                                imageView.setImageResource(R.drawable.ic_account_circle);
                            });
                        }
                    } catch (Exception e) {
                        System.out.println("✗ Exception in image processing: " + e.getMessage());
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            imageView.setImageResource(R.drawable.ic_account_circle);
                        });
                    }
                } else {
                    System.out.println("✗ API response failed or empty body");
                    System.out.println("Response code: " + response.code());
                    System.out.println("Response message: " + response.message());
                    runOnUiThread(() -> {
                        imageView.setImageResource(R.drawable.ic_account_circle);
                    });
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                System.out.println("=== AVATAR API FAILURE ===");
                System.out.println("✗ Failed to load avatar: " + t.getMessage());
                t.printStackTrace();
                runOnUiThread(() -> {
                    imageView.setImageResource(R.drawable.ic_account_circle);
                });
            }
        });
    }

    // גם ודא שהפונקציה createCircularBitmap עובדת טוב
    private Bitmap createCircularBitmap(Bitmap bitmap) {
        System.out.println("=== CREATING CIRCULAR BITMAP ===");
        System.out.println("Input bitmap: " + bitmap.getWidth() + "x" + bitmap.getHeight());

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = Math.min(width, height);

        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        canvas.drawBitmap(bitmap, (size - width) / 2f, (size - height) / 2f, paint);

        System.out.println("✓ Circular bitmap created: " + output.getWidth() + "x" + output.getHeight());
        return output;
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

    // החלף את המתודה addCustomLabelsToMenu הקיימת בזו:

    private void addCustomLabelsToMenu(List<Label> allLabels) {
        // סנן רק תוויות מותאמות (לא מערכת)
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

            // ** החדש: הוסף long-click listener **
            setupLongClickForCustomLabel(item, label);
        }
    }

    // מתודה חדשה לטיפול ב-long click על תוויות מותאמות
    private void setupLongClickForCustomLabel(MenuItem menuItem, Label label) {
        // למצער Android לא תומך ב-long click ישירות על MenuItem
        // נשתמש בפתרון חלופי - נוסיף ContextMenu בלחיצה רגילה אם זה תווית מותאמת

        // נשמור את ה-label במקום נגיש כדי לטפל בו בonNavigationItemSelected
        menuItem.getIntent(); // ניצור Intent חדש לשמירה
        if (menuItem.getIntent() == null) {
            menuItem.setIntent(new android.content.Intent());
        }
        menuItem.getIntent().putExtra("custom_label_id", label.id);
        menuItem.getIntent().putExtra("custom_label_name", label.name);
    }

    // עדכון onNavigationItemSelected לטיפול בתוויות מותאמות:
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
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

            // דלג על הכותרת "תוויות אישיות"
            if (labelName.equals("תוויות אישיות")) {
                return true;
            }

            // בדוק אם יש מידע על תווית מותאמת
            if (item.getIntent() != null &&
                    item.getIntent().hasExtra("custom_label_id")) {

                String labelId = item.getIntent().getStringExtra("custom_label_id");

                // הצג תפריט אפשרויות לתווית מותאמת
                showCustomLabelOptions(labelId, labelName);
                return true;
            } else {
                // תווית רגילה - פתח אותה
                viewModel.loadEmailsByLabel(labelName, labelName);
                return true;
            }
        }

        return false;
    }

    // מתודה חדשה להצגת אפשרויות לתווית מותאמת
    private void showCustomLabelOptions(String labelId, String labelName) {
        String[] options = {"הצג מיילים", "ערוך תווית", "מחק תווית"};

        new AlertDialog.Builder(this)
                .setTitle("'" + labelName + "'")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // הצג מיילים
                            viewModel.loadEmailsByLabel(labelName, labelName);
                            break;
                        case 1: // ערוך תווית
                            showEditLabelDialog(labelId, labelName);
                            break;
                        case 2: // מחק תווית
                            showDeleteLabelDialog(labelId, labelName);
                            break;
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    // מתודה לעריכת תווית
    private void showEditLabelDialog(String labelId, String currentName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("עריכת תווית");

        final EditText input = new EditText(this);
        input.setText(currentName);
        input.setSelection(currentName.length());
        input.setPadding(32, 32, 32, 32);
        builder.setView(input);

        builder.setPositiveButton("עדכן", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                if (!newName.equals(currentName)) {
                    updateLabel(labelId, newName);
                } else {
                    Toast.makeText(this, "לא בוצעו שינויים", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "יש להזין שם תווית", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("ביטול", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        input.requestFocus();
    }

    // מתודה למחיקת תווית
    private void showDeleteLabelDialog(String labelId, String labelName) {
        new AlertDialog.Builder(this)
                .setTitle("מחיקת תווית")
                .setMessage("האם אתה בטוח שברצונך למחוק את התווית '" + labelName + "'?\n\nהתווית תוסר מכל המיילים הקיימים.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("מחק", (dialog, which) -> deleteLabel(labelId, labelName))
                .setNegativeButton("ביטול", null)
                .show();
    }

    // מתודה לעדכון תווית בשרת
    private void updateLabel(String labelId, String newName) {
        LabelAPI labelAPI = BackendClient.get(this).create(LabelAPI.class);
        UpdateLabelRequest request = new UpdateLabelRequest(newName);

        labelAPI.updateLabel(labelId, request).enqueue(new Callback<Label>() {
            @Override
            public void onResponse(Call<Label> call, Response<Label> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(InboxActivity.this,
                            "תווית עודכנה ל-'" + response.body().name + "'", Toast.LENGTH_SHORT).show();
                    refreshNavigationMenu(); // רענן תפריט
                } else {
                    handleLabelError("עדכון", response.code());
                }
            }

            @Override
            public void onFailure(Call<Label> call, Throwable t) {
                Toast.makeText(InboxActivity.this,
                        "שגיאה בחיבור לשרת: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // מתודה למחיקת תווית בשרת
    private void deleteLabel(String labelId, String labelName) {
        LabelAPI labelAPI = BackendClient.get(this).create(LabelAPI.class);

        labelAPI.deleteLabel(labelId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(InboxActivity.this,
                            "תווית '" + labelName + "' נמחקה בהצלחה", Toast.LENGTH_SHORT).show();

                    String currentFilter = viewModel.getCurrentFilter().getValue();
                    // השווה גם במקרה המקורי וגם בלווארקייס
                    if (labelName.equalsIgnoreCase(currentFilter) ||
                            labelName.toLowerCase().equals(currentFilter)) {
                        viewModel.loadEmails(); // חזור לדואר נכנס
                        Toast.makeText(InboxActivity.this,
                                "התווית נמחקה - מעבר לדואר נכנס", Toast.LENGTH_SHORT).show();
                    }

                    refreshNavigationMenu(); // רענן תפריט
                } else {
                    handleLabelError("מחיקה", response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(InboxActivity.this,
                        "שגיאה בחיבור לשרת: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // מתודה לטיפול בשגיאות תוויות
    private void handleLabelError(String action, int responseCode) {
        String message;
        switch (responseCode) {
            case 404:
                message = "התווית לא נמצאה";
                break;
            case 409:
                message = "תווית עם שם זה כבר קיימת";
                break;
            case 403:
                message = "אין הרשאה לבצע פעולה זו";
                break;
            default:
                message = "שגיאה ב" + action + " תווית (קוד " + responseCode + ")";
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private void refreshNavigationMenu() {
        // רענון תוויות אחרי יצירת תווית חדשה
        loadCustomLabels();
    }

    private void handleEditDraft(Email email) {
        Intent intent = new Intent(this, ComposeActivity.class);
        intent.putExtra("is_draft", true);
        intent.putExtra("draft_id", email.id);
        intent.putExtra("draft_to", email.recipients != null ? String.join(", ", email.recipients) : "");
        intent.putExtra("draft_subject", email.subject);
        intent.putExtra("draft_content", email.content);
        startActivity(intent);
    }

    private void setupRecyclerView() {
        String currentUserEmail = TokenManager.getCurrentUserEmail(this);
        System.out.println("Current user email from token: " + currentUserEmail);

        adapter = new EmailAdapter(email -> {
            // בדוק אם זה טיוטה
            boolean isDraft = (email.labels != null && email.labels.contains("drafts"));

            if (isDraft) {
                // טיוטה - פתח עריכה
                handleEditDraft(email);
            } else {
                // מייל רגיל - הצג פרטים
                Intent intent = new Intent(this, EmailDetailActivity.class);
                intent.putExtra("email_id", email.id);
                intent.putExtra("sender", email.sender);
                intent.putExtra("subject", email.subject);
                intent.putExtra("content", email.content);
                intent.putExtra("timestamp", email.timestamp);
                startActivity(intent);
            }
        }, currentUserEmail);

// שאר הlisteners
        adapter.setDeleteListener(this::handleEmailDelete);
        adapter.setStarListener(this::handleEmailStar);
        adapter.setEditDraftListener(this::handleEditDraft);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void handleEmailDelete(Email email) {
        // לוגיקה פשוטה על פי השרת:
        // אם אנחנו בתווית trash → מחיקה סופית (DELETE API)
        // אחרת → החלפת כל התוויות ב-"trash" בלבד

        if ("trash".equals(currentFilter)) {
            // באשפה - אשר מחיקה סופית
            showPermanentDeleteConfirmation(email);
        } else {
            // במצב רגיל - העבר לאשפה (החלף את כל התוויות ב-trash)
            viewModel.moveToTrash(email.id, success -> {
                if (success) {
                    Toast.makeText(this, "המייל הועבר לאשפה", Toast.LENGTH_SHORT).show();
                    viewModel.refreshCurrentFilter(); // רענן רשימה
                } else {
                    Toast.makeText(this, "שגיאה בהעברת המייל לאשפה", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showPermanentDeleteConfirmation(Email email) {
        new AlertDialog.Builder(this)
                .setTitle("מחיקה סופית")
                .setMessage("האם אתה בטוח שברצונך למחוק את המייל לצמיתות?\nפעולה זו לא ניתנת לביטול!")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("מחק לצמיתות", (dialog, which) -> {
                    // השתמש ב-DELETE API
                    viewModel.deleteEmail(email.id, success -> {
                        if (success) {
                            Toast.makeText(InboxActivity.this, "המייל נמחק לצמיתות", Toast.LENGTH_SHORT).show();
                            viewModel.refreshCurrentFilter(); // רענן רשימה
                        } else {
                            Toast.makeText(InboxActivity.this, "שגיאה במחיקת המייל", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("ביטול", null)
                .show();
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

        // Observer לעדכון כותרת המסך
        viewModel.getCurrentFilterDisplayName().observe(this, displayName -> {
            TextView tvWelcome = findViewById(R.id.tvWelcome);
            if (tvWelcome != null && displayName != null) {
                tvWelcome.setText(displayName);
            }
        });

        // Observer למעקב אחר הפילטר הנוכחי
        viewModel.getCurrentFilter().observe(this, filter -> {
            currentFilter = filter != null ? filter : "inbox";
            System.out.println("Current filter changed to: " + currentFilter);
        });

        // הוסף בתוך setupObservers()
        viewModel.getSearchResults().observe(this, searchResults -> {
            if (searchResults != null) {
                adapter.updateEmails(searchResults);
                System.out.println("Search results: " + searchResults.size() + " emails found");
            }
        });

        viewModel.getIsSearching().observe(this, isSearching -> {
            if (isSearching != null && isSearching) {
                Toast.makeText(this, "מחפש...", Toast.LENGTH_SHORT).show();
            }
        });

        System.out.println("Observer setup complete");
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refreshCurrentFilter();
    }

    private void setupFab(FloatingActionButton fab) {
        fab.setOnClickListener(v -> {
            startActivity(new Intent(this, ComposeActivity.class));
        });
    }

    private void setupHeader() {
        ImageView ivMenu = findViewById(R.id.ivMenu);
        ImageView ivThemeToggle = findViewById(R.id.ivThemeToggle);
        TextView tvProfileAvatar = findViewById(R.id.tvProfileAvatar);
        LinearLayout searchBar = findViewById(R.id.searchBar);
        ImageView ivProfileImage = findViewById(R.id.ivProfileImageHeader ); // זה יתייחס לזה שבshורה העליונה


        // כפתור המבורגר
        if (ivMenu != null) {
            ivMenu.setOnClickListener(v -> {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        // כפתור החלפת ערכת נושא
        if (ivThemeToggle != null) {
            updateThemeIcon(ivThemeToggle);
            ivThemeToggle.setOnClickListener(v -> {
                toggleTheme();
                updateThemeIcon(ivThemeToggle);
            });
        }
        String currentUserId = TokenManager.getCurrentUserId(this);
        System.out.println("=== PROFILE IMAGE DEBUG ===");
        System.out.println("User ID: " + currentUserId);
        System.out.println("ImageView found: " + (ivProfileImage != null));

        if (currentUserId != null && ivProfileImage != null) {
            loadProfileImage(currentUserId, ivProfileImage);
        } else {
            System.out.println("Cannot load profile image");
            if (ivProfileImage != null) {
                ivProfileImage.setImageResource(R.drawable.ic_account_circle);
            }
        }


        // פרופיל אווטר - אפשרויות משתמש (רק רענן והתנתק עכשיו)
        if (tvProfileAvatar != null) {
            String userEmail = TokenManager.getCurrentUserEmail(this);
            if (userEmail != null && !userEmail.isEmpty()) {
                tvProfileAvatar.setText(userEmail.substring(0, 1).toUpperCase());
            }

            tvProfileAvatar.setOnClickListener(v -> showUserProfileMenu());
        }

        // חיפוש
        if (searchBar != null) {
            searchBar.setOnClickListener(v -> showSearchDialog());
        }
    }

    private void updateThemeIcon(ImageView themeIcon) {
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            // מצב כהה - הצג שמש (כדי לעבור למצב בהיר)
            themeIcon.setImageResource(R.drawable.ic_theme_light);
        } else {
            // מצב בהיר - הצג ירח (כדי לעבור למצב כהה)
            themeIcon.setImageResource(R.drawable.ic_theme_dark);
        }
    }

    private void showUserProfileMenu() {
        String[] options = {"רענן מיילים", "התנתק"};

        new AlertDialog.Builder(this)
                .setTitle("אפשרויות משתמש")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // רענן
                            viewModel.refreshCurrentFilter();
                            Toast.makeText(this, "מרענן מיילים...", Toast.LENGTH_SHORT).show();
                            break;
                        case 1: // התנתק
                            showLogoutConfirmation();
                            break;
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void toggleTheme() {
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        int newMode = (currentMode == AppCompatDelegate.MODE_NIGHT_YES) ?
                AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES;

        // שמור את ההעדפה
        SharedPreferences prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        prefs.edit().putInt("night_mode", newMode).apply();

        // החלף את הערכת נושא
        AppCompatDelegate.setDefaultNightMode(newMode);
    }

    private void showUserOptions() {
        String[] options = {"רענן", "התנתק"};

        new AlertDialog.Builder(this)
                .setTitle("אפשרויות")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // רענן
                            viewModel.refreshCurrentFilter();
                            Toast.makeText(this, "מרענן...", Toast.LENGTH_SHORT).show();
                            break;
                        case 1: // התנתק
                            showLogoutConfirmation();
                            break;
                    }
                })
                .show();
    }

    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("חיפוש מיילים");

        final EditText input = new EditText(this);
        input.setHint("הזן מילות חיפוש...");
        input.setPadding(32, 32, 32, 32);
        builder.setView(input);

        builder.setPositiveButton("חפש", (dialog, which) -> {
            String query = input.getText().toString().trim();
            if (!query.isEmpty()) {
                performSearch(query);
            } else {
                Toast.makeText(this, "יש להזין מילות חיפוש", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("ביטול", null);

        builder.setNeutralButton("נקה", (dialog, which) -> {
            clearSearch();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
        input.requestFocus();
    }

    private void performSearch(String query) {
        viewModel.searchEmails(query);
        currentFilter = "search"; // עדכן מצב
        TextView tvWelcome = findViewById(R.id.tvWelcome);
        tvWelcome.setText("תוצאות חיפוש: " + query);
    }

    private void clearSearch() {
        viewModel.clearSearch();
        viewModel.refreshCurrentFilter(); // חזור למצב הקודם
        Toast.makeText(this, "החיפוש נוקה", Toast.LENGTH_SHORT).show();
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


    private void handleEmailStar(Email email) {
        boolean isCurrentlyStarred = email.labels != null && email.labels.contains("starred");

        viewModel.toggleStar(email.id, success -> {
            if (success) {
                if (isCurrentlyStarred) {
                    Toast.makeText(this, "הכוכב הוסר", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "נוסף כוכב", Toast.LENGTH_SHORT).show();
                }
                viewModel.refreshCurrentFilter(); // רענן רשימה
            } else {
                Toast.makeText(this, "שגיאה בעדכון הכוכב", Toast.LENGTH_SHORT).show();
            }
        });
    }

}