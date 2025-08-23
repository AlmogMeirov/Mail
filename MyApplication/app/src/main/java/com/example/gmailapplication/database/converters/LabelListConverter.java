package com.example.gmailapplication.database.converters;

import androidx.room.TypeConverter;
import com.example.gmailapplication.shared.Label;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

public class LabelListConverter {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromLabelList(List<Label> list) {
        if (list == null) {
            return null;
        }
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<Label> toLabelList(String data) {
        if (data == null) {
            return null;
        }
        Type listType = new TypeToken<List<Label>>(){}.getType();
        return gson.fromJson(data, listType);
    }
}