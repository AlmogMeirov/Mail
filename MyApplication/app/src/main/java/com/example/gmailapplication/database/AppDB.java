package com.example.gmailapplication.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.gmailapplication.database.entities.UserEntity;
import com.example.gmailapplication.local.UserDao;

@Database(
        entities = {UserEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDB extends RoomDatabase {

    public abstract UserDao userDao();

    private static volatile AppDB INSTANCE;

    public static AppDB get(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDB.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDB.class,
                            "app_database"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}