package com.example.gmailapplication.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.gmailapplication.database.entities.UserEntity;

import java.util.List;

@Dao
public interface UserDao {

    @Query("SELECT * FROM users")
    LiveData<List<UserEntity>> observeAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(UserEntity user);

    @Query("DELETE FROM users")
    void clearAll();

    @Query("SELECT * FROM users WHERE id = :userId")
    UserEntity getUserById(String userId);
}