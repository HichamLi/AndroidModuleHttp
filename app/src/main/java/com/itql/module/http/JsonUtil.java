package com.itql.module.http;


import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class JsonUtil {
    public static final String TAG = JsonUtil.class.getSimpleName();
    private static Gson sGson;
    private static Gson sMapGson;

    static {
        sGson = new Gson();
        sMapGson = new GsonBuilder().registerTypeAdapter(
            new TypeToken<TreeMap<String, Object>>() {}.getType(),
            new JsonDeserializer<TreeMap<String, Object>>() {
                @Override
                public TreeMap<String, Object> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    TreeMap<String, Object> treeMap = new TreeMap<>();
                    JsonObject jsonObject = json.getAsJsonObject();
                    Set<Map.Entry<String, JsonElement>> entrySet = jsonObject.entrySet();
                    for (Map.Entry<String, JsonElement> entry : entrySet) {
                        Object ot = entry.getValue();
                        if (ot instanceof JsonPrimitive) {
                            treeMap.put(entry.getKey(), ((JsonPrimitive) ot).getAsString());
                        } else {
                            treeMap.put(entry.getKey(), ot);
                        }
                    }
                    return treeMap;
                }
            }).create();
    }

    public static String beanToJson(Object o) {
        String s = sGson.toJson(o);
        JsonElement element = JsonParser.parseString(s);
        sort(element);
        return sGson.toJson(element);
    }

    public static <T> T jsonToBean(String s, Class<T> tClass) {
        try {
            return sGson.fromJson(s, tClass);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }

    public static <T> T jsonToBean(String s, TypeToken typeToken) {
        try {
            return sGson.fromJson(s, typeToken.getType());
        } catch (JsonSyntaxException e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }

    public static <T> List<T> jsonToList(String s, TypeToken typeToken) {
        try {
            return sGson.fromJson(s, typeToken.getType());
        } catch (JsonSyntaxException e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }

    public static <T> ArrayList<T> jsonToArrayList(String s, TypeToken typeToken) {
        try {
            return sGson.fromJson(s, typeToken.getType());
        } catch (JsonSyntaxException e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }

    public static TreeMap<String, Object> beanToMap(Object o) {
        return sMapGson.fromJson(beanToJson(o), new TypeToken<TreeMap<String, Object>>() {}.getType());
    }

    public static TreeMap<String, Object> jsonToMap(String s) {
        return sMapGson.fromJson(s, new TypeToken<TreeMap<String, Object>>() {}.getType());
    }

    public static void sort(JsonElement e) {
        if (e.isJsonNull()) {
            return;
        }

        if (e.isJsonPrimitive()) {
            return;
        }

        if (e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            for (Iterator<JsonElement> it = a.iterator(); it.hasNext(); ) {
                sort(it.next());
            }
            return;
        }

        if (e.isJsonObject()) {
            Map<String, JsonElement> tm = new TreeMap<String, JsonElement>(getComparator());
            for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) {
                tm.put(en.getKey(), en.getValue());
            }

            for (Map.Entry<String, JsonElement> en : tm.entrySet()) {
                e.getAsJsonObject().remove(en.getKey());
                e.getAsJsonObject().add(en.getKey(), en.getValue());
                sort(en.getValue());
            }
            return;
        }
    }

    private static Comparator<String> getComparator() {
        Comparator<String> c = new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        };
        return c;
    }

}
