package ru.practicum.moviehub.api;

import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.util.List;

public class JsonUtils {

    private static final Gson GSON = new Gson();

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    public static <T> List<T> fromJsonList(String json, Type listType) {
        return GSON.fromJson(json, listType);
    }
}