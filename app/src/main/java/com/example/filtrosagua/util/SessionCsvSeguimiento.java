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
            "ubicacion.departamento",
            "ubicacion.municipio",
            "ubicacion.vereda_corregimiento",
            "ubicacion.direccion",
            "ubicacion.latitud",
            "ubicacion.altitud",
            "info_responsable.cedula"
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
        File staging = stagingFile(ctx); // FIX: antes file(ctx)
        if (!staging.exists() || staging.length()==0) return;

        List<StagingEntry> entries = readStagingEntries(staging);
        LinkedHashMap<String,String> orderedMap = new LinkedHashMap<>();
        for (StagingEntry e : entries) {
            if (!orderedMap.containsKey(e.key)) {
                orderedMap.put(e.key, e.value);
            } else {
                orderedMap.put(e.key, e.value);
            }
        }

        orderedMap.put("timestamp_ms", String.valueOf(System.currentTimeMillis()));
        orderedMap.put("tipo_formulario", "seguimiento");

        ensure(orderedMap, "ubicacion.departamento");
        ensure(orderedMap, "ubicacion.municipio");
        ensure(orderedMap, "ubicacion.vereda_corregimiento");
        ensure(orderedMap, "ubicacion.direccion");
        ensure(orderedMap, "ubicacion.latitud");
        ensure(orderedMap, "ubicacion.altitud");
        ensure(orderedMap, "info_responsable.cedula");
        ensure(orderedMap, "info_responsable.telefono"); // nueva columna unificada

        List<String> columnOrder = buildColumnOrder(orderedMap.keySet());
        File master = masterFile(ctx);
        appendRowNoHeader(master, columnOrder, orderedMap);
        clearSession(ctx);
    }

    // Helpers añadidos (similar a primera)
    private static void ensure(Map<String,String> m, String k){
        if(!m.containsKey(k)) m.put(k, "");
    }
    private static class StagingEntry {
        final String key;
        final String value;
        StagingEntry(String k, String v){ this.key = k; this.value = v; }
    }

    private static List<StagingEntry> readStagingEntries(File staging) throws Exception {
        List<StagingEntry> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(staging))) {
            String line = br.readLine();
            boolean header = isHeader(line);
            if (!header && line != null) {
                StagingEntry e = parseTriple(line);
                if (e != null) list.add(e);
            }
            while ((line = br.readLine()) != null) {
                StagingEntry e = parseTriple(line);
                if (e != null) list.add(e);
            }
        }
        return list;
    }

    private static StagingEntry parseTriple(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        String[] parts = splitCsvTriple(line);
        if (parts == null) return null;
        return new StagingEntry(parts[0] + "." + parts[1], parts[2]);
    }

    private static boolean isHeader(String line) {
        if (line == null) return false;
        String l = line.trim().toLowerCase(Locale.ROOT);
        return l.equals("\"seccion\",\"campo\",\"valor\"")
                || l.equals("seccion,campo,valor")
                || l.startsWith("seccion,campo,valor");
    }

    private static String[] splitCsvTriple(String line) {
        List<String> out = new ArrayList<>(3);
        StringBuilder sb = new StringBuilder();
        boolean inQ = false;
        for (int i=0;i<line.length();i++) {
            char ch = line.charAt(i);
            if (ch=='"') {
                if (inQ && i+1<line.length() && line.charAt(i+1)=='"') { sb.append('"'); i++; }
                else inQ = !inQ;
            } else if (ch==',' && !inQ) {
                out.add(sb.toString()); sb.setLength(0);
            } else {
                sb.append(ch);
            }
        }
        out.add(sb.toString());
        if (out.size()!=3) return null;
        return new String[]{out.get(0), out.get(1), out.get(2)};
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