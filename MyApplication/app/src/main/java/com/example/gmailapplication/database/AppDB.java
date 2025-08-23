package com.example.gmailapplication.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.gmailapplication.database.entities.LabelEntity;
import com.example.gmailapplication.database.entities.UserEntity;
import com.example.gmailapplication.database.entities.EmailEntity;
import com.example.gmailapplication.database.converters.StringListConverter;
import com.example.gmailapplication.database.converters.LabelListConverter;
import com.example.gmailapplication.database.dao.LabelDao;
import com.example.gmailapplication.local.UserDao;
import com.example.gmailapplication.local.EmailDao;

@Database(
        entities = {EmailEntity.class, LabelEntity.class, UserEntity.class}, // הוסף את כל הentities
        version = 2, // גרסה 2
        exportSchema = false
)
@TypeConverters({StringListConverter.class, LabelListConverter.class})
public abstract class AppDB extends RoomDatabase {

    private static volatile AppDB INSTANCE;

    public abstract EmailDao emailDao();
    public abstract LabelDao labelDao();
    public abstract UserDao userDao(); // אם יש לך UserEntity

    // הוסף Migration לגרסה 2
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // יצירת טבלת labels
            database.execSQL("CREATE TABLE IF NOT EXISTS `labels` (" +
                    "`id` TEXT NOT NULL, " +
                    "`name` TEXT, " +
                    "`userId` TEXT, " +
                    "`color` TEXT, " +
                    "`isSystemLabel` INTEGER NOT NULL DEFAULT 0, " +
                    "`isDefault` INTEGER NOT NULL DEFAULT 0, " +
                    "`createdAt` INTEGER NOT NULL DEFAULT 0, " +
                    "`lastModified` INTEGER NOT NULL DEFAULT 0, " +
                    "`needsSync` INTEGER NOT NULL DEFAULT 0, " +
                    "`syncStatus` TEXT DEFAULT 'synced', " +
                    "PRIMARY KEY(`id`))");
        }
    };

    public static AppDB get(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDB.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDB.class,
                                    "gmail_app_database"
                            )
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static AppDB getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDB.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDB.class,
                                    "gmail_app_database"
                            )
                            .addMigrations(MIGRATION_1_2) // הוסף את ה-migration
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // פונקציה לניקוי הinstance (לטסטים)
    public static void destroyInstance() {
        INSTANCE = null;
    }
}