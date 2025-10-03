package com.example.filtrosagua.util;

import android.content.Context;

import java.io.*;
import java.util.*;

/**
 * STAGING  : /files/csv/seguimiento.csv  (formato "long": seccion,campo,valor)
 * MASTER   : /files/csv/encuestas_master_wide.csv (formato "wide": una fila por encuesta SIN CABECERA)
 *
 * MODIFICADO: Guarda las filas sin cabecera en el archivo maestro wide.
 * Las columnas siguen el orden definido en BASE_ORDER y luego alfabético.
 */
public class SessionCsvSeguimiento {

    private static final String STAGING_NAME = "seguimiento.csv";
    // Ahora ambos formularios (primera y seguimiento) apuntan al mismo master unificado
    private static final String MASTER_NAME  = "encuestas_master_wide.csv";

    /** Orden base obligatorio al inicio de las columnas (unificado para ambos formularios) */
    private static final List<String> BASE_ORDER = Arrays.asList(
            "timestamp_ms",
            "tipo_formulario",
            "ubicacion.municipio",
            "ubicacion.vereda_corregimiento",
            "ubicacion.direccion",
            "ubicacion.latitud",
            "ubicacion.altitud",
            "info_responsable.cedula",   // solo primera, quedará vacío en seguimiento
            "info_basica.cedula"         // seguimiento
    );


    /* ================== Archivos ================== */

    public static File stagingFile(Context ctx) {
        File dir = new File(ctx.getFilesDir(), "csv");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, STAGING_NAME);
    }

    public static File masterFile(Context ctx) {
        File dir = new File(ctx.getFilesDir(), "csv");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, MASTER_NAME);
    }

    /* ================== API pública ================== */

    /** Inserta o reemplaza completamente una sección en STAGING (long). */
    public static synchronized void saveSection(Context ctx, String section, Map<String, String> data) throws Exception {
        File f = stagingFile(ctx);
        List<String[]> triples = new ArrayList<>();

        // Cargar previos (omitiendo la sección a reemplazar)
        if (f.exists() && f.length() > 0) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line = br.readLine(); // cabecera o primera línea
                if (line != null && !isHeader(line)) {
                    String[] p = splitTriple(line);
                    if (p != null && !section.equals(p[0])) triples.add(p);
                }
                while ((line = br.readLine()) != null) {
                    String[] p = splitTriple(line);
                    if (p != null && !section.equals(p[0])) triples.add(p);
                }
            }
        }

        // Añadir nueva sección
        for (Map.Entry<String, String> e : data.entrySet()) {
            String key = e.getKey();
            String val = e.getValue() == null ? "" : e.getValue();
            String sec = section;
            String campo = key;
            int dot = key.indexOf('.');
            if (dot > 0) {
                sec = key.substring(0, dot);
                campo = key.substring(dot + 1);
            }
            triples.add(new String[]{sec, campo, val});
        }

        // Reescribir staging
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

    /** Limpia el STAGING. */
    public static synchronized void clearSession(Context ctx) {
        File f = stagingFile(ctx);
        if (f.exists()) //noinspection ResultOfMethodCallIgnored
            f.delete();
    }

    /**
     * Toma TODO lo que haya en STAGING y lo vuelca como UNA FILA en MASTER (wide) SIN CABECERA.
     * Simplemente agrega una nueva línea al final del archivo con el orden de columnas consistente.
     */
    public static synchronized void commitToMasterWide(Context ctx) throws Exception {
        File staging = stagingFile(ctx);
        if (!staging.exists() || staging.length() == 0) return;

        // 1) Parsear staging -> mapa wide
        Map<String, String> row = parseStagingAsRow(staging);

        row.put("timestamp_ms", String.valueOf(System.currentTimeMillis()));
        row.put("tipo_formulario", "seguimiento");

        // Asegurar claves base
        if (!row.containsKey("ubicacion.municipio"))             row.put("ubicacion.municipio", "");
        if (!row.containsKey("ubicacion.vereda_corregimiento"))  row.put("ubicacion.vereda_corregimiento", "");
        if (!row.containsKey("ubicacion.direccion"))             row.put("ubicacion.direccion", "");
        if (!row.containsKey("ubicacion.latitud"))               row.put("ubicacion.latitud", "");
        if (!row.containsKey("ubicacion.altitud"))               row.put("ubicacion.altitud", "");
        if (!row.containsKey("info_responsable.cedula"))         row.put("info_responsable.cedula", "");
        if (!row.containsKey("info_basica.cedula"))              row.put("info_basica.cedula", "");

        File master = masterFile(ctx);

        // Determinar el orden de columnas basado en BASE_ORDER + nuevas columnas
        List<String> columnOrder = buildColumnOrder(row.keySet());

        // Simplemente agregar la fila al archivo (sin cabecera)
        appendRowNoHeader(master, columnOrder, row);

        // Limpiar staging después de consolidar
        clearSession(ctx);
    }

    /* ================== Internos ================== */

    private static Map<String, String> parseStagingAsRow(File f) throws Exception {
        Map<String, String> row = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line = br.readLine(); // cabecera o primera línea
            if (line == null) return row;
            if (!isHeader(line)) {
                String[] p = splitTriple(line);
                if (p != null) row.put(p[0] + "." + p[1], p[2]);
            }
            while ((line = br.readLine()) != null) {
                String[] p = splitTriple(line);
                if (p != null) row.put(p[0] + "." + p[1], p[2]);
            }
        }
        return row;
    }

    private static boolean isHeader(String line) {
        String l = line.trim().toLowerCase(Locale.ROOT);
        return l.equals("seccion,campo,valor") || l.equals("\"seccion\",\"campo\",\"valor\"");
    }

    /** Divide "a","b","c" en 3 columnas respetando comillas. */
    private static String[] splitTriple(String line) {
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
        return new String[]{unquote(out.get(0)), unquote(out.get(1)), unquote(out.get(2))};
    }

    private static String unquote(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1).replace("\"\"", "\"");
        }
        return s;
    }

    private static String csvQuote(String s) {
        if (s == null) s = "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    /* ===== Funciones simplificadas sin manejo de cabecera ===== */

    /**
     * Construye el orden de columnas basado en BASE_ORDER + columnas adicionales ordenadas alfabéticamente
     */
    private static List<String> buildColumnOrder(Set<String> keys) {
        LinkedHashSet<String> rest = new LinkedHashSet<>(keys);
        rest.removeAll(BASE_ORDER);
        List<String> tail = new ArrayList<>(rest);
        Collections.sort(tail);
        List<String> columnOrder = new ArrayList<>(BASE_ORDER);
        columnOrder.addAll(tail);
        return columnOrder;
    }

    /**
     * Agrega una fila al archivo maestro SIN escribir cabecera
     */
    private static void appendRowNoHeader(File master, List<String> columnOrder, Map<String, String> rowMap) throws Exception {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(master, true))) {
            List<String> cols = new ArrayList<>(columnOrder.size());
            for (String col : columnOrder) {
                cols.add(csvQuote(rowMap.getOrDefault(col, "")));
            }
            bw.write(String.join(",", cols));
            bw.newLine();
        }
    }
}