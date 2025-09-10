package com.example.filtrosagua.util;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maneja el CSV de SEGUIMIENTO:
 *  - Staging (encuesta en curso):  /files/csv/seguimiento.csv
 *  - Maestro (acumulado):         /files/csv/master.csv
 *
 * Formato por fila: "seccion","campo","valor"
 * El consolidado respeta el orden canónico de secciones (orden de las pantallas).
 */
public class SessionCsv {

    private static final String STAGING = "seguimiento.csv";
    private static final String MASTER  = "master.csv";
    private static final String HEADER  = "\"seccion\",\"campo\",\"valor\"";

    // Orden canónico (según flujo de la app)
    private static final List<String> SECTION_ORDER = Arrays.asList(
            "info_basica",
            "acceso_agua_filtro",
            "percepciones_cambios",
            "mantenimiento",
            "observaciones_tecnicas",
            "ubicacion"
    );

    /* ========= Rutas ========= */
    public static File file(Context ctx)       { return CsvUtils.getCsvFile(ctx, STAGING); }
    public static File masterFile(Context ctx) { return CsvUtils.getCsvFile(ctx, MASTER);  }

    /* ========= Helpers CSV ========= */

    private static String esc(String s) { return s == null ? "" : s.replace("\"", "\"\""); }
    private static String row(String sec, String key, String val) {
        return "\"" + esc(sec) + "\",\"" + esc(key) + "\",\"" + esc(val) + "\"";
    }
    private static boolean isHeader(String line) { return HEADER.equals(line); }

    private static List<String> readAll(File f) throws Exception {
        List<String> out = new ArrayList<>();
        if (f.exists() && f.length() > 0) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) out.add(line);
            }
        }
        return out;
    }

    private static void writeAll(File f, List<String> lines) throws Exception {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, false))) {
            for (String l : lines) {
                bw.write(l);
                bw.newLine();
            }
        }
    }

    /* ========= API ========= */

    /** Inserta/actualiza TODA una sección en el staging (sin duplicar filas de esa sección). */
    public static synchronized void saveSection(Context ctx,
                                                String section,
                                                Map<String, String> data) throws Exception {
        File f = file(ctx);

        List<String> in = readAll(f);
        List<String> out = new ArrayList<>();

        if (in.isEmpty()) {
            out.add(HEADER);
        } else {
            out.add(isHeader(in.get(0)) ? in.get(0) : HEADER);
        }

        String prefix = "\"" + esc(section) + "\",";

        // Copia todo excepto filas de la sección a actualizar
        for (int i = 1; i < in.size(); i++) {
            String line = in.get(i);
            if (!line.startsWith(prefix)) out.add(line);
        }

        // Añade filas de la sección en el orden del mapa (usa LinkedHashMap para mantenerlo)
        for (Map.Entry<String, String> e : data.entrySet()) {
            out.add(row(section, e.getKey(), e.getValue()));
        }

        writeAll(f, out);
    }

    /** Consolida (append) el staging -> maestro, ordenando por secciones según SECTION_ORDER. */
    public static synchronized void commitToMaster(Context ctx) throws Exception {
        File s = file(ctx);
        if (!s.exists() || s.length() == 0) return; // nada por consolidar

        List<String> staging = readAll(s);
        if (staging.size() <= 1) {
            // solo header (o vacío)
            clearSession(ctx);
            return;
        }

        // Bucket por sección en orden canónico
        Map<String, List<String>> secToLines = new LinkedHashMap<>();
        for (String sec : SECTION_ORDER) secToLines.put(sec, new ArrayList<>());

        // También guardamos “secciones no listadas” al final, si aparecieran
        List<String> extraOrder = new ArrayList<>();

        for (int i = 1; i < staging.size(); i++) {
            String l = staging.get(i);
            if (l.trim().isEmpty()) continue;

            // "seccion","campo","valor"  -> extraemos la seccion
            int cut = l.indexOf("\",\"");
            if (cut <= 1) continue; // línea mal formada
            String sec = l.substring(1, cut);

            List<String> bucket = secToLines.get(sec);
            if (bucket != null) {
                bucket.add(l);
            } else {
                // sección no prevista: la acumulamos y luego la escribimos al final
                if (!extraOrder.contains(sec)) extraOrder.add(sec);
                // usamos un bucket temporal por nombre
                // (no hace falta map global; simplemente guardamos la línea)
                // Para mantener orden relativo, almacenamos “tal cual”
                // y luego al escribir recorremos otra vez staging filtrando por estas extra.
            }
        }

        File m = masterFile(ctx);
        boolean writeHeader = !m.exists() || m.length() == 0;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(m, true))) {
            if (writeHeader) {
                bw.write(HEADER);
                bw.newLine();
            }
            // Primero, secciones en orden canónico
            for (String sec : SECTION_ORDER) {
                for (String l : secToLines.get(sec)) {
                    bw.write(l);
                    bw.newLine();
                }
            }
            // Después, secciones no previstas en orden de aparición
            if (!extraOrder.isEmpty()) {
                for (String sec : extraOrder) {
                    String prefix = "\"" + esc(sec) + "\",";
                    for (int i = 1; i < staging.size(); i++) {
                        String l = staging.get(i);
                        if (l.startsWith(prefix)) {
                            bw.write(l);
                            bw.newLine();
                        }
                    }
                }
            }
        }

        // Limpiar staging tras consolidar
        clearSession(ctx);
    }

    /** Deja el staging vacío para una nueva encuesta. */
    public static synchronized void clearSession(Context ctx) {
        File f = file(ctx);
        if (f.exists()) {
            // puedes borrar o truncar; borrar es simple y seguro
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }
}
