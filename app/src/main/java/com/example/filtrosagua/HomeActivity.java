package com.example.filtrosagua;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.filtrosagua.util.ExportUtils;
import com.example.filtrosagua.util.Prefs;
import com.example.filtrosagua.util.SessionCsv;
import com.example.filtrosagua.util.SessionCsvPrimera;
import com.example.filtrosagua.util.SessionCsvSeguimiento;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        MaterialButton btnPrimera = findViewById(R.id.btnPrimera);
        MaterialButton btnSeguimiento = findViewById(R.id.btnSeguimiento);
        MaterialButton btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        // Abrir Primera Visita
        btnPrimera.setOnClickListener(v ->
                startActivity(new Intent(this, PrimeraVisitaActivity.class)));

        // Abrir Seguimiento (se irá llenando seguimiento.csv por secciones)
        btnSeguimiento.setOnClickListener(v ->
                startActivity(new Intent(this, com.example.filtrosagua.seguimiento.SeguimientoInfoBasicaActivity.class)));

        // Cerrar sesión = MISMO FLUJO que “Enviar encuesta” en Ubicación
        btnCerrarSesion.setOnClickListener(v -> confirmarCierreSesion());
    }

    private void confirmarCierreSesion() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cerrar sesión")
                .setMessage("Se guardará la encuesta actual y se cerrará la sesión. ¿Deseas continuar?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Sí, cerrar sesión", (d, w) -> cerrarSesionIgualQueUbicacion())
                .show();
    }

    private void cerrarSesionIgualQueUbicacion() {
        try {
            // 1) Consolidar encuestas en curso -> maestros
        // Consolidar cualquier staging a wide unificado
        try { SessionCsvPrimera.commitToMasterWide(this); } catch (Exception ignored) {}
        try { SessionCsvSeguimiento.commitToMasterWide(this); } catch (Exception ignored) {}

        // 2) Ruta del master unificado
        File unificado = new File(getFilesDir(), "csv/encuestas_master_wide.csv");
        String ruta = unificado.exists() ? unificado.getAbsolutePath() : "(sin archivo)";

            Toast.makeText(
                    this,
            "Guardado unificado:\n" + ruta,
                    Toast.LENGTH_LONG
            ).show();

            // 3) Limpiar archivos de sesión (staging) para próxima encuesta
            SessionCsv.clearSession(this);      // seguimiento legacy long
            SessionCsvPrimera.clearSession(this); // primera long

            // 4) Limpiar autosave
            Prefs.clearAll(this);

            // 5) Volver al login y limpiar back stack
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();

        } catch (Exception e) {
            Toast.makeText(this, "Error al cerrar sesión: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** Mismo flujo que en SeguimientoUbicacionActivity.btnEnviar pero exportando */
    private void cerrarSesionConExport() {
        try {
            // 1) Consolidar encuesta en curso -> maestros
            try { SessionCsvPrimera.commitToMasterWide(this); } catch (Exception ignored) {}
            try { SessionCsvSeguimiento.commitToMasterWide(this); } catch (Exception ignored) {}

            // 2) Exportar a Descargas/FiltrosAgua el unificado (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                File unificado = new File(getFilesDir(), "csv/encuestas_master_wide.csv");
                if (unificado.exists() && unificado.length() > 0) {
                    ExportUtils.exportToDownloads(
                            this,
                            unificado,
                            "encuestas_master_" + System.currentTimeMillis() + ".csv"
                    );
                    Toast.makeText(this, "Exportado a Descargas/FiltrosAgua", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "No hay datos unificados para exportar", Toast.LENGTH_LONG).show();
                }
            }

            // 3) Dejar ambos stagings limpios para próximas encuestas
            SessionCsv.clearSession(this);
            SessionCsvPrimera.clearSession(this);

            // 4) Limpiar autosave
            Prefs.clearAll(this);

            // 5) Volver al login
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();

        } catch (Exception e) {
            Toast.makeText(this, "Error al guardar/cerrar sesión: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
