package com.example.gmailapplication.repository;

import android.content.Context;
import com.example.gmailapplication.API.BackendClient;
import com.example.gmailapplication.API.UserAPI;
import com.example.gmailapplication.shared.LoginRequest;
import com.example.gmailapplication.shared.LoginResponse;
import com.example.gmailapplication.shared.RegisterRequest;
import com.example.gmailapplication.shared.UserDto;
import retrofit2.Call;

public class UserRepository {
    private UserAPI api;

    public UserRepository(Context context) {
        this.api = BackendClient.get(context).create(UserAPI.class);
    }

    public Call<UserDto> register(RegisterRequest request) {
        return api.register(request);
    }

    public Call<LoginResponse> login(LoginRequest request) {
        return api.login(request);
    }

    public Call<UserDto> getCurrentUser(String authToken) {
        return api.getCurrentUser(authToken);
    }
}