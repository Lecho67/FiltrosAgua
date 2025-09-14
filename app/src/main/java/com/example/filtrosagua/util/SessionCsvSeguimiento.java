package com.example.filtrosagua.util;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

/**
 * Maneja:
 *  - STAGING: seguimiento.csv con formato "long" ("seccion","campo","valor")
 *  - MASTER_WIDE: seguimiento_master_wide.csv (una fila por encuesta)
 *
 * commitToMasterWide() toma TODO lo que haya en STAGING y lo vuelca como UNA FILA en MASTER_WIDE.
 * Incluye las claves 'ubicacion.latitud' y 'ubicacion.altitud' si existen en STAGING.
 */
public class SessionCsvSeguimiento {

    private static final String STAGING_NAME   = "seguimiento.csv";
    private static final String MASTER_WIDE    = "seguimiento_master_wide.csv";

    /* ===== Archivos ===== */

    public static File stagingFile(Context ctx) {
        File dir = new File(ctx.getFilesDir(), "csv");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, STAGING_NAME);
    }

    public static File masterWideFile(Context ctx) {
        File dir = new File(ctx.getFilesDir(), "csv");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, MASTER_WIDE);
    }

    /* ===== API pública ===== */

    /** Inserta o reemplaza COMPLETAMENTE una sección en el STAGING. */
    public static synchronized void saveSection(Context ctx, String section, Map<String, String> data) throws Exception {
        File f = stagingFile(ctx);
        List<String[]> triples = new ArrayList<>();

        // Cargar lo previo (omitir la sección que vamos a reemplazar)
        if (f.exists() && f.length() > 0) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                // Saltar cabecera si está
                br.mark(256);
                line = br.readLine();
                if (line != null && !isHeader(line)) {
                    br.reset();
                }
                while ((line = br.readLine()) != null) {
                    String[] parts = splitCsvTriple(line);
                    if (parts != null && !section.equals(parts[0])) {
                        triples.add(parts);
                    }
                }
            }
        }

        // Agregar la sección nueva (cada entrada es una fila "section,campo,valor")
        for (Map.Entry<String,String> e : data.entrySet()) {
            String key = e.getKey();
            String val = e.getValue() == null ? "" : e.getValue();
            // key puede venir como "ubicacion.latitud" -> seccion = "ubicacion", campo = "latitud"
            String sec = section;
            String campo = key;
            int dot = key.indexOf('.');
            if (dot > 0) {
                sec = key.substring(0, dot);
                campo = key.substring(dot + 1);
            }
            triples.add(new String[]{sec, campo, val});
        }

        // Escribir de nuevo STAGING (con cabecera)
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, false))) {
            bw.write("seccion,campo,valor");
            bw.newLine();
            for (String[] t : triples) {
                bw.write(csvQuote(t[0])); bw.write(",");
                bw.write(csvQuote(t[1])); bw.write(",");
                bw.write(csvQuote(t[2]));
                bw.newLine();
            }
        }
    }

    /** Limpia el STAGING para la próxima encuesta. */
    public static synchronized void clearSession(Context ctx) {
        File f = stagingFile(ctx);
        if (f.exists()) f.delete();
    }

    /**
     * Toma TODO el STAGING (una encuesta) -> genera UNA FILA en MASTER_WIDE.
     * - Si MASTER_WIDE no existe, crea cabecera automáticamente con todas las claves vistas,
     *   asegurando que incluya 'ubicacion.latitud' y 'ubicacion.altitud'.
     * - Si ya existe, usa su cabecera y rellena vacíos para las claves faltantes.
     */
    public static synchronized void commitToMasterWide(Context ctx) throws Exception {
        File staging = stagingFile(ctx);
        if (!staging.exists() || staging.length() == 0) return;

        // 1) Parsear STAGING (long) a un mapa row (wide)
        Map<String,String> row = parseStagingAsRow(staging);

        // 2) Asegurar que las dos claves estén presentes aunque vengan vacías
        if (!row.containsKey("ubicacion.latitud")) row.put("ubicacion.latitud", "");
        if (!row.containsKey("ubicacion.altitud")) row.put("ubicacion.altitud", "");

        // 3) Abrir MASTER_WIDE
        File master = masterWideFile(ctx);

        if (!master.exists() || master.length() == 0) {
            // 3a) Crear cabecera dinámica a partir de las claves del row
            List<String> header = new ArrayList<>(row.keySet());
            // un pequeño orden por seccion.campo
            Collections.sort(header);
            // Garantizar orden relativo (si quieres fijar un orden de secciones, puedes reordenar aquí)

            // Escribir cabecera + primera fila
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(master, false))) {
                bw.write(String.join(",", quoteList(header)));
                bw.newLine();

                List<String> cols = new ArrayList<>(header.size());
                for (String h : header) cols.add(csvQuote(row.getOrDefault(h, "")));
                bw.write(String.join(",", cols));
                bw.newLine();
            }
        } else {
            // 3b) Ya existe: leer cabecera, y solo APPEND una nueva fila
            List<String> header;
            try (BufferedReader br = new BufferedReader(new FileReader(master))) {
                String first = br.readLine();
                if (first == null || first.trim().isEmpty())
                    throw new IllegalStateException("Cabecera de master_wide está vacía.");
                header = parseHeader(first);
            }

            // Si por algún motivo la cabecera no tiene las nuevas columnas, no reescribimos
            // todo el archivo (para mantener simple). Solo usamos lo existente y rellenamos.
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(master, true))) {
                List<String> cols = new ArrayList<>(header.size());
                for (String h : header) cols.add(csvQuote(row.getOrDefault(h, "")));
                bw.write(String.join(",", cols));
                bw.newLine();
            }
        }
    }

    /* ===== Internos ===== */

    private static Map<String,String> parseStagingAsRow(File f) throws Exception {
        Map<String,String> row = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line = br.readLine(); // cabecera o primera línea
            if (line == null) return row;
            if (!isHeader(line)) {
                // la primera no era cabecera
                String[] p = splitCsvTriple(line);
                if (p != null) row.put(p[0] + "." + p[1], p[2]);
            }
            while ((line = br.readLine()) != null) {
                String[] p = splitCsvTriple(line);
                if (p == null) continue;
                row.put(p[0] + "." + p[1], p[2]);
            }
        }
        return row;
    }

    private static boolean isHeader(String line) {
        String l = line.trim().toLowerCase(Locale.ROOT);
        return l.equals("\"seccion\",\"campo\",\"valor\"") || l.equals("seccion,campo,valor");
    }

    /** Divide una línea CSV muy simple con comillas: "a","b","c" -> [a,b,c] */
    private static String[] splitCsvTriple(String line) {
        List<String> out = new ArrayList<>(3);
        StringBuilder sb = new StringBuilder();
        boolean inQ = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQ && i + 1 < line.length() && line.charAt(i + 1) == '"') { sb.append('"'); i++; }
                else inQ = !inQ;
            } else if (ch == ',' && !inQ) {
                out.add(sb.toString()); sb.setLength(0);
            } else {
                sb.append(ch);
            }
        }
        out.add(sb.toString());
        if (out.size() != 3) return null;
        return new String[]{out.get(0), out.get(1), out.get(2)};
    }

    private static List<String> parseHeader(String line) {
        // Cabeceras sencillas: no esperamos comillas dobles complejas aquí
        List<String> h = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQ = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQ && i + 1 < line.length() && line.charAt(i + 1) == '"') { sb.append('"'); i++; }
                else inQ = !inQ;
            } else if (ch == ',' && !inQ) {
                h.add(unquote(sb.toString()));
                sb.setLength(0);
            } else {
                sb.append(ch);
            }
        }
        h.add(unquote(sb.toString()));
        return h;
    }

    private static String unquote(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1).replace("\"\"", "\"");
        }
        return s;
    }

    private static List<String> quoteList(List<String> keys) {
        List<String> out = new ArrayList<>(keys.size());
        for (String k : keys) out.add(csvQuote(k));
        return out;
    }

    private static String csvQuote(String s) {
        if (s == null) s = "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
