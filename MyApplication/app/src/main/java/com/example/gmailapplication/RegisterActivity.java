// NOTE: comments in English only
package com.example.gmailapplication;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import com.example.gmailapplication.API.BackendClient;
import com.example.gmailapplication.API.UserAPI;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.widget.Toast; // for success toast

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.gmailapplication.shared.*;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilFirstName, tilLastName, tilEmail, tilPassword, tilConfirm, tilGender, tilBirth, tilPhone;
    private TextInputEditText etFirstName, etLastName, etEmail, etPassword, etConfirm, etBirth, etPhone;
    private AutoCompleteTextView etGender;
    private Button btnPickImage, btnSubmit;
    private TextView tvResult, tvLoginLink;

    private String avatarBase64; // holds base64 of selected image (no header)
    private String avatarMime = "image/png"; // default, will try to infer

    // simple phone regex allowing Israeli and international numbers
    private static final Pattern PHONE_RE = Pattern.compile("^(\\+?972[-\\s]?|0)?[2-9]\\d{7,8}$|^(\\+?[1-9]\\d{7,14})$");

    // Password validation regex - at least 8 chars with letters and numbers
    private static final Pattern PASSWORD_RE = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&]{8,}$");

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onImagePicked);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        bindViews();
        wireGenderDropdown();
        wireBirthPicker();
        wireLiveValidation();
        wirePickImage();
        wireSubmit();
        wireLoginLink();

        // Restore state if needed
        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!TextUtils.isEmpty(avatarBase64)) {
            outState.putString("avatarBase64", avatarBase64);
            outState.putString("avatarMime", avatarMime);
        }
    }

    private void restoreInstanceState(Bundle savedInstanceState) {
        avatarBase64 = savedInstanceState.getString("avatarBase64");
        avatarMime = savedInstanceState.getString("avatarMime", "image/png");
        if (!TextUtils.isEmpty(avatarBase64)) {
            btnPickImage.setText("תמונה נבחרה");
        }
    }

    // --- Bind XML views ---
    private void bindViews() {
        tilFirstName = findViewById(R.id.tilFirstName);
        tilLastName  = findViewById(R.id.tilLastName);
        tilEmail     = findViewById(R.id.tilEmail);
        tilPassword  = findViewById(R.id.tilPassword);
        tilConfirm   = findViewById(R.id.tilConfirm);
        tilGender    = findViewById(R.id.tilGender);
        tilBirth     = findViewById(R.id.tilBirth);
        tilPhone     = findViewById(R.id.tilPhone);

        etFirstName = findViewById(R.id.etFirstName);
        etLastName  = findViewById(R.id.etLastName);
        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        etConfirm   = findViewById(R.id.etConfirm);
        etGender    = findViewById(R.id.etGender);
        etBirth     = findViewById(R.id.etBirth);
        etPhone     = findViewById(R.id.etPhone);

        btnPickImage = findViewById(R.id.btnPickImage);
        btnSubmit    = findViewById(R.id.btnSubmit);
        tvResult     = findViewById(R.id.tvResult);
        tvLoginLink  = findViewById(R.id.tvLoginLink);
    }

    // --- Gender dropdown with both Hebrew and English values ---
    private void wireGenderDropdown() {
        String[] genders = new String[]{"זכר", "נקבה", "אחר", "male", "female", "other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, genders);
        etGender.setAdapter(adapter);
    }

    // --- Date picker for birth date ---
    private void wireBirthPicker() {
        etBirth.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int y = c.get(Calendar.YEAR);
            int m = c.get(Calendar.MONTH);
            int d = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dlg = new DatePickerDialog(this, (DatePicker view, int year, int month, int dayOfMonth) -> {
                String s = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                etBirth.setText(s);
                tilBirth.setError(null);
            }, y, m, d);

            // restrict future dates
            dlg.getDatePicker().setMaxDate(System.currentTimeMillis());
            dlg.show();
        });
    }

    // --- Create TextWatcher for specific layout to prevent memory leaks ---
    private TextWatcher createClearErrorWatcher(final TextInputLayout layout) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                layout.setError(null);
            }
        };
    }

    // --- Enhanced live validation that clears errors while typing ---
    private void wireLiveValidation() {
        etFirstName.addTextChangedListener(createClearErrorWatcher(tilFirstName));
        etLastName.addTextChangedListener(createClearErrorWatcher(tilLastName));
        etEmail.addTextChangedListener(createClearErrorWatcher(tilEmail));
        etPassword.addTextChangedListener(createClearErrorWatcher(tilPassword));
        etConfirm.addTextChangedListener(createClearErrorWatcher(tilConfirm));
        etGender.addTextChangedListener(createClearErrorWatcher(tilGender));
        etBirth.addTextChangedListener(createClearErrorWatcher(tilBirth));
        etPhone.addTextChangedListener(createClearErrorWatcher(tilPhone));
    }

    // --- Image picker (gallery) -> base64 (no data: header) ---
    private void wirePickImage() {
        btnPickImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
    }

    // --- Enhanced image processing with background thread ---
    private void onImagePicked(Uri uri) {
        if (uri == null) return;

        // Show loading state
        btnPickImage.setEnabled(false);
        btnPickImage.setText("טוען תמונה...");

        // Process image in background thread
        new Thread(() -> {
            try {
                byte[] imageBytes = readImageBytes(uri);

                runOnUiThread(() -> {
                    btnPickImage.setEnabled(true);

                    if (imageBytes != null) {
                        avatarBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
                        btnPickImage.setText("תמונה נבחרה");
                        showToast("תמונת פרופיל נבחרה בהצלחה");
                    } else {
                        btnPickImage.setText("בחר תמונת פרופיל");
                        showToast("שגיאה בקריאת התמונה");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnPickImage.setEnabled(true);
                    btnPickImage.setText("בחר תמונת פרופיל");
                    showToast("שגיאה בעיבוד התמונה: " + e.getMessage());
                });
            }
        }).start();
    }

    // --- Helper method to read image bytes ---
    private byte[] readImageBytes(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) return null;

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            byte[] bytes = bos.toByteArray();

            // Size limit: 2MB
            if (bytes.length > 2 * 1024 * 1024) {
                runOnUiThread(() -> showToast("התמונה גדולה מדי (מקסימום 2MB)"));
                return null;
            }

            // Infer mime from content resolver if possible
            String detected = getContentResolver().getType(uri);
            if (!TextUtils.isEmpty(detected)) {
                avatarMime = detected;
            }

            return bytes;
        } catch (IOException e) {
            return null;
        }
    }

    // --- Enhanced submit with better error handling ---
    private void wireSubmit() {
        btnSubmit.setOnClickListener(v -> {
            clearAllErrors();
            if (!validateAll()) return;

            btnSubmit.setEnabled(false);
            tvResult.setText("מבצע רישום...");

            try {
                RegisterRequest request = buildRegisterRequest();
                UserAPI api = BackendClient.get(this).create(UserAPI.class);
                Call<UserDto> call = api.register(request);

                call.enqueue(new Callback<UserDto>() {
                    @Override
                    public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                        btnSubmit.setEnabled(true);

                        if (response.isSuccessful()) {
                            // comments in English only
                            UserDto user = response.body();

// Local fallback from the form, in case server does not return firstName
                            String localFirst = textOf(etFirstName).trim();
                            String displayName =
                                    (user != null && !TextUtils.isEmpty(user.firstName)) ? user.firstName :
                                            (!TextUtils.isEmpty(localFirst)) ? localFirst :
                                                    (user != null && !TextUtils.isEmpty(user.email)) ? user.email :
                                                            "חבר";

                            tvResult.setText("הרישום הושלם בהצלחה!\nברוך הבא " + displayName + "!");
                            // Navigate straight to Login and prefill the email field
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            intent.putExtra("prefill_email", textOf(etEmail).trim().toLowerCase(Locale.US));
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish(); // remove Register from back stack
                            return;

                        } else {
                            handleServerError(response);
                        }
                    }

                    @Override
                    public void onFailure(Call<UserDto> call, Throwable t) {
                        btnSubmit.setEnabled(true);
                        tvResult.setText("הרישום נכשל: " + t.getMessage());
                        System.err.println("Registration error: " + t.getMessage());
                    }
                });

            } catch (Exception e) {
                btnSubmit.setEnabled(true);
                tvResult.setText("שגיאה ביצירת הבקשה: " + e.getMessage());
            }
        });
    }

    // --- Build RegisterRequest object ---
    private RegisterRequest buildRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.firstName = textOf(etFirstName).trim();
        request.lastName = textOf(etLastName).trim();
        request.email = textOf(etEmail).trim().toLowerCase(Locale.US);
        request.password = textOf(etPassword);

        // Optional fields
        String gender = mapGenderToEnglish(textOf(etGender).trim());
        if (!TextUtils.isEmpty(gender)) {
            request.gender = gender;
        }

        String birth = textOf(etBirth).trim();
        if (!TextUtils.isEmpty(birth)) {
            request.birthDate = birth;
        }

        String phone = textOf(etPhone).trim();
        if (!TextUtils.isEmpty(phone)) {
            request.phone = phone;
        }

        // Profile picture
        if (!TextUtils.isEmpty(avatarBase64)) {
            String dataUrl = "data:" + avatarMime + ";base64," + avatarBase64;
            request.profilePicture = dataUrl;
        }

        return request;
    }

    // --- Map Hebrew/English gender to English for API ---
    private String mapGenderToEnglish(String gender) {
        if (TextUtils.isEmpty(gender)) return "";
        String genderLower = gender.toLowerCase().trim();
        switch (genderLower) {
            case "זכר":
            case "male":
                return "male";
            case "נקבה":
            case "female":
                return "female";
            case "אחר":
            case "other":
                return "other";
            default:
                return genderLower;
        }
    }

    // --- Wire login link click ---
    private void wireLoginLink() {
        tvLoginLink.setText("כבר יש לך חשבון? התחבר כאן");
        tvLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Optional: remove this activity from stack
        });
    }

    // --- Enhanced server error handling ---
    private void handleServerError(Response<UserDto> response) {
        try {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "שגיאה לא ידועה";
            System.err.println("Server error: " + errorBody);

            // Try to parse JSON error response
            try {
                JSONObject errorJson = new JSONObject(errorBody);
                String message = errorJson.optString("message", "הרישום נכשל");
                tvResult.setText(message);
                return;
            } catch (JSONException e) {
                // Fall back to HTTP status codes
            }

            switch (response.code()) {
                case 400:
                    tvResult.setText("נתוני רישום לא תקינים");
                    break;
                case 409:
                    tvResult.setText("משתמש זה כבר קיים במערכת");
                    tilEmail.setError("כתובת מייל זו כבר רשומה במערכת");
                    break;
                case 422:
                    tvResult.setText("אימות הנתונים נכשל");
                    break;
                case 500:
                    tvResult.setText("שגיאת שרת פנימית - אנא נסה מאוחר יותר");
                    break;
                default:
                    tvResult.setText("הרישום נכשל (שגיאה " + response.code() + ")");
            }
        } catch (Exception e) {
            tvResult.setText("הרישום נכשל - אנא נסה שוב");
        }
    }

    // --- Enhanced validation with Hebrew messages and stronger password rules ---
    private boolean validateAll() {
        boolean ok = true;

        // Debug: print all values
        System.out.println("=== DEBUG VALIDATION ===");
        System.out.println("First name: '" + textOf(etFirstName).trim() + "'");
        System.out.println("Last name: '" + textOf(etLastName).trim() + "'");
        System.out.println("Email: '" + textOf(etEmail).toLowerCase(Locale.US).trim() + "'");
        System.out.println("Gender: '" + textOf(etGender) + "'");
        System.out.println("Birth: '" + textOf(etBirth) + "'");
        System.out.println("Phone: '" + textOf(etPhone) + "'");

        String first = textOf(etFirstName).trim();
        if (first.length() < 2) {
            tilFirstName.setError("שם פרטי חייב להכיל לפחות 2 תווים");
            ok = false;
        }

        String last = textOf(etLastName).trim();
        if (last.length() < 2) {
            tilLastName.setError("שם משפחה חייב להכיל לפחות 2 תווים");
            ok = false;
        }

        String email = textOf(etEmail).toLowerCase(Locale.US).trim();
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("כתובת מייל לא תקינה");
            ok = false;
        }

        String pass = textOf(etPassword);
        if (!validatePassword(pass)) {
            ok = false;
        }

        String confirm = textOf(etConfirm);
        if (!pass.equals(confirm)) {
            tilConfirm.setError("הסיסמאות אינן זהות");
            ok = false;
        }

        String gender = textOf(etGender);
        if (!TextUtils.isEmpty(gender) && !isValidGender(gender)) {
            tilGender.setError("יש לבחור מין תקין (male/female/other או זכר/נקבה/אחר)");
            ok = false;
        }

        String birth = textOf(etBirth);
        if (!TextUtils.isEmpty(birth)) {
            if (!isValidIsoDate(birth)) {
                tilBirth.setError("תאריך לידה חייב להיות בפורמט YYYY-MM-DD");
                ok = false;
            } else if (!isReasonableBirth(birth)) {
                tilBirth.setError("תאריך לידה לא סביר");
                ok = false;
            }
        }

        String phone = textOf(etPhone);
        if (!TextUtils.isEmpty(phone)) {
            String cleanPhone = phone.replaceAll("[-\\s]", "");
            if (!PHONE_RE.matcher(cleanPhone).matches()) {
                tilPhone.setError("מספר טלפון לא תקין (דוגמה: 053-4302092 או +972534302092)");
                ok = false;
            }
        }

        System.out.println("Validation result: " + ok);
        return ok;
    }

    // --- Enhanced password validation ---
    private boolean validatePassword(String password) {
        if (password.length() < 8) {
            tilPassword.setError("הסיסמה חייבת להכיל לפחות 8 תווים");
            return false;
        }

        if (!PASSWORD_RE.matcher(password).matches()) {
            tilPassword.setError("הסיסמה חייבת להכיל שילוב של אותיות ומספרים");
            return false;
        }

        return true;
    }

    // --- Validate Hebrew gender options ---
    private boolean isValidGender(String gender) {
        if (TextUtils.isEmpty(gender)) return true; // Optional field
        String genderLower = gender.toLowerCase().trim();
        return genderLower.equals("זכר") || genderLower.equals("נקבה") || genderLower.equals("אחר") ||
                genderLower.equals("male") || genderLower.equals("female") || genderLower.equals("other");
    }

    // --- Helper methods ---
    private String textOf(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }

    private String textOf(AutoCompleteTextView et) {
        return et.getText() == null ? "" : et.getText().toString();
    }

    private void clearAllErrors() {
        tilFirstName.setError(null);
        tilLastName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirm.setError(null);
        tilGender.setError(null);
        tilBirth.setError(null);
        tilPhone.setError(null);
    }

    private void showToast(String msg) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
    }

    // --- Date validation methods ---
    private boolean isValidIsoDate(String s) {
        if (TextUtils.isEmpty(s)) return false;
        String[] parts = s.split("-");
        if (parts.length != 3) return false;
        try {
            int y = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int d = Integer.parseInt(parts[2]);
            if (y < 1900 || y > Calendar.getInstance().get(Calendar.YEAR)) return false;
            if (m < 1 || m > 12) return false;
            if (d < 1 || d > 31) return false;
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isReasonableBirth(String iso) {
        if (TextUtils.isEmpty(iso)) return true;
        String[] p = iso.split("-");
        int y = Integer.parseInt(p[0]);
        int m = Integer.parseInt(p[1]) - 1;
        int d = Integer.parseInt(p[2]);

        Calendar birth = Calendar.getInstance();
        birth.set(y, m, d, 0, 0, 0);
        birth.set(Calendar.MILLISECOND, 0);

        Calendar now = Calendar.getInstance();
        if (birth.after(now)) return false;

        Calendar min = Calendar.getInstance();
        min.add(Calendar.YEAR, -120);
        return birth.after(min);
    }
}