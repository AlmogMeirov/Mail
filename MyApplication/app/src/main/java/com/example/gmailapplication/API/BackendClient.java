package com.example.gmailapplication.API;

import android.content.Context;
import android.os.Build;

import com.example.gmailapplication.shared.Label;
import com.example.gmailapplication.shared.LabelDeserializer;
import com.example.gmailapplication.shared.TokenManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class BackendClient {
    private static volatile Retrofit INSTANCE;

    /**
     * זיהוי אוטומטי של כתובת השרת
     * תמיד משתמש ב-10.0.2.2 לאמולטור - הכתובת הסטנדרטית שעובדת
     */
    private static String getServerUrl() {
        // בדיקה מחוזקת אם זה אמולטור
        boolean isEmulator = Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.FINGERPRINT.contains("test-keys")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.MANUFACTURER.equals("Google")
                || Build.BRAND.startsWith("generic")
                || Build.DEVICE.startsWith("generic")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("emulator")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || "google_sdk".equals(Build.PRODUCT)
                || "sdk_gphone_x86".equals(Build.PRODUCT);

        String url;
        if (isEmulator) {
            // אמולטור: כתובת סטנדרטית שתמיד עובדת
            url = "http://10.0.2.2:3000/api/";
        } else {
            // מכשיר אמיתי: localhost עם adb forward
            url = "http://localhost:3000/api/";
        }

        System.out.println("=== AUTO SERVER CONFIG ===");
        System.out.println("Device: " + Build.MODEL);
        System.out.println("Manufacturer: " + Build.MANUFACTURER);
        System.out.println("Product: " + Build.PRODUCT);
        System.out.println("Hardware: " + Build.HARDWARE);
        System.out.println("Fingerprint: " + Build.FINGERPRINT);
        System.out.println("Is Emulator: " + isEmulator);
        System.out.println("Server URL: " + url);
        System.out.println("=========================");

        return url;
    }

    public static Retrofit get(Context ctx) {
        if (INSTANCE == null) {
            synchronized (BackendClient.class) {
                if (INSTANCE == null) {
                    OkHttpClient ok = new OkHttpClient.Builder()
                            .addInterceptor(new Interceptor() {
                                @Override
                                public Response intercept(Chain chain) throws IOException {
                                    Request original = chain.request();
                                    String jwt = TokenManager.load(ctx);

                                    System.out.println("=== REQUEST DEBUG ===");
                                    System.out.println("URL: " + original.url());
                                    System.out.println("Method: " + original.method());
                                    System.out.println("Token exists: " + (jwt != null && !jwt.isEmpty()));
                                    if (jwt != null && !jwt.isEmpty()) {
                                        System.out.println("Token preview: " + jwt.substring(0, Math.min(20, jwt.length())) + "...");
                                        System.out.println("Full token length: " + jwt.length());
                                    }
                                    System.out.println("=====================");

                                    Request.Builder rb = original.newBuilder();
                                    if (jwt != null && !jwt.isEmpty()) {
                                        String authHeader = "Bearer " + jwt;
                                        rb.header("Authorization", authHeader);
                                        System.out.println("Added Authorization header: " + authHeader.substring(0, Math.min(30, authHeader.length())) + "...");
                                    }

                                    try {
                                        Request finalRequest = rb.build();
                                        System.out.println("Final request headers: " + finalRequest.headers());

                                        Response response = chain.proceed(finalRequest);
                                        System.out.println("=== RESPONSE DEBUG ===");
                                        System.out.println("Response code: " + response.code());
                                        System.out.println("Response message: " + response.message());

                                        // קריאת התוכן הגולמי של התגובה
                                        String responseBodyString = null;
                                        try {
                                            if (response.body() != null) {
                                                ResponseBody responseBody = response.body();
                                                responseBodyString = responseBody.string();
                                                System.out.println("=== RAW RESPONSE BODY ===");
                                                System.out.println("Body length: " + responseBodyString.length());
                                                System.out.println("Body content: " + responseBodyString);
                                                System.out.println("Content-Type: " + response.header("Content-Type"));
                                                System.out.println("========================");

                                                // יצירת ResponseBody חדש כי קראנו את המקורי
                                                MediaType contentType = responseBody.contentType();
                                                ResponseBody newBody = ResponseBody.create(contentType, responseBodyString);
                                                response = response.newBuilder().body(newBody).build();
                                            } else {
                                                System.out.println("=== NO RESPONSE BODY ===");
                                            }
                                        } catch (Exception e) {
                                            System.out.println("Error reading response body: " + e.getMessage());
                                            e.printStackTrace();
                                        }

                                        // אם יש שגיאת 401, הדפס פרטים נוספים
                                        if (response.code() == 401) {
                                            System.out.println("*** 401 UNAUTHORIZED DEBUG ***");
                                            System.out.println("Request URL: " + finalRequest.url());
                                            System.out.println("Authorization header exists: " + (finalRequest.header("Authorization") != null));
                                            if (finalRequest.header("Authorization") != null) {
                                                String authHeaderValue = finalRequest.header("Authorization");
                                                System.out.println("Auth header starts with 'Bearer ': " + (authHeaderValue != null && authHeaderValue.startsWith("Bearer ")));
                                            }
                                        }

                                        System.out.println("======================");
                                        return response;
                                    } catch (Exception e) {
                                        System.out.println("=== NETWORK ERROR ===");
                                        System.out.println("Error: " + e.getMessage());
                                        System.out.println("Error type: " + e.getClass().getSimpleName());
                                        System.out.println("====================");
                                        throw e;
                                    }
                                }
                            }).build();

                    // יצירת Gson פשוט ללא deserializers מותאמים
                    Gson gson = new GsonBuilder()
                            .setLenient() // מאפשר JSON לא מושלם
                            .create();

                    INSTANCE = new Retrofit.Builder()
                            .baseUrl(getServerUrl())
                            .client(ok)
                            .addConverterFactory(GsonConverterFactory.create(gson))
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private BackendClient() {}
}