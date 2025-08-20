package com.example.gmailapplication.shared;

// NOTE: comments in English only
import com.example.gmailapplication.database.entities.UserEntity;

public final class UserMapper {

    // Map network DTO -> Room/UI entity
    public static UserEntity toEntity(UserDto d) {
        UserEntity e = new UserEntity();
        e.id = d.id;
        e.email = d.email;

        // build full name from first/last, fallback to email
        String first = d.firstName != null ? d.firstName.trim() : "";
        String last  = d.lastName  != null ? d.lastName.trim()  : "";
        String name  = (first + " " + last).trim();
        e.name = name.isEmpty() ? d.email : name;

        // align field name with your entity
        e.profileImageUrl = d.profileImage; // may be null
        return e;
    }

    private UserMapper() {}
}

