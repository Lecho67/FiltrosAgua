package com.example.filtrosagua.data;

import android.content.Context;
import android.util.Log;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Guarda pares clave-valor en un CSV sencillo:
 * Cada línea: "key","value"
 * - Se escapan comillas internas duplicándolas.
 * - Sin permisos: usa almacenamiento interno de la app.
 */
public class SurveyStore {
    private static final String FILE_NAME = "seguimiento.csv";
    private static final String TAG = "SurveyStore";

    /* ---------- API pública cómoda ---------- */

    public static void save(Context ctx, String key, String value) {
        try {
            Map<String, String> map = readAll(ctx);
            map.put(key, value == null ? "" : value);
            writeAll(ctx, map);
        } catch (Exception e) {
            Log.e(TAG, "save error", e);
        }
    }

    public static String load(Context ctx, String key) {
        try {
            Map<String, String> map = readAll(ctx);
            String v = map.get(key);
            return v == null ? "" : v;
        } catch (Exception e) {
            Log.e(TAG, "load error", e);
            return "";
        }
    }

    /* ---------- Lectura/escritura CSV ---------- */

    private static Map<String, String> readAll(Context ctx) throws IOException {
        File file = new File(ctx.getFilesDir(), FILE_NAME);
        Map<String, String> map = new LinkedHashMap<>();
        if (!file.exists()) return map;

        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while ((line = br.readLine()) != null) {
            // Formato: "key","value"
            if (line.length() < 5) continue;
            // buscamos la coma que separa "key","value"
            int comma = findCsvComma(line);
            if (comma <= 0) continue;

            String key = unquote(line.substring(0, comma));
            String value = unquote(line.substring(comma + 1));
            map.put(key, value);
        }
        br.close();
        return map;
    }

    private static void writeAll(Context ctx, Map<String, String> map) throws IOException {
        File file = new File(ctx.getFilesDir(), FILE_NAME);
        BufferedWriter bw = new BufferedWriter(new FileWriter(file, false));
        for (Map.Entry<String, String> e : map.entrySet()) {
            bw.write(quote(e.getKey()));
            bw.write(",");
            bw.write(quote(e.getValue()));
            bw.newLine();
        }
        bw.flush();
        bw.close();
    }

    private static int findCsvComma(String line) {
        // esperamos formato: "....","...."
        // Buscamos la coma que está fuera de comillas internas
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                // si hay comillas dobles "" es un escape; saltamos 2
                if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                return i;
            }
        }
        return -1;
    }

    private static String quote(String s) {
        if (s == null) s = "";
        // duplicamos comillas internas
        s = s.replace("\"", "\"\"");
        return "\"" + s + "\"";
    }

    private static String unquote(String s) {
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        // revertimos comillas duplicadas
        return s.replace("\"\"", "\"");
    }
}
