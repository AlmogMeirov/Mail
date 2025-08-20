package com.example.gmailapplication.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey
    @NonNull
    public String id = ""; // Just add = "" here

    public String email;
    public String name;
    public String profileImageUrl;

    public UserEntity() {}
}