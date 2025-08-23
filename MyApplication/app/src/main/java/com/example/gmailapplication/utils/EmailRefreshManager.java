package com.example.gmailapplication.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class EmailRefreshManager {
    private static final String TAG = "EmailRefreshManager";
    private static final long DEFAULT_REFRESH_INTERVAL = 30000; // 30 שניות
    private static final long FAST_REFRESH_INTERVAL = 10000;    // 10 שניות
    private static final long SLOW_REFRESH_INTERVAL = 60000;    // 60 שניות

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<RefreshListener> listeners = new ArrayList<>();

    private boolean isRefreshing = false;
    private boolean isPaused = false;
    private long currentInterval = DEFAULT_REFRESH_INTERVAL;
    private Runnable refreshRunnable;

    // Singleton pattern
    private static EmailRefreshManager instance;

    public static EmailRefreshManager getInstance() {
        if (instance == null) {
            instance = new EmailRefreshManager();
        }
        return instance;
    }

    private EmailRefreshManager() {
        createRefreshRunnable();
    }

    public interface RefreshListener {
        void onRefreshRequested();
        void onRefreshStarted();
        void onRefreshCompleted(boolean success);
    }

    private void createRefreshRunnable() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPaused && !listeners.isEmpty()) {
                    Log.d(TAG, "Auto-refresh triggered");
                    triggerRefresh();
                }
                // תזמון הרענון הבא
                scheduleNextRefresh();
            }
        };
    }

    // הפעלת רענון אוטומטי
    public void startAutoRefresh() {
        Log.d(TAG, "Starting auto refresh with interval: " + currentInterval + "ms");
        isPaused = false;
        scheduleNextRefresh();
    }

    // עצירת רענון אוטומטי
    public void stopAutoRefresh() {
        Log.d(TAG, "Stopping auto refresh");
        isPaused = true;
        handler.removeCallbacks(refreshRunnable);
    }

    // השהיה זמנית (כשהאפליקציה לא פעילה)
    public void pauseAutoRefresh() {
        Log.d(TAG, "Pausing auto refresh");
        isPaused = true;
        handler.removeCallbacks(refreshRunnable);
    }

    // חידוש אחרי השהיה
    public void resumeAutoRefresh() {
        Log.d(TAG, "Resuming auto refresh");
        if (!listeners.isEmpty()) {
            isPaused = false;
            scheduleNextRefresh();
        }
    }

    // רענון מיידי
    public void refreshNow() {
        Log.d(TAG, "Manual refresh triggered");
        handler.removeCallbacks(refreshRunnable);
        triggerRefresh();
        scheduleNextRefresh();
    }

    // הוספת listener
    public void addRefreshListener(RefreshListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            Log.d(TAG, "Added refresh listener. Total listeners: " + listeners.size());

            // אם זה הlistener הראשון, התחל רענון אוטומטי
            if (listeners.size() == 1) {
                startAutoRefresh();
            }
        }
    }

    // הסרת listener
    public void removeRefreshListener(RefreshListener listener) {
        listeners.remove(listener);
        Log.d(TAG, "Removed refresh listener. Total listeners: " + listeners.size());

        // אם אין יותר listeners, עצור רענון אוטומטי
        if (listeners.isEmpty()) {
            stopAutoRefresh();
        }
    }

    // שינוי מהירות רענון
    public void setRefreshInterval(long intervalMs) {
        this.currentInterval = intervalMs;
        Log.d(TAG, "Refresh interval changed to: " + intervalMs + "ms");

        // אם רענון פעיל, התחל מחדש עם המרווח החדש
        if (!isPaused && !listeners.isEmpty()) {
            handler.removeCallbacks(refreshRunnable);
            scheduleNextRefresh();
        }
    }

    // מעבר לרענון מהיר (למשל אחרי שליחת מייל)
    public void enableFastRefresh() {
        setRefreshInterval(FAST_REFRESH_INTERVAL);

        // חזור לרענון רגיל אחרי דקה
        handler.postDelayed(() -> {
            setRefreshInterval(DEFAULT_REFRESH_INTERVAL);
        }, 60000);
    }

    // מעבר לרענון איטי (כשהאפליקציה ברקע)
    public void enableSlowRefresh() {
        setRefreshInterval(SLOW_REFRESH_INTERVAL);
    }

    // חזרה לרענון רגיל
    public void enableNormalRefresh() {
        setRefreshInterval(DEFAULT_REFRESH_INTERVAL);
    }

    private void scheduleNextRefresh() {
        if (!isPaused) {
            handler.postDelayed(refreshRunnable, currentInterval);
        }
    }

    private void triggerRefresh() {
        if (isRefreshing) {
            Log.d(TAG, "Refresh already in progress, skipping");
            return;
        }

        isRefreshing = true;

        // הודעה לכל הlisteners שהרענון מתחיל
        for (RefreshListener listener : listeners) {
            listener.onRefreshStarted();
            listener.onRefreshRequested();
        }
    }

    // קריאה זו צריכה להיקרא כשהרענון מסתיים
    public void onRefreshCompleted(boolean success) {
        isRefreshing = false;

        // הודעה לכל הlisteners שהרענון הסתיים
        for (RefreshListener listener : listeners) {
            listener.onRefreshCompleted(success);
        }

        Log.d(TAG, "Refresh completed. Success: " + success);
    }

    // בדיקה אם רענון פעיל
    public boolean isAutoRefreshEnabled() {
        return !isPaused && !listeners.isEmpty();
    }

    // בדיקה אם רענון מתבצע כרגע
    public boolean isCurrentlyRefreshing() {
        return isRefreshing;
    }
}