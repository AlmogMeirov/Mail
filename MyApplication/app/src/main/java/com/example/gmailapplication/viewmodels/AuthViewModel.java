package com.example.gmailapplication.viewmodels;
// NOTE: comments in English only
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.gmailapplication.database.entities.UserEntity;
import com.example.gmailapplication.repository.UserRepository;
import com.example.gmailapplication.shared.RegisterRequest;

public class AuthViewModel extends AndroidViewModel {
    private final UserRepository repo;

    public AuthViewModel(@NonNull Application app) {
        super(app);
        repo = new UserRepository(app);
    }

    public LiveData<UserEntity> currentUser() { return repo.currentUser(); }

    public void login(String email, String password) { repo.login(email, password); }

    public void register(RegisterRequest req) { repo.register(req); }

    public void fetchMe() { repo.fetchCurrentUser(); }

    public void logout() { repo.logout(); }
}
