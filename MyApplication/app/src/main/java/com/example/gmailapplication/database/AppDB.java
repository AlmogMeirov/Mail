package com.example.gmailapplication.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.gmailapplication.database.entities.UserEntity;
import com.example.gmailapplication.database.entities.EmailEntity;
import com.example.gmailapplication.database.converters.StringListConverter;
import com.example.gmailapplication.database.converters.LabelListConverter;
import com.example.gmailapplication.local.UserDao;
import com.example.gmailapplication.local.EmailDao;

@Database(
        entities = {UserEntity.class, EmailEntity.class},
        version = 2, // עדכנו את הגרסה כי הוספנו entity חדש
        exportSchema = false
)
@TypeConverters({StringListConverter.class, LabelListConverter.class})
public abstract class AppDB extends RoomDatabase {

    public abstract UserDao userDao();
    public abstract EmailDao emailDao(); // הוספת EmailDao

    private static volatile AppDB INSTANCE;

    public static AppDB get(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDB.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDB.class,
                                    "app_database"
                            )
                            .fallbackToDestructiveMigration() // למקרה של שינוי במבנה הDB
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}