package com.example.filtrosagua.util;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

/**
 * Maneja PRIMERA VISITA en formato "long" (staging) y "wide" (maestro por filas).
 *
 * Staging (long):  /files/csv/primeravisita.csv         -> "seccion","campo","valor"
 * Maestro long:    /files/csv/primeravisita_master.csv  -> append long (si ya usas esto)
 * Maestro wide UNIFICADO ahora: /files/csv/encuestas_master_wide.csv -> 1 fila por encuesta (primera y seguimiento)
 *
 * En el maestro wide la fila se guarda así (orden inicial):
 *   timestamp, ubicacion.latitud, ubicacion.altitud, info_responsable.cedula,
 *   ubicacion.municipio, ubicacion.vereda_corregimiento, ubicacion.direccion, ...
 */
public class SessionCsvPrimera {

    /* ====== rutas ====== */
    public static File file(Context ctx)         { return new File(ensureCsvDir(ctx), "primeravisita.csv"); }
    public static File fMaster(Context ctx)      { return new File(ensureCsvDir(ctx), "primeravisita_master.csv"); }
    // Archivo unificado (mismo que usa seguimiento)
    public static File fMasterWide(Context ctx)  { return new File(ensureCsvDir(ctx), "encuestas_master_wide.csv"); }

    private static File ensureCsvDir(Context ctx) {
        File dir = new File(ctx.getFilesDir(), "csv");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /* ====== API "long" ====== */

    /** Guardar/actualizar sección en primeravisita.csv (staging temporal, formato long). */
    public static void saveSection(Context ctx, String section, Map<String, String> data) throws Exception {
        CsvUtils.upsertSection(file(ctx), section, data);
    }

    /** Consolidar archivo de sesión -> maestro long (append ordenado). */
    public static void commitToMaster(Context ctx) throws Exception {
        CsvUtils.appendFileOrdered(file(ctx), fMaster(ctx));
    }

    /** Limpiar archivo de sesión para la próxima encuesta. */
    public static void clearSession(Context ctx) {
        File f = file(ctx);
        if (f.exists()) f.delete();
    }

    /* ====== API "wide": una fila por encuesta ====== */

    /**
     * Commit en formato WIDE al archivo unificado.
     * Reglas de cabecera (compartida con seguimiento):
     * BASE_ORDER definida abajo y luego columnas nuevas en orden alfabético
     */
    public static synchronized void commitToMasterWide(Context ctx) throws Exception {
        File staging = file(ctx);
        if (!staging.exists() || staging.length() == 0) return;

        Map<String,String> row = parseStagingToWideRow(staging);
        // Migración: si quedó un "timestamp" previo lo mapeamos a timestamp_ms sólo si no existe
        if (row.containsKey("timestamp") && !row.containsKey("timestamp_ms")) {
            row.put("timestamp_ms", row.remove("timestamp"));
        }
        row.put("timestamp_ms", String.valueOf(System.currentTimeMillis()));
        row.put("tipo_formulario", "primera_visita");

        // Asegurar claves base del bloque unificado
        ensure(row, "ubicacion.municipio");
        ensure(row, "ubicacion.vereda_corregimiento");
        ensure(row, "ubicacion.direccion");
        ensure(row, "ubicacion.latitud");
        ensure(row, "ubicacion.altitud");
        ensure(row, "info_responsable.cedula");
        ensure(row, "info_basica.cedula"); // quedará vacío en primera visita

        File master = fMasterWide(ctx);
        if (!master.exists() || master.length()==0) {
            List<String> header = buildHeaderForNewFile(row.keySet());
            writeMasterWithHeaderAndOneRow(master, header, row);
        } else {
            List<String> header = readHeader(master);
            List<String> merged = mergeHeaderPreservingBase(header, row.keySet());
            if (!merged.equals(header)) {
                List<List<String>> oldRows = readRows(master);
                rewriteMasterWithNewHeader(master, header, merged, oldRows);
                header = merged;
            }
            appendRow(master, header, row);
        }
        clearSession(ctx);
    }

    // Cabecera base unificada (mismo orden que en seguimiento adaptado)
    private static final List<String> BASE_ORDER = Arrays.asList(
        "timestamp_ms",
        "tipo_formulario",
        "ubicacion.municipio",
        "ubicacion.vereda_corregimiento",
        "ubicacion.direccion",
        "ubicacion.latitud",
        "ubicacion.altitud",
        "info_responsable.cedula",
        "info_basica.cedula" // quedará vacío aquí
    );

    /* ====== helpers: de long -> wide row ====== */

    /** Convierte el staging "long" a un mapa de columnas fijas (wide) para UNA encuesta. */
    private static Map<String,String> parseStagingToWideRow(File staging) throws Exception {
        Map<String,String> row = new LinkedHashMap<>();
        // Lee todo el staging y arma clave "seccion.campo"
        try (BufferedReader br = new BufferedReader(new FileReader(staging))) {
            String line = br.readLine();
            boolean headerSeen = isHeader(line);
            if (!headerSeen) {
                if (line != null) putLine(row, line);
            }
            while ((line = br.readLine()) != null) {
                putLine(row, line);
            }
        }
        // Normalización de alias
        normalizeRow(row);
        return row;
    }

    /* ====== Soporte cabecera dinámica unificada ====== */
    private static List<String> buildHeaderForNewFile(Set<String> keys) {
        LinkedHashSet<String> rest = new LinkedHashSet<>(keys);
        rest.removeAll(BASE_ORDER);
        List<String> tail = new ArrayList<>(rest);
        Collections.sort(tail);
        List<String> header = new ArrayList<>(BASE_ORDER);
        header.addAll(tail);
        return header;
    }
    private static List<String> readHeader(File master) throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(master))) {
            String first = br.readLine();
            if (first == null) return new ArrayList<>();
            return splitCsvLine(first);
        }
    }
    private static List<List<String>> readRows(File master) throws Exception {
        List<List<String>> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(master))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) rows.add(splitCsvLine(line));
        }
        return rows;
    }
    private static List<String> splitCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQ=false;
        for (int i=0;i<line.length();i++) {
            char ch=line.charAt(i);
            if (ch=='"') {
                if (inQ && i+1<line.length() && line.charAt(i+1)=='"') { sb.append('"'); i++; }
                else inQ=!inQ;
            } else if (ch==',' && !inQ) {
                out.add(unquote(sb.toString())); sb.setLength(0);
            } else sb.append(ch);
        }
        out.add(unquote(sb.toString()));
        return out;
    }
    private static String unquote(String s){
        if (s==null) return ""; s=s.trim();
        if (s.length()>=2 && s.startsWith("\"") && s.endsWith("\""))
            s = s.substring(1,s.length()-1).replace("\"\"","\"");
        return s;
    }
    private static List<String> mergeHeaderPreservingBase(List<String> existing, Set<String> newKeys) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        merged.addAll(BASE_ORDER);
        for (String h: existing) if (!merged.contains(h)) merged.add(h);
        List<String> missing = new ArrayList<>();
        for (String k: newKeys) if (!merged.contains(k)) missing.add(k);
        Collections.sort(missing);
        merged.addAll(missing);
        return new ArrayList<>(merged);
    }
    private static void rewriteMasterWithNewHeader(File master, List<String> oldHeader, List<String> newHeader, List<List<String>> oldRows) throws Exception {
        Map<String,Integer> idx = new HashMap<>();
        for (int i=0;i<oldHeader.size();i++) idx.put(oldHeader.get(i), i);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(master,false))) {
            bw.write(String.join(",", newHeader)); bw.newLine();
            for (List<String> old: oldRows) {
                List<String> row = new ArrayList<>(newHeader.size());
                for (String col: newHeader) {
                    Integer j = idx.get(col);
                    String v = (j==null || j>=old.size())?"": old.get(j);
                    row.add(csvQuote(v));
                }
                bw.write(String.join(",", row)); bw.newLine();
            }
        }
    }
    private static void writeMasterWithHeaderAndOneRow(File master, List<String> header, Map<String,String> rowMap) throws Exception {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(master,false))) {
            bw.write(String.join(",", header)); bw.newLine();
            List<String> cols = new ArrayList<>(header.size());
            for (String h: header) cols.add(csvQuote(rowMap.getOrDefault(h, "")));
            bw.write(String.join(",", cols)); bw.newLine();
        }
    }
    private static void appendRow(File master, List<String> header, Map<String,String> rowMap) throws Exception {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(master,true))) {
            List<String> cols = new ArrayList<>(header.size());
            for (String h: header) cols.add(csvQuote(rowMap.getOrDefault(h, "")));
            bw.write(String.join(",", cols)); bw.newLine();
        }
    }
    private static void ensure(Map<String,String> m, String k){ if(!m.containsKey(k)) m.put(k, ""); }

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
        row.put(parts[0] + "." + parts[1], parts[2]);
    }

    /** Unifica nombres/alias a las claves finales usadas en WIDE_COLUMNS. */
    private static void normalizeRow(Map<String,String> r) {
        // beneficiario
        moveAny(r, Arrays.asList("beneficiario.tipo","beneficiario.tipo_benef"), "beneficiario.tipo_beneficiario");
        moveAny(r, Arrays.asList("beneficiario.grupo","beneficiario.poblacional"), "beneficiario.grupo_poblacional");

        // ubicación
        moveAny(r, Arrays.asList("ubicacion.vereda","ubicacion.corregimiento","ubicacion.vereda_correg"), "ubicacion.vereda_corregimiento");

        // acceso_agua
        moveAny(r, Arrays.asList("acceso_agua.tiene_agua","acceso_agua.dispone_agua"), "acceso_agua.tiene_agua");
        moveAny(r, Arrays.asList("acceso_agua.fuente_respuesta","acceso_agua.fuente_agua","acceso_agua.fuente"), "acceso_agua.fuente_respuesta");
        moveAny(r, Arrays.asList("acceso_agua.usa_otra_fuente","acceso_agua.otra_fuente","acceso_agua.usa_otra"), "acceso_agua.usa_otra_fuente");
        moveAny(r, Arrays.asList("acceso_agua.administra_servicio","acceso_agua.administrador_servicio","acceso_agua.administrador"), "acceso_agua.administra_servicio");
        moveAny(r, Arrays.asList("acceso_agua.horas_dia","acceso_agua.horas_disponibilidad_dia","acceso_agua.horas"), "acceso_agua.horas_dia");

        // desplazamiento
        moveAny(r, Arrays.asList("desplazamiento.necesita_desplazarse","desplazamiento.tiene_que_desplazarse"), "desplazamiento.necesita_desplazarse");
        moveAny(r, Arrays.asList("desplazamiento.medio_utiliza","desplazamiento.medio","desplazamiento.medio_transporte"), "desplazamiento.medio_utiliza");
        moveAny(r, Arrays.asList("desplazamiento.tiempo_min","desplazamiento.minutos","desplazamiento.tiempo"), "desplazamiento.tiempo_min");

        // percepción_agua
        moveAny(r, Arrays.asList("percepcion_agua.percepcion","percepcion_agua.percepcion_agua"), "percepcion_agua.percepcion");
        moveAny(r, Arrays.asList("percepcion_agua.opinion_sabor","percepcion_agua.sabor"), "percepcion_agua.opinion_sabor");
        moveAny(r, Arrays.asList("percepcion_agua.aspecto","percepcion_agua.claridad_temporada","percepcion_agua.claridad"), "percepcion_agua.aspecto");
        moveAny(r, Arrays.asList("percepcion_agua.presenta_olores","percepcion_agua.olores"), "percepcion_agua.presenta_olores");

        // almacenamiento_tratamiento
        moveAny(r, Arrays.asList("almacenamiento_tratamiento.tanque","almacenamiento_tratamiento.almacena_tanque"), "almacenamiento_tratamiento.tanque");
        moveAny(r, Arrays.asList("almacenamiento_tratamiento.tratamientos","almacenamiento_tratamiento.metodos_tratamiento"), "almacenamiento_tratamiento.tratamientos");
        moveAny(r, Arrays.asList("almacenamiento_tratamiento.hierve_como","almacenamiento_tratamiento.hervir_emplea"), "almacenamiento_tratamiento.hierve_como");
        moveAny(r, Arrays.asList("almacenamiento_tratamiento.quien_labores","almacenamiento_tratamiento.quien_realiza"), "almacenamiento_tratamiento.quien_labores");

        // contaminación / protección
        moveAny(r, Arrays.asList("contaminacion.contacto_fuentes","contaminacion_proteccion.contaminacion_fuentes","contaminacion.contaminacion_fuentes"), "contaminacion.contacto_fuentes");
        moveAny(r, Arrays.asList("contaminacion.fuente_protegida","contaminacion_proteccion.fuente_protegida"), "contaminacion.fuente_protegida");
        moveAny(r, Arrays.asList("contaminacion.importancia_consumir_buena","contaminacion_proteccion.importante_consumir_potable"), "contaminacion.importancia_consumir_buena");
        moveAny(r, Arrays.asList("contaminacion.beneficios","contaminacion_proteccion.beneficios_consumir_potable"), "contaminacion.beneficios");

        // saneamiento
        moveAny(r, Arrays.asList("saneamiento.taza","saneamiento.sanitario_taza"), "saneamiento.taza");
        moveAny(r, Arrays.asList("saneamiento.sistema_residuos","saneamiento.sistema_disposicion"), "saneamiento.sistema_residuos");

        // higiene
        moveAny(r, Arrays.asList("higiene.capacitacion","higiene.capacitacion_higiene"), "higiene.capacitacion");
        List<String> pract = new ArrayList<>();
        if ("Si".equalsIgnoreCase(r.getOrDefault("higiene.practica_lavado_manos","")))   pract.add("Lavado de manos");
        if ("Si".equalsIgnoreCase(r.getOrDefault("higiene.practica_limpieza_hogar",""))) pract.add("Limpieza del hogar");
        if ("Si".equalsIgnoreCase(r.getOrDefault("higiene.practica_cepillado_dientes",""))) pract.add("Cepillado de dientes");
        if ("Si".equalsIgnoreCase(r.getOrDefault("higiene.practica_otro","")))           pract.add("Otro");
        if ("Si".equalsIgnoreCase(r.getOrDefault("higiene.practica_bano_diario","")))    pract.add("Baño diario");
        if (!pract.isEmpty()) r.put("higiene.practicas", String.join(",", pract));

        // salud
        List<String> enfs = new ArrayList<>();
        if ("Si".equalsIgnoreCase(r.getOrDefault("salud.enf_diarrea","")))      enfs.add("Diarrea");
        if ("Si".equalsIgnoreCase(r.getOrDefault("salud.enf_vomito","")))       enfs.add("Vómito");
        if ("Si".equalsIgnoreCase(r.getOrDefault("salud.enf_colera","")))       enfs.add("Cólera");
        if ("Si".equalsIgnoreCase(r.getOrDefault("salud.enf_hepatitis","")))    enfs.add("Hepatitis");
        if ("Si".equalsIgnoreCase(r.getOrDefault("salud.enf_parasitosis","")))  enfs.add("Parásitos");
        if ("Si".equalsIgnoreCase(r.getOrDefault("salud.enf_otro","")))         enfs.add("Otro");
        if ("Si".equalsIgnoreCase(r.getOrDefault("salud.enf_ninguna","")))      enfs.add("Ninguna");
        if (!enfs.isEmpty()) r.put("salud.enfermedades", String.join(",", enfs));
        moveAny(r, Arrays.asList("salud.observaciones","salud.obs"), "salud.observaciones");
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

    // Lee "a","b","c" en 3 campos sin romper comas escapadas
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

    // (quoteList ya no usado para cabecera fija, se mantiene compat si se necesitara en otro lado)
    private static List<String> quoteList(List<String> keys) { List<String> out=new ArrayList<>(keys.size()); for(String k:keys) out.add(csvQuote(k)); return out; }
    private static String csvQuote(String s) {
        if (s == null) s = "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
