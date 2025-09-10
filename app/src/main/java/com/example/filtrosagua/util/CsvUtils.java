package com.example.filtrosagua.util;

import android.content.Context;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvUtils {

    // === CONFIG COMÚN PARA AMBOS CSV ===
    private static final String HEADER_SCV = "\"seccion\",\"campo\",\"valor\"";

    // Archivo en almacenamiento interno de la app: /data/data/<pkg>/files/<name>
    public static File getCsvFile(Context ctx, String fileName) {
        File dir = new File(ctx.getFilesDir(), "csv"); // /files/csv
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, fileName);
    }

    /** Agrega una línea al CSV.
     *  - Si el archivo no existe o está vacío Y header != null, escribe el header.
     *  - Si header == null, nunca escribe encabezado (útil para sesiones en curso).
     */
    public static synchronized void appendLine(Context ctx, String fileName,
                                               String[] header, String[] row) throws IOException {
        File f = getCsvFile(ctx, fileName);

        boolean needHeader = (!f.exists() || f.length() == 0L);
        boolean shouldWriteHeader = needHeader && (header != null);

        try (FileWriter fw = new FileWriter(f, true)) {
            if (shouldWriteHeader) {
                fw.write(toCsvRow(header));
                fw.write("\n");
            }
            fw.write(toCsvRow(row));
            fw.write("\n");
        }
    }

    /** Reemplaza la **última** fila del CSV (no el header). Si el archivo no existe,
     *  crea con header + la fila (equivale a la primera vez).
     */
    public static synchronized void replaceLastLine(Context ctx, String fileName,
                                                    String[] header, String[] row) throws IOException {
        File f = getCsvFile(ctx, fileName);

        // Si no existe o está vacío, es equivalente a la primera escritura
        if (!f.exists() || f.length() == 0L) {
            appendLine(ctx, fileName, header, row);
            return;
        }

        // Lee todo en memoria (ok para archivos chicos/medianos)
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) lines.add(line);
        }

        String newRow = toCsvRow(row);
        if (lines.size() <= 1) {
            // Solo estaba el header
            lines.add(newRow);
        } else {
            // Reemplaza la última fila (conserva el header en [0])
            lines.set(lines.size() - 1, newRow);
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, false))) {
            for (String l : lines) {
                bw.write(l);
                bw.newLine();
            }
        }
    }

    // ===== NUEVO: utilidades para sesiones/maestro (sección, campo, valor) =====

    /** Asegura que exista header en el archivo (para formato seccion,campo,valor). */
    private static void ensureHeader(File f) throws IOException {
        if (!f.exists() || f.length() == 0L) {
            try (FileWriter fw = new FileWriter(f, false)) {
                fw.write(HEADER_SCV);
                fw.write("\n");
            }
        }
    }

    /** Convierte columnas a una fila CSV: cada campo va entre comillas y con comas entre columnas. */
    private static String toCsvRow(String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(escape(fields[i])).append('"');
        }
        return sb.toString();
    }

    /** Fila CSV para el esquema (seccion,campo,valor). */
    private static String toCsvRowKV(String section, String key, String value) {
        return "\"" + escape(section) + "\",\"" + escape(key) + "\",\"" + escape(value) + "\"";
    }

    /** Escapa comillas dobles dentro del campo CSV. */
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\"\"");
    }

    /**
     * UPSERT de una sección completa en un archivo CSV de sesión (formato seccion,campo,valor).
     * - Si la sección ya existe, elimina sus filas y escribe las nuevas.
     * - Si no existe, la agrega (al final).
     * El orden de 'data' se respetará si usas LinkedHashMap al construirla.
     *
     * @param f       archivo de sesión (p.ej. seguimiento.csv o primeravisita.csv)
     * @param section nombre de la sección (ej.: "info_basica")
     * @param data    pares campo->valor a escribir
     */
    public static synchronized void upsertSection(File f,
                                                  String section,
                                                  Map<String, String> data) throws IOException {
        ensureHeader(f);

        // Lee todo
        List<String> in = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) in.add(line);
        }

        List<String> out = new ArrayList<>();
        // Header
        if (!in.isEmpty() && HEADER_SCV.equals(in.get(0))) out.add(in.get(0));
        else out.add(HEADER_SCV);

        // Copia todo excepto la sección que vamos a reemplazar
        String prefix = "\"" + escape(section) + "\",";
        for (int i = 1; i < in.size(); i++) {
            String line = in.get(i);
            if (!line.startsWith(prefix)) out.add(line);
        }

        // Añade la nueva sección
        for (Map.Entry<String, String> e : data.entrySet()) {
            out.add(toCsvRowKV(section, e.getKey(), e.getValue()));
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, false))) {
            for (String l : out) {
                bw.write(l);
                bw.newLine();
            }
        }
    }

    /**
     * Copia (append) todas las filas de 'src' a 'dest' manteniendo el orden en el que
     * fueron guardadas (omite headers). Útil para consolidar sesión -> maestro.
     *
     * @param src archivo de sesión (ej. seguimiento.csv / primeravisita.csv)
     * @param dest archivo maestro acumulado (ej. master.csv / primeravisita_master.csv)
     */
    public static synchronized void appendFileOrdered(File src, File dest) throws IOException {
        if (src == null || !src.exists() || src.length() == 0L) return;

        ensureHeader(dest); // asegura header en el maestro

        try (BufferedReader br = new BufferedReader(new FileReader(src));
             BufferedWriter bw = new BufferedWriter(new FileWriter(dest, true))) {

            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { // salta header del src
                    first = false;
                    continue;
                }
                if (line.trim().isEmpty()) continue;
                if (HEADER_SCV.equals(line)) continue;

                bw.write(line);
                bw.newLine();
            }
        }
    }
}
