package com.example.gmailapplication.viewmodels;

import android.app.Application;
import android.text.TextUtils;
import android.util.Patterns;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gmailapplication.API.BackendClient;
import com.example.gmailapplication.API.UserAPI;
import com.example.gmailapplication.shared.LoginRequest;
import com.example.gmailapplication.shared.LoginResponse;
import com.example.gmailapplication.shared.UserDto;

import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginViewModel extends AndroidViewModel {

    // API client
    private UserAPI userAPI;

    // Form data - survives configuration changes
    private MutableLiveData<String> email = new MutableLiveData<>("");
    private MutableLiveData<String> password = new MutableLiveData<>("");

    // UI State
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private MutableLiveData<String> errorMessage = new MutableLiveData<>("");
    private MutableLiveData<String> successMessage = new MutableLiveData<>("");

    // Validation errors
    private MutableLiveData<String> emailError = new MutableLiveData<>("");
    private MutableLiveData<String> passwordError = new MutableLiveData<>("");

    // Login result - just use LoginResponse directly
    private MutableLiveData<LoginResponse> loginResult = new MutableLiveData<>();
    private MutableLiveData<UserDto> currentUser = new MutableLiveData<>();

    public LoginViewModel(@NonNull Application application) {
        super(application);
        userAPI = BackendClient.get(application).create(UserAPI.class);
    }

    // --- Getters for LiveData (read-only) ---
    public LiveData<String> getEmail() { return email; }
    public LiveData<String> getPassword() { return password; }

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }

    public LiveData<String> getEmailError() { return emailError; }
    public LiveData<String> getPasswordError() { return passwordError; }

    public LiveData<LoginResponse> getLoginResult() { return loginResult; }
    public LiveData<UserDto> getCurrentUser() { return currentUser; }

    // --- Setters for form data ---
    public void setEmail(String value) {
        email.setValue(value);
        if (!TextUtils.isEmpty(value)) {
            emailError.setValue(""); // Clear error when user types
        }
    }

    public void setPassword(String value) {
        password.setValue(value);
        if (!TextUtils.isEmpty(value)) {
            passwordError.setValue(""); // Clear error when user types
        }
    }

    // --- Pre-fill email (for registration success) ---
    public void prefillEmail(String emailValue) {
        if (!TextUtils.isEmpty(emailValue)) {
            email.setValue(emailValue);
        }
    }

    // --- Clear all errors ---
    public void clearAllErrors() {
        emailError.setValue("");
        passwordError.setValue("");
        errorMessage.setValue("");
    }

    // --- Validation methods ---
    public boolean validateAll() {
        boolean isValid = true;
        clearAllErrors();

        // Email validation
        String emailVal = email.getValue();
        if (TextUtils.isEmpty(emailVal)) {
            emailError.setValue("יש להזין כתובת מייל");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailVal.trim()).matches()) {
            emailError.setValue("כתובת מייל לא תקינה");
            isValid = false;
        }

        // Password validation
        String passwordVal = password.getValue();
        if (TextUtils.isEmpty(passwordVal)) {
            passwordError.setValue("יש להזין סיסמה");
            isValid = false;
        } else if (passwordVal.length() < 6) {
            passwordError.setValue("סיסמה חייבת להכיל לפחות 6 תווים");
            isValid = false;
        }

        return isValid;
    }

    // --- Login method ---
    // --- Login method ---
    public void login() {
        if (!validateAll()) {
            return;
        }

        isLoading.setValue(true);
        errorMessage.setValue("");
        successMessage.setValue("");

        LoginRequest request = buildLoginRequest();

        userAPI.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                isLoading.setValue(false);

                if (response.isSuccessful()) {
                    LoginResponse result = response.body();

                    // Debug: Print response
                    System.out.println("=== LOGIN RESPONSE DEBUG ===");
                    System.out.println("Response code: " + response.code());
                    System.out.println("Token: " + (result != null ? result.token : "null"));
                    System.out.println("ExpiresIn: " + (result != null ? result.expiresIn : "null"));

                    if (result != null && result.token != null) {
                        // Now fetch user data with the token
                        fetchUserData(result);
                    } else {
                        errorMessage.setValue("תגובת שרת לא תקינה - אין token");
                    }
                } else {
                    System.out.println("Response not successful: " + response.code());
                    handleServerError(response);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                isLoading.setValue(false);
                System.out.println("Login failed: " + t.getMessage());
                errorMessage.setValue("שגיאת חיבור: " + t.getMessage());
            }
        });
    }

    // --- Fetch user data after successful login ---
    // החלף את השיטה fetchUserData() הקיימת בקובץ LoginViewModel.java עם הקוד הזה:

    private void fetchUserData(LoginResponse loginResponse) {
        com.example.gmailapplication.shared.TokenManager.save(getApplication(), loginResponse.token);

        String authHeader = "Bearer " + loginResponse.token;

        userAPI.getCurrentUser(authHeader).enqueue(new Callback<UserDto>() {
            @Override
            public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                System.out.println("=== RESPONSE DEBUG ===");
                System.out.println("Response code: " + response.code());
                System.out.println("Response successful: " + response.isSuccessful());

                if (response.isSuccessful()) {
                    UserDto user = response.body();
                    if (user != null) {
                        System.out.println("User email: " + user.email);
                        System.out.println("User name: " + user.getFullName());
                        System.out.println("User ID: " + user.getId());
                        System.out.println("Raw id field: " + user.id);
                        System.out.println("Raw _id field: " + user._id);

                        // שמירת נתוני משתמש ב-SharedPreferences
                        android.content.SharedPreferences prefs = getApplication().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE);
                        prefs.edit()
                                .putString("user_id", user.getId())
                                .putString("user_email", user.email)
                                .putString("user_name", user.getFullName())
                                .apply();

                        System.out.println("Saved user ID to SharedPreferences: " + user.getId());

                        currentUser.setValue(user);
                        loginResult.setValue(loginResponse);
                        successMessage.setValue("התחברות מוצלחת!");
                    } else {
                        errorMessage.setValue("שגיאה בקבלת נתוני משתמש - response body is null");
                    }
                } else {
                    errorMessage.setValue("שגיאה בקבלת נתוני משתמש - קוד " + response.code());
                }
            }

            @Override
            public void onFailure(Call<UserDto> call, Throwable t) {
                errorMessage.setValue("שגיאה בקבלת נתוני משתמש: " + t.getMessage());
            }
        });
    }

    // --- Build login request ---
    private LoginRequest buildLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.email = email.getValue().trim().toLowerCase(Locale.US);
        request.password = password.getValue();
        return request;
    }

    // --- Server error handling ---
    private void handleServerError(Response<LoginResponse> response) {
        try {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "שגיאה לא ידועה";

            switch (response.code()) {
                case 400:
                    errorMessage.setValue("נתונים לא תקינים");
                    break;
                case 401:
                    errorMessage.setValue("כתובת מייל או סיסמה שגויים");
                    emailError.setValue("פרטי התחברות שגויים");
                    passwordError.setValue("פרטי התחברות שגויים");
                    break;
                case 403:
                    errorMessage.setValue("חשבון חסום או לא פעיל");
                    break;
                case 404:
                    errorMessage.setValue("משתמש לא נמצא");
                    emailError.setValue("משתמש לא קיים במערכת");
                    break;
                case 429:
                    errorMessage.setValue("יותר מדי נסיונות התחברות - אנא נסה מאוחר יותר");
                    break;
                case 500:
                    errorMessage.setValue("שגיאת שרת פנימית - אנא נסה מאוחר יותר");
                    break;
                default:
                    errorMessage.setValue("התחברות נכשלה (שגיאה " + response.code() + ")");
            }
        } catch (Exception e) {
            errorMessage.setValue("התחברות נכשלה - אנא נסה שוב");
        }
    }

    // --- Reset password functionality ---
    public void requestPasswordReset() {
        String emailVal = email.getValue();
        if (TextUtils.isEmpty(emailVal) || !Patterns.EMAIL_ADDRESS.matcher(emailVal.trim()).matches()) {
            emailError.setValue("יש להזין כתובת מייל תקינה לאיפוס סיסמה");
            return;
        }

        isLoading.setValue(true);
        errorMessage.setValue("");

        // Create password reset request
        // You'll need to add this to your API interface
        /*
        userAPI.requestPasswordReset(emailVal.trim()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                isLoading.setValue(false);
                if (response.isSuccessful()) {
                    successMessage.setValue("קישור לאיפוס סיסמה נשלח למייל שלך");
                } else {
                    errorMessage.setValue("שגיאה בשליחת איפוס סיסמה");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("שגיאה בשליחת איפוס סיסמה: " + t.getMessage());
            }
        });
        */

        // For now, just show a message
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            isLoading.setValue(false);
            successMessage.setValue("קישור לאיפוס סיסמה נשלח למייל " + emailVal);
        }, 1000);
    }

    // --- Clear login state ---
    public void clearLoginState() {
        loginResult.setValue(null);
        currentUser.setValue(null);
        email.setValue("");
        password.setValue("");
        clearAllErrors();
    }
}