package com.example.gmailapplication.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.gmailapplication.R;
import com.example.gmailapplication.database.entities.LabelEntity;

public class CreateLabelDialog extends DialogFragment {

    private EditText etLabelName;
    private GridLayout colorGrid;
    private TextView tvSelectedColor;
    private Button btnCancel, btnCreate;

    private CreateLabelListener listener;
    private String selectedColor = "#2196F3"; // כחול ברירת מחדל
    private boolean isEditMode = false;
    private LabelEntity labelToEdit;

    // צבעים זמינים
    private final String[] availableColors = {
            "#2196F3", // כחול
            "#4CAF50", // ירוק
            "#FF9800", // כתום
            "#F44336", // אדום
            "#9C27B0", // סגול
            "#607D8B", // אפור כחלחל
            "#795548", // חום
            "#E91E63", // ורוד
            "#009688", // טורקיז
            "#FF5722", // אדום כתום
            "#3F51B5", // כחול כהה
            "#8BC34A"  // ירוק בהיר
    };

    public interface CreateLabelListener {
        void onLabelCreated(String labelName, String color);
        void onLabelEdited(String labelId, String newName, String color);
    }

    public static CreateLabelDialog newInstance() {
        return new CreateLabelDialog();
    }

    public static CreateLabelDialog editLabel(LabelEntity label) {
        CreateLabelDialog dialog = new CreateLabelDialog();
        dialog.isEditMode = true;
        dialog.labelToEdit = label;
        dialog.selectedColor = label.color != null ? label.color : "#2196F3";
        return dialog;
    }

    public void setCreateLabelListener(CreateLabelListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_create_label, null);

        initViews(view);
        setupColorGrid();
        setupButtons();

        if (isEditMode && labelToEdit != null) {
            setupEditMode();
        }

        builder.setView(view);
        builder.setTitle(isEditMode ? "עריכת תווית" : "יצירת תווית חדשה");

        return builder.create();
    }

    private void initViews(View view) {
        etLabelName = view.findViewById(R.id.etLabelName);
        colorGrid = view.findViewById(R.id.colorGrid);
        tvSelectedColor = view.findViewById(R.id.tvSelectedColor);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnCreate = view.findViewById(R.id.btnCreate);
    }

    private void setupColorGrid() {
        colorGrid.removeAllViews();

        int columns = 4;
        colorGrid.setColumnCount(columns);

        for (int i = 0; i < availableColors.length; i++) {
            String color = availableColors[i];
            View colorView = createColorView(color);
            colorGrid.addView(colorView);
        }

        // עדכן תצוגת הצבע הנבחר
        updateSelectedColorDisplay();
    }

    private View createColorView(String colorHex) {
        View colorView = new View(getActivity());

        // הגדר גודל
        int size = (int) (40 * getResources().getDisplayMetrics().density);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = size;
        params.height = size;
        params.setMargins(8, 8, 8, 8);
        colorView.setLayoutParams(params);

        // הגדר צבע רקע
        try {
            int color = Color.parseColor(colorHex);
            colorView.setBackgroundColor(color);
        } catch (Exception e) {
            colorView.setBackgroundColor(Color.BLUE);
        }

        // הוסף border אם זה הצבע הנבחר
        if (colorHex.equals(selectedColor)) {
            colorView.setPadding(4, 4, 4, 4);
            colorView.setBackground(createSelectedBackground(colorHex));
        }

        // הוסף click listener
        colorView.setOnClickListener(v -> {
            selectedColor = colorHex;
            updateSelectedColorDisplay();
            setupColorGrid(); // רענן את הגריד
        });

        return colorView;
    }

    private android.graphics.drawable.Drawable createSelectedBackground(String colorHex) {
        try {
            int color = Color.parseColor(colorHex);
            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setColor(color);
            drawable.setStroke(4, Color.BLACK);
            drawable.setCornerRadius(8);
            return drawable;
        } catch (Exception e) {
            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setColor(Color.BLUE);
            drawable.setStroke(4, Color.BLACK);
            return drawable;
        }
    }

    private void updateSelectedColorDisplay() {
        if (tvSelectedColor != null) {
            try {
                int color = Color.parseColor(selectedColor);
                tvSelectedColor.setBackgroundColor(color);
                tvSelectedColor.setText("צבע נבחר");
            } catch (Exception e) {
                tvSelectedColor.setBackgroundColor(Color.BLUE);
            }
        }
    }

    private void setupButtons() {
        btnCancel.setOnClickListener(v -> dismiss());

        btnCreate.setText(isEditMode ? "עדכן" : "צור");
        btnCreate.setOnClickListener(v -> {
            String labelName = etLabelName.getText().toString().trim();

            if (validateInput(labelName)) {
                if (isEditMode) {
                    handleEditLabel(labelName);
                } else {
                    handleCreateLabel(labelName);
                }
            }
        });
    }

    private void setupEditMode() {
        if (labelToEdit != null) {
            etLabelName.setText(labelToEdit.name);
            etLabelName.setSelection(labelToEdit.name.length());

            if (labelToEdit.color != null) {
                selectedColor = labelToEdit.color;
            }

            updateSelectedColorDisplay();
        }
    }

    private boolean validateInput(String labelName) {
        if (TextUtils.isEmpty(labelName)) {
            etLabelName.setError("יש להזין שם לתווית");
            etLabelName.requestFocus();
            return false;
        }

        if (labelName.length() > 50) {
            etLabelName.setError("שם התווית ארוך מדי (מקסימום 50 תווים)");
            etLabelName.requestFocus();
            return false;
        }

        // בדיקת תווים לא חוקיים
        if (labelName.contains("/") || labelName.contains("\\") ||
                labelName.contains("<") || labelName.contains(">")) {
            etLabelName.setError("שם התווית מכיל תווים לא חוקיים");
            etLabelName.requestFocus();
            return false;
        }

        return true;
    }

    private void handleCreateLabel(String labelName) {
        if (listener != null) {
            listener.onLabelCreated(labelName, selectedColor);
            dismiss();
        }
    }

    private void handleEditLabel(String newName) {
        if (listener != null && labelToEdit != null) {
            listener.onLabelEdited(labelToEdit.id, newName, selectedColor);
            dismiss();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // הגדר גודל הדיאלוג
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}