package com.example.filtrosagua.util;

import android.content.Context;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CsvMerge {

    private static final String HEADER_UNIFICADO = "\"origen\",\"seccion\",\"campo\",\"valor\"";

    /** Une master.csv (seguimiento) y primeravisita_master.csv en un nuevo CSV. */
    public static File crearMaestroUnificado(Context ctx) throws IOException {
        File dir = new File(ctx.getFilesDir(), "csv");
        if (!dir.exists()) dir.mkdirs();

        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File out = new File(dir, "maestro_unificado_" + ts + ".csv");

        // Archivos fuente
        File seg = SessionCsv.masterFile(ctx);       // /files/csv/master.csv
        File prv = SessionCsvPrimera.fMaster(ctx);   // /files/csv/primeravisita_master.csv

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(out, false))) {
            // Cabecera unificada
            bw.write(HEADER_UNIFICADO);
            bw.newLine();

            // 1) Seguimiento
            appendConOrigen(bw, seg, "seguimiento");

            // 2) Primera visita
            appendConOrigen(bw, prv, "primera_visita");
        }
        return out;
    }

    private static void appendConOrigen(BufferedWriter bw, File src, String origen) throws IOException {
        if (src == null || !src.exists() || src.length() == 0) return;
        try (BufferedReader br = new BufferedReader(new FileReader(src))) {
            String line; boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { // saltar encabezado "seccion","campo","valor"
                    first = false;
                    continue;
                }
                if (line.trim().isEmpty()) continue;
                // Prefija el origen: "origen", + línea original
                bw.write("\"" + origen + "\"," + line);
                bw.newLine();
            }
        }
    }
}
