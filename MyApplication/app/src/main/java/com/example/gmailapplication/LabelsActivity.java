package com.example.gmailapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.gmailapplication.adapters.LabelsAdapter;
import com.example.gmailapplication.database.entities.LabelEntity;
import com.example.gmailapplication.dialogs.CreateLabelDialog;
import com.example.gmailapplication.repository.LabelRepository;
import com.example.gmailapplication.shared.Label;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class LabelsActivity extends AppCompatActivity implements CreateLabelDialog.CreateLabelListener {

    private RecyclerView recyclerViewLabels;
    private LabelsAdapter labelsAdapter;
    private View emptyState;  // שונה מ-TextView ל-View
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fabAddLabel;
    private Toolbar toolbar;

    private LabelRepository labelRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_labels);

        initViews();
        setupToolbar();
        setupRepository();
        setupRecyclerView();
        setupSwipeRefresh();
        setupFab();
        setupSearchAndSettings();
        observeLabels();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerViewLabels = findViewById(R.id.recyclerViewLabels);
        emptyState = findViewById(R.id.emptyState);  // שונה מ-tvEmpty
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        fabAddLabel = findViewById(R.id.fabAddLabel);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("ניהול תוויות");
        }
    }

    private void setupRepository() {
        labelRepository = LabelRepository.getInstance(this);
    }

    private void setupRecyclerView() {
        recyclerViewLabels.setLayoutManager(new LinearLayoutManager(this));
        labelsAdapter = new LabelsAdapter(new ArrayList<>(), new LabelsAdapter.OnLabelClickListener() {
            @Override
            public void onLabelClick(LabelEntity label) {
                openEmailsWithLabel(label);
            }

            @Override
            public void onLabelLongClick(LabelEntity label) {
                showLabelActionsMenu(label);
            }

            @Override
            public void onEditLabel(LabelEntity label) {
                editLabel(label);
            }

            @Override
            public void onDeleteLabel(LabelEntity label) {
                deleteLabel(label);
            }
        });
        recyclerViewLabels.setAdapter(labelsAdapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            labelRepository.fetchLabelsFromServer();
        });

        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );
    }

    private void setupFab() {
        fabAddLabel.setOnClickListener(v -> showCreateLabelDialog());
    }

    // הוספת setup לכפתורי החיפוש וההגדרות
    private void setupSearchAndSettings() {
        findViewById(R.id.ivSearch).setOnClickListener(v -> {
            // פונקציונליות חיפוש תוויות
            showSearchLabelsDialog();
        });

        findViewById(R.id.ivSettings).setOnClickListener(v -> {
            // הגדרות תוויות
            showLabelSettings();
        });
    }

    private void showSearchLabelsDialog() {
        // TODO: מימוש חיפוש תוויות
        Toast.makeText(this, "חיפוש תוויות ימומש בקרוב", Toast.LENGTH_SHORT).show();
    }

    private void showLabelSettings() {
        // TODO: מימוש הגדרות תוויות
        Toast.makeText(this, "הגדרות תוויות יממושו בקרוב", Toast.LENGTH_SHORT).show();
    }

    private void observeLabels() {
        labelRepository.getAllLabels().observe(this, labels -> {
            if (labels != null) {
                labelsAdapter.updateLabels(labels);
                updateEmptyState(labels.isEmpty());
            }
        });

        labelRepository.isLoading().observe(this, isLoading -> {
            swipeRefreshLayout.setRefreshing(isLoading != null ? isLoading : false);
        });

        labelRepository.getLastError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, "שגיאה: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            if (recyclerViewLabels != null) recyclerViewLabels.setVisibility(View.GONE);
        } else {
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            if (recyclerViewLabels != null) recyclerViewLabels.setVisibility(View.VISIBLE);
        }
    }

    private void showCreateLabelDialog() {
        CreateLabelDialog dialog = new CreateLabelDialog();
        dialog.setCreateLabelListener(this);
        dialog.show(getSupportFragmentManager(), "CreateLabelDialog");
    }

    private void editLabel(LabelEntity label) {
        if (label.isSystemLabel) {
            Toast.makeText(this, "לא ניתן לערוך תוויות מערכת", Toast.LENGTH_SHORT).show();
            return;
        }

        CreateLabelDialog dialog = CreateLabelDialog.editLabel(label);
        dialog.setCreateLabelListener(this);
        dialog.show(getSupportFragmentManager(), "EditLabelDialog");
    }

    private void deleteLabel(LabelEntity label) {
        if (label.isSystemLabel) {
            Toast.makeText(this, "לא ניתן למחוק תוויות מערכת", Toast.LENGTH_SHORT).show();
            return;
        }

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("מחיקת תווית");
        builder.setMessage("האם אתה בטוח שברצונך למחוק את התווית '" + label.name + "'?");
        builder.setPositiveButton("מחק", (dialog, which) -> {
            labelRepository.deleteLabel(label.id, new LabelRepository.DeleteLabelCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        Toast.makeText(LabelsActivity.this, "התווית נמחקה בהצלחה", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(LabelsActivity.this, "שגיאה במחיקת התווית: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

    private void showLabelActionsMenu(LabelEntity label) {
        if (label.isSystemLabel) {
            openEmailsWithLabel(label);
            return;
        }

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(label.name);

        String[] actions = {"הצג מיילים", "ערוך תווית", "מחק תווית"};
        builder.setItems(actions, (dialog, which) -> {
            switch (which) {
                case 0:
                    openEmailsWithLabel(label);
                    break;
                case 1:
                    editLabel(label);
                    break;
                case 2:
                    deleteLabel(label);
                    break;
            }
        });
        builder.show();
    }

    private void openEmailsWithLabel(LabelEntity label) {
        Intent intent = new Intent(this, InboxActivity.class);
        intent.putExtra("filter_by_label", label.name);
        intent.putExtra("label_id", label.id);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.labels_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_refresh) {
            labelRepository.fetchLabelsFromServer();
            return true;
        } else if (itemId == R.id.action_add_label) {
            showCreateLabelDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // CreateLabelDialog.CreateLabelListener implementation
    @Override
    public void onLabelCreated(String labelName, String color) {
        labelRepository.createLabel(labelName, color, new LabelRepository.CreateLabelCallback() {
            @Override
            public void onSuccess(Label label) {
                runOnUiThread(() -> {
                    Toast.makeText(LabelsActivity.this, "התווית נוצרה בהצלחה", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(LabelsActivity.this, "שגיאה ביצירת התווית: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onLabelEdited(String labelId, String newName, String color) {
        // TODO: מימוש עריכת תווית
        Toast.makeText(this, "עריכה תמומש בקרוב", Toast.LENGTH_SHORT).show();
    }
}