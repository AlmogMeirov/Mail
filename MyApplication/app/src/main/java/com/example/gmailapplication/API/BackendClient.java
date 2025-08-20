package com.example.gmailapplication.API;
// NOTE: comments in English only
import android.content.Context;

import com.example.gmailapplication.shared.TokenManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class BackendClient {
    private static volatile Retrofit INSTANCE;

    public static Retrofit get(Context ctx) {
        if (INSTANCE == null) {
            synchronized (BackendClient.class) {
                if (INSTANCE == null) {
                    OkHttpClient ok = new OkHttpClient.Builder()
                            .addInterceptor((Interceptor) chain -> {
                                Request original = chain.request();
                                String jwt = TokenManager.load(ctx);
                                Request.Builder rb = original.newBuilder();
                                if (jwt != null && !jwt.isEmpty()) {
                                    rb.header("Authorization", "Bearer " + jwt);
                                }
                                return chain.proceed(rb.build());
                            }).build();

                    INSTANCE = new Retrofit.Builder()
                            .baseUrl("http://10.0.2.2:3000/api/") // emulator -> localhost:3000
                            .client(ok)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return INSTANCE;
    }
    private BackendClient(){}
}
