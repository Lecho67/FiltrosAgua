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
 * Maestro wide:    /files/csv/primeravisita_master_wide.csv -> 1 fila por encuesta
 *
 * En el maestro wide la fila se guarda así (orden inicial):
 *   timestamp, ubicacion.latitud, ubicacion.altitud, info_responsable.cedula,
 *   ubicacion.municipio, ubicacion.vereda_corregimiento, ubicacion.direccion, ...
 */
public class SessionCsvPrimera {

    /* ====== rutas ====== */
    public static File file(Context ctx)         { return new File(ensureCsvDir(ctx), "primeravisita.csv"); }
    public static File fMaster(Context ctx)      { return new File(ensureCsvDir(ctx), "primeravisita_master.csv"); }
    public static File fMasterWide(Context ctx)  { return new File(ensureCsvDir(ctx), "primeravisita_master_wide.csv"); }

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
     * Toma el staging long (primeravisita.csv), lo aplana a columnas fijas y
     * hace append de UNA FILA a primeravisita_master_wide.csv con el orden deseado.
     * Luego limpia el staging.
     */
    public static void commitToMasterWide(Context ctx) throws Exception {
        File staging = file(ctx);
        if (!staging.exists() || staging.length() == 0) return;

        // 1) Parsear el staging "long" -> mapa columna->valor (wide) para esta encuesta
        Map<String,String> row = parseStagingToWideRow(staging);

        // 2) Insertar timestamp (epoch ms) como primera columna lógica
        row.put("timestamp", String.valueOf(System.currentTimeMillis()));

        // 3) Escribir/append al maestro wide con cabecera fija y orden solicitado
        File masterWide = fMasterWide(ctx);
        boolean writeHeader = !masterWide.exists() || masterWide.length() == 0;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(masterWide, true))) {
            if (writeHeader) {
                bw.write(String.join(",", quoteList(WIDE_COLUMNS)));
                bw.newLine();
            }
            List<String> out = new ArrayList<>(WIDE_COLUMNS.size());
            for (String col : WIDE_COLUMNS) {
                out.add(csvQuote(row.getOrDefault(col, "")));
            }
            bw.write(String.join(",", out));
            bw.newLine();
        }

        // 4) Limpiar staging para la siguiente encuesta
        clearSession(ctx);
    }

    /* ====== columnas fijas (en orden) para el CSV wide ======
       Primero timestamp, latitud, altitud, cédula del responsable, municipio, vereda/corregimiento,
       dirección; luego el resto de variables del formulario. */
    private static final List<String> WIDE_COLUMNS = Arrays.asList(
            // Orden requerido (bloque inicial):
            "timestamp",
            "ubicacion.latitud",
            "ubicacion.altitud",
            "info_responsable.cedula",
            "ubicacion.municipio",
            "ubicacion.vereda_corregimiento",
            "ubicacion.direccion",

            // Resto (puedes ajustar/añadir aquí si cambias el formulario)
            "info_responsable.fecha",
            "info_responsable.responsable",
            "info_responsable.empresa",

            // beneficiario
            "beneficiario.tipo_beneficiario",
            "beneficiario.grupo_poblacional",
            "beneficiario.nombre_beneficiario",
            "beneficiario.cedula",
            "beneficiario.telefono",

            // ubicación extra (departamento queda después del bloque base)
            "ubicacion.departamento",

            // demografía
            "demografia.menor_5",
            "demografia.entre_6_17",
            "demografia.entre_18_64",
            "demografia.mayor_65",

            // acceso_agua
            "acceso_agua.tiene_agua",
            "acceso_agua.fuente_respuesta",
            "acceso_agua.usa_otra_fuente",
            "acceso_agua.administra_servicio",
            "acceso_agua.horas_dia",

            // desplazamiento
            "desplazamiento.necesita_desplazarse",
            "desplazamiento.medio_utiliza",
            "desplazamiento.tiempo_min",

            // percepción agua
            "percepcion_agua.percepcion",
            "percepcion_agua.opinion_sabor",
            "percepcion_agua.aspecto",
            "percepcion_agua.presenta_olores",

            // almacenamiento / tratamiento
            "almacenamiento_tratamiento.tanque",
            "almacenamiento_tratamiento.tratamientos",
            "almacenamiento_tratamiento.hierve_como",
            "almacenamiento_tratamiento.quien_labores",
            "almacenamiento_tratamiento.gasto_mensual",

            // contaminación / protección
            "contaminacion.contacto_fuentes",
            "contaminacion.fuente_protegida",
            "contaminacion.importancia_consumir_buena",
            "contaminacion.beneficios",

            // saneamiento
            "saneamiento.taza",
            "saneamiento.sistema_residuos",

            // higiene
            "higiene.capacitacion",
            "higiene.practicas",

            // salud
            "salud.dolor_estomago",
            "salud.enfermedades",
            "salud.observaciones"
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
