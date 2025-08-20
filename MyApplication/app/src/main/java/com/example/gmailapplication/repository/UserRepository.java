package com.example.gmailapplication.repository;
// NOTE: comments in English only
import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gmailapplication.API.BackendClient;
import com.example.gmailapplication.API.UserAPI;
import com.example.gmailapplication.database.AppDB;
import com.example.gmailapplication.database.entities.UserEntity;
import com.example.gmailapplication.local.UserDao;
import com.example.gmailapplication.shared.*;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {

    private final Context appCtx;
    private final UserAPI api;
    private final UserDao dao;
    private final Executor io = Executors.newSingleThreadExecutor();

    private final MutableLiveData<UserEntity> currentUser = new MutableLiveData<>(null);

    public UserRepository(Context context) {
        this.appCtx = context.getApplicationContext();
        this.api = BackendClient.get(appCtx).create(UserAPI.class);
        this.dao = AppDB.get(appCtx).userDao();
    }

    public LiveData<List<UserEntity>> observeAll() { return dao.observeAll(); }

    public LiveData<UserEntity> currentUser() { return currentUser; }

    public void register(RegisterRequest req) {
        api.register(req).enqueue(new Callback<UserDto>() {
            @Override public void onResponse(Call<UserDto> c, Response<UserDto> r) {
                if (!r.isSuccessful() || r.body()==null) return;
                upsertFromDto(r.body());
            }
            @Override public void onFailure(Call<UserDto> c, Throwable t) { /* TODO: log */ }
        });
    }

    public void login(String email, String password) {
        LoginRequest body = new LoginRequest();
        body.email = email; body.password = password;

        api.login(body).enqueue(new Callback<LoginResponse>() {
            @Override public void onResponse(Call<LoginResponse> c, Response<LoginResponse> r) {
                if (!r.isSuccessful() || r.body()==null) return;
                TokenManager.save(appCtx, r.body().token);
                fetchCurrentUser();
            }
            @Override public void onFailure(Call<LoginResponse> c, Throwable t) { /* TODO: log */ }
        });
    }

    public void fetchCurrentUser() {
        String token = TokenManager.get(appCtx);
        if (token == null || token.isEmpty()) {
            // No token available
            return;
        }

        api.getCurrentUser("Bearer " + token).enqueue(new Callback<UserDto>() {
            @Override public void onResponse(Call<UserDto> c, Response<UserDto> r) {
                if (!r.isSuccessful() || r.body()==null) return;
                upsertFromDto(r.body());
            }
            @Override public void onFailure(Call<UserDto> c, Throwable t) { /* TODO: log */ }
        });
    }

    public void logout() {
        TokenManager.clear(appCtx);
        currentUser.postValue(null);
        // optional: io.execute(dao::clearAll);
    }

    private void upsertFromDto(UserDto d) {
        UserEntity e = new UserEntity();

        // Handle ID safely (MongoDB uses _id, some APIs use id)
        String userId = d.getId(); // Use helper method from UserDto
        if (userId == null || userId.trim().isEmpty()) {
            userId = "user_" + System.currentTimeMillis(); // Fallback ID
        }
        e.id = userId;

        e.email = d.email != null ? d.email : "";

        // Build name smartly
        String name = "";
        if (d.name != null && !d.name.trim().isEmpty()) {
            name = d.name.trim();
        } else {
            String firstName = d.firstName != null ? d.firstName.trim() : "";
            String lastName = d.lastName != null ? d.lastName.trim() : "";
            name = (firstName + " " + lastName).trim();
            if (name.isEmpty()) {
                name = d.email != null ? d.email : "משתמש";
            }
        }
        e.name = name;

        e.profileImageUrl = d.profileImage;

        currentUser.postValue(e);
        io.execute(() -> dao.upsert(e));
    }
}