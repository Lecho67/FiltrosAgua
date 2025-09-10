package com.example.filtrosagua.util;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    private static final String PREFS_NAME = "filtros_agua_prefs";

    public static void put(Context c, String key, String value){
        c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(key, value).apply();
    }

    public static String get(Context c, String key){
        return c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(key, "");
    }

    public static void remove(Context c, String key){
        c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().remove(key).apply();
    }

    public static void clearAll(Context c){
        c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().clear().apply();
    }
}
