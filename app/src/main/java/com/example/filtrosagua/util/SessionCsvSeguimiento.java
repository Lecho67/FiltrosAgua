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
import java.util.Locale;
import java.util.Map;

/**
 * Maneja el CSV para el formulario de SEGUIMIENTO.
 *
 * Staging (long):  /files/csv/seguimiento.csv         -> "seccion","campo","valor"
 * Maestro wide:    /files/csv/seguimiento_master_wide.csv -> 1 fila por encuesta.
 */
public class SessionCsvSeguimiento {

    /* ====== Rutas ====== */
    public static File file(Context ctx)        { return new File(ensureCsvDir(ctx), "seguimiento.csv"); }
    public static File fMasterWide(Context ctx) { return new File(ensureCsvDir(ctx), "seguimiento_master_wide.csv"); }

    private static File ensureCsvDir(Context ctx) {
        File dir = new File(ctx.getFilesDir(), "csv");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /* ====== API Principal ====== */

    /**
     * Guarda/actualiza una sección en el archivo de staging (seguimiento.csv).
     * Usa el formato "long" (seccion, campo, valor).
     */
    public static void saveSection(Context ctx, String section, Map<String, String> data) throws Exception {
        CsvUtils.upsertSection(file(ctx), section, data);
    }

    /**
     * Limpia el archivo de staging para la próxima encuesta.
     */
    public static void clearSession(Context ctx) {
        File f = file(ctx);
        if (f.exists()) f.delete();
    }

    /**
     * Toma el staging "long" (seguimiento.csv), lo convierte a una fila "wide"
     * y la añade al maestro (seguimiento_master_wide.csv).
     * Finalmente, limpia el staging.
     */
    public static void commitToMasterWide(Context ctx) throws Exception {
        File staging = file(ctx);
        if (!staging.exists() || staging.length() == 0) return;

        // 1. Parsear el staging "long" a un mapa de clave->valor para la fila "wide"
        Map<String, String> row = parseStagingToWideRow(staging);

        // 2. Escribir/añadir al maestro "wide"
        File masterWide = fMasterWide(ctx);
        boolean writeHeader = !masterWide.exists() || masterWide.length() == 0;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(masterWide, true))) {
            if (writeHeader) {
                bw.write(String.join(",", quoteList(WIDE_COLUMNS)));
                bw.newLine();
            }
            // Producir la fila en el orden definido por WIDE_COLUMNS
            List<String> values = new ArrayList<>(WIDE_COLUMNS.size());
            for (String col : WIDE_COLUMNS) {
                values.add(csvQuote(row.getOrDefault(col, "")));
            }
            bw.write(String.join(",", values));
            bw.newLine();
        }

        // 3. Limpiar el staging para la siguiente encuesta
        clearSession(ctx);
    }

    /* ====== Columnas fijas (en orden) para el CSV wide de seguimiento ====== */
    private static final List<String> WIDE_COLUMNS = Arrays.asList(
            // info_basica
            "info_basica.fecha",
            "info_basica.responsable",
            "info_basica.empresa",
            "info_basica.numero1",
            "info_basica.numero2",

            // acceso_agua_filtro
            "acceso_agua_filtro.fecha",
            "acceso_agua_filtro.fuente_agua",
            "acceso_agua_filtro.porque_arcilla",
            "acceso_agua_filtro.dias_almacenada",
            "acceso_agua_filtro.veces_recarga",
            "acceso_agua_filtro.miembro_recarga",
            "acceso_agua_filtro.uso_del_agua",

            // percepciones_cambios
            "percepciones_cambios.cambios",
            "percepciones_cambios.percepcion",
            "percepciones_cambios.sabor",
            "percepciones_cambios.color",
            "percepciones_cambios.olor",
            "percepciones_cambios.enfermedades_disminuyen",
            "percepciones_cambios.gastos_disminuyen",
            "percepciones_cambios.gasto_actual",

            // mantenimiento
            "mantenimiento.frecuencia_mantenimiento",
            "mantenimiento.productos_limpieza_arcilla",
            "mantenimiento.productos_limpieza_plastico",
            "mantenimiento.conoce_vida_util_arcilla",
            "mantenimiento.sabe_donde_conseguir_repuestos",

            // observaciones_tecnicas
            "observaciones_tecnicas.estable_seguro",
            "observaciones_tecnicas.ensamblado_tapado",
            "observaciones_tecnicas.limpio_parte_externa",
            "observaciones_tecnicas.lavado_manos_previo",
            "observaciones_tecnicas.manipulacion_arcilla_adecuada",
            "observaciones_tecnicas.limpieza_tanque_sin_sedimentos",
            "observaciones_tecnicas.limpieza_vasija_sin_sedimentos",
            "observaciones_tecnicas.fisuras_arcilla",
            "observaciones_tecnicas.niveles_agua_impiden_manipulacion",
            "observaciones_tecnicas.instalacion_lavado_manos",
            "observaciones_tecnicas.disp_jabon_lavado_manos",

            // ubicacion
            "ubicacion.direccion_referencias"
    );

    /* ====== Helpers: de long -> wide row ====== */

    /** Convierte el staging "long" a un mapa de columnas fijas (wide) para UNA encuesta. */
    private static Map<String,String> parseStagingToWideRow(File staging) throws Exception {
        Map<String,String> row = new LinkedHashMap<>();
        // Lee todo el staging y arma clave "seccion.campo"
        try (BufferedReader br = new BufferedReader(new FileReader(staging))) {
            String line = br.readLine();
            boolean headerSeen = isHeader(line);
            if (!headerSeen) {
                // primera línea era dato
                if (line != null) putLine(row, line);
            }
            while ((line = br.readLine()) != null) {
                putLine(row, line);
            }
        }

        // Normalización para consolidar claves si es necesario
        normalizeRow(row);

        return row;
    }

    private static boolean isHeader(String line) {
        if (line == null) return false;
        String l = line.trim().toLowerCase(Locale.ROOT);
        return l.equals("\"seccion\",\"campo\",\"valor\"")
                || l.equals("seccion,campo,valor")
                || l.startsWith("seccion,campo,valor");
    }

    private static void putLine(Map<String,String> row, String csvLine) {
        if (csvLine == null || csvLine.trim().isEmpty()) return;
        String[] parts = splitCsvTriple(csvLine);
        if (parts == null) return;
        String sec = parts[0];
        String key = parts[1];
        String val = parts[2];
        row.put(sec + "." + key, val);
    }

    /**
     * Acepta variantes de nombres de campos y las mapea a las columnas finales.
     * Para seguimiento, los nombres son bastante consistentes, pero esto es útil por si acaso.
     */
    private static void normalizeRow(Map<String,String> r) {
        // Ejemplo: si en una Activity guardaste "miembro" y en la columna es "miembro_recarga"
        // moveAny(r, Arrays.asList("acceso_agua_filtro.miembro"), "acceso_agua_filtro.miembro_recarga");

        // Por ahora, las claves de las Activities de seguimiento ya coinciden con WIDE_COLUMNS,
        // por lo que no se necesita normalización. Se deja el método por si se necesita en el futuro.
    }

    private static void moveAny(Map<String,String> map, List<String> sources, String target) {
        if (map.containsKey(target) && notEmpty(map.get(target))) return;
        for (String s : sources) {
            String v = map.get(s);
            if (notEmpty(v)) { map.put(target, v); return; }
        }
    }

    private static boolean notEmpty(String s){ return s != null && !s.trim().isEmpty(); }

    /* ====== CSV helpers ====== */

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