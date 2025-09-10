package com.example.filtrosagua.util;

import android.content.Context;
import android.util.Log;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Genera CSV "wide" (una fila por encuesta) a partir de:
 *  - /files/master.csv
 *  - /files/csv/primeravisita_master.csv
 *
 * Mejoras:
 *  - Corte robusto: por índice de sección Y por “reaparición” de la sección raíz (info_responsable) en PV.
 *  - Normalización con más alias.
 *  - Dump de claves para depurar (csv/primeravisita_keys_dump.txt).
 */
public class CsvUnifiedWide {

    private static final String TAG = "CSV_WIDE";
    private static final boolean DEBUG_LOG = false;

    // Orden de SEGUIMIENTO
    private static final List<String> SEG_ORDER = Arrays.asList(
            "info_basica",
            "acceso_agua_filtro",
            "percepciones_cambios",
            "mantenimiento",
            "observaciones_tecnicas",
            "ubicacion"
    );

    // Orden de PRIMERA VISITA (ajústalo si cambias pantallas)
    private static final List<String> PV_ORDER = Arrays.asList(
            "info_responsable",
            "beneficiario",
            "ubicacion",
            "demografia",
            "acceso_agua",
            "desplazamiento",
            "percepcion_agua",
            "almacenamiento_tratamiento",
            "contaminacion_proteccion",
            "saneamiento",
            "higiene",
            "salud"
    );

    /** Genera /files/csv/maestro_unificado_wide_yyyyMMdd_HHmmss.csv */
    public static File buildWideCsv(Context ctx) throws Exception {
        File dirCsv = new File(ctx.getFilesDir(), "csv");
        if (!dirCsv.exists()) dirCsv.mkdirs();

        File segMaster = SessionCsv.masterFile(ctx);
        File pvMaster  = SessionCsvPrimera.fMaster(ctx);

        // 1) Parseo “long → filas wide”
        List<Map<String,String>> segRows = parseMasterAsRows(segMaster, SEG_ORDER, null);
        List<Map<String,String>> pvRows  = parseMasterAsRows(pvMaster,  PV_ORDER, "info_responsable"); // <- corte adicional por raíz

        // 2) Normalización PV (alias + columnas combinadas)
        for (Map<String,String> r : pvRows) normalizePrimeraRow(r);

        // 3) Unión por índice
        int n = Math.max(segRows.size(), pvRows.size());
        List<Map<String,String>> unifiedRows = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Map<String,String> row = new LinkedHashMap<>();
            if (i < pvRows.size())  row.putAll(pvRows.get(i));
            if (i < segRows.size()) row.putAll(segRows.get(i));
            unifiedRows.add(row);
        }

        // 4) Cabecera (PV primero, SEG después) según orden de secciones
        List<String> header = buildHeader(pvRows, PV_ORDER);
        header.addAll(buildHeader(segRows, SEG_ORDER));

        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File out = new File(dirCsv, "maestro_unificado_wide_" + ts + ".csv");
        writeCsv(out, header, unifiedRows);

        if (DEBUG_LOG) Log.d(TAG, "CSV wide generado: " + out.getAbsolutePath());
        return out;
    }

    /* =================== Parseo long→filas con corte robusto =================== */

    /**
     * @param rootSection Si no es null, además del corte por orden de secciones,
     *                    se inicia nueva fila cuando reaparece rootSection (p.ej. "info_responsable").
     */
    private static List<Map<String,String>> parseMasterAsRows(File f, List<String> secOrder, String rootSection) throws Exception {
        List<Map<String,String>> rows = new ArrayList<>();
        if (f == null || !f.exists() || f.length() == 0) return rows;

        Map<String,Integer> secIndex = new HashMap<>();
        for (int i = 0; i < secOrder.size(); i++) secIndex.put(secOrder.get(i), i);

        // Dump de claves para depuración (solo PV; para SEG cambia el nombre si quieres)
        boolean doDump = (rootSection != null); // solo para PV
        Set<String> keysSeen = doDump ? new TreeSet<>() : null;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;

            // Saltar cabecera si está
            br.mark(2048);
            line = br.readLine();
            if (line == null) return rows;
            if (!isHeader(line)) br.reset();

            Map<String,String> current = new LinkedHashMap<>();
            Integer lastIdx = null;

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = splitCsvTriple(line);
                if (parts == null) continue;

                String sec = parts[0].trim();
                String key = parts[1].trim();
                String val = parts[2];

                if (doDump && keysSeen != null) keysSeen.add(sec + "." + key);

                Integer idx = secIndex.get(sec);
                // Corte por “reaparición” de la raíz
                if (rootSection != null && rootSection.equals(sec) && !current.isEmpty()) {
                    rows.add(current);
                    current = new LinkedHashMap<>();
                    lastIdx = null; // reinicia el seguimiento de índice
                }
                // Corte por orden (si retrocede o repite)
                if (idx != null) {
                    if (lastIdx != null && idx <= lastIdx && !current.isEmpty()) {
                        rows.add(current);
                        current = new LinkedHashMap<>();
                    }
                    lastIdx = idx;
                }

                current.put(sec + "." + key, val);
            }

            if (!current.isEmpty()) rows.add(current);
        }

        // Escribe dump de claves para ver EXACTAMENTE qué llega
        if (doDump && keysSeen != null && !keysSeen.isEmpty()) {
            writeKeysDump(f.getParentFile().getParentFile(), keysSeen); // /files/csv/...
        }

        return rows;
    }

    private static boolean isHeader(String line) {
        String l = line.trim().toLowerCase(Locale.ROOT);
        return l.equals("\"seccion\",\"campo\",\"valor\"")
                || l.equals("seccion,campo,valor")
                || l.startsWith("seccion,campo,valor");
    }

    /** "a","b","c" -> [a,b,c] con comillas escapadas */
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

    /* =================== Normalización de PV (más alias) =================== */

    private static void normalizePrimeraRow(Map<String,String> row) {
        Map<String,String> r = new LinkedHashMap<>(row);

        // UBICACIÓN
        move(r, "ubicacion.vereda",            "ubicacion.vereda_corregimiento");
        move(r, "ubicacion.corregimiento",     "ubicacion.vereda_corregimiento");
        move(r, "ubicacion.vereda_correg",     "ubicacion.vereda_corregimiento");

        // ACCESO AGUA
        move(r, "acceso_agua.dispone_agua",             "acceso_agua.tiene_agua");
        move(r, "acceso_agua.fuente_agua",              "acceso_agua.fuente_respuesta");
        move(r, "acceso_agua.administrador_servicio",   "acceso_agua.administra_servicio");
        move(r, "acceso_agua.horas_disponibilidad_dia", "acceso_agua.horas_dia");

        // DESPLAZAMIENTO
        move(r, "desplazamiento.tiene_que_desplazarse", "desplazamiento.necesita_desplazarse");
        move(r, "desplazamiento.medio_transporte",      "desplazamiento.medio_utiliza");
        move(r, "desplazamiento.medio",                 "desplazamiento.medio_utiliza");
        move(r, "desplazamiento.minutos",               "desplazamiento.tiempo_min");
        move(r, "desplazamiento.tiempo",                "desplazamiento.tiempo_min");

        // PERCEPCIÓN
        move(r, "percepcion_agua.claridad_temporada",   "percepcion_agua.aspecto");
        move(r, "percepcion_agua.sabor",                "percepcion_agua.opinion_sabor");
        move(r, "percepcion_agua.olores",               "percepcion_agua.presenta_olores");

        // ALMACENAMIENTO/TRATAMIENTO
        move(r, "almacenamiento_tratamiento.almacena_tanque", "almacenamiento_tratamiento.tanque");
        move(r, "almacenamiento_tratamiento.hervir_emplea",   "almacenamiento_tratamiento.hierve_como");
        move(r, "almacenamiento_tratamiento.quien_realiza",   "almacenamiento_tratamiento.quien_labores");

        // CONTAMINACIÓN/PROTECCIÓN
        move(r, "contaminacion_proteccion.contaminacion_fuentes",       "contaminacion.contacto_fuentes");
        move(r, "contaminacion_proteccion.fuente_protegida",            "contaminacion.fuente_protegida");
        move(r, "contaminacion_proteccion.importante_consumir_potable", "contaminacion.importancia_consumir_buena");
        move(r, "contaminacion_proteccion.beneficios_consumir_potable", "contaminacion.beneficios");

        // SANEAMIENTO
        move(r, "saneamiento.sanitario_taza",       "saneamiento.taza");
        move(r, "saneamiento.sistema_disposicion",  "saneamiento.sistema_residuos");

        // HIGIENE
        move(r, "higiene.capacitacion_higiene", "higiene.capacitacion");
        List<String> pract = new ArrayList<>();
        if (eqSi(r.get("higiene.practica_lavado_manos")))    pract.add("Lavado de manos");
        if (eqSi(r.get("higiene.practica_limpieza_hogar")))  pract.add("Limpieza del hogar");
        if (eqSi(r.get("higiene.practica_cepillado_dientes"))) pract.add("Cepillado de dientes");
        if (eqSi(r.get("higiene.practica_otro")))            pract.add("Otro");
        if (eqSi(r.get("higiene.practica_bano_diario")))     pract.add("Baño diario");
        if (!pract.isEmpty()) r.put("higiene.practicas", String.join(",", pract));

        // SALUD
        List<String> enfs = new ArrayList<>();
        if (eqSi(r.get("salud.enf_diarrea")))      enfs.add("Diarrea");
        if (eqSi(r.get("salud.enf_vomito")))       enfs.add("Vómito");
        if (eqSi(r.get("salud.enf_colera")))       enfs.add("Cólera");
        if (eqSi(r.get("salud.enf_hepatitis")))    enfs.add("Hepatitis");
        if (eqSi(r.get("salud.enf_parasitosis")))  enfs.add("Parásitos");
        if (eqSi(r.get("salud.enf_otro")))         enfs.add("Otro");
        if (eqSi(r.get("salud.enf_ninguna")))      enfs.add("Ninguna");
        if (!enfs.isEmpty()) r.put("salud.enfermedades", String.join(",", enfs));

        row.clear();
        row.putAll(r);
    }

    private static boolean eqSi(String v) { return v != null && v.equalsIgnoreCase("Si"); }

    private static void move(Map<String,String> map, String from, String to) {
        if (from.equals(to)) return;
        String v = map.get(from);
        if (v != null && !v.isEmpty()) {
            map.put(to, v);
            map.remove(from);
        }
    }

    /* =================== Cabecera y escritura =================== */

    private static List<String> buildHeader(List<Map<String,String>> rows, List<String> secOrder) {
        Map<String, List<String>> bySec = new LinkedHashMap<>();
        for (String sec : secOrder) bySec.put(sec + ".", new ArrayList<>());

        for (Map<String,String> r : rows) {
            for (String k : r.keySet()) {
                for (String secPref : bySec.keySet()) {
                    if (k.startsWith(secPref)) {
                        List<String> list = bySec.get(secPref);
                        if (!list.contains(k)) list.add(k);
                        break;
                    }
                }
            }
        }

        List<String> header = new ArrayList<>();
        for (String secPref : bySec.keySet()) header.addAll(bySec.get(secPref));
        return header;
    }

    private static void writeCsv(File out, List<String> header, List<Map<String,String>> rows) throws Exception {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(out, false))) {
            bw.write(String.join(",", quoteList(header)));
            bw.newLine();
            for (Map<String,String> r : rows) {
                List<String> cols = new ArrayList<>(header.size());
                for (String h : header) cols.add(csvQuote(r.getOrDefault(h, "")));
                bw.write(String.join(",", cols));
                bw.newLine();
            }
        }
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

    /* =================== Dump de claves (para que encuentres el alias que falta) =================== */

    private static void writeKeysDump(File filesDir, Set<String> keysSeen) {
        try {
            File dirCsv = new File(filesDir, "csv");
            if (!dirCsv.exists()) dirCsv.mkdirs();
            File dump = new File(dirCsv, "primeravisita_keys_dump.txt");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(dump, false))) {
                for (String k : keysSeen) {
                    bw.write(k);
                    bw.newLine();
                }
            }
        } catch (Exception ignored) {}
    }
}
