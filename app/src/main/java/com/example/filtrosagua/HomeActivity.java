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
            SessionCsv.commitToMaster(this);          // seguimiento -> master.csv
            SessionCsvPrimera.commitToMaster(this);   // primera visita -> primeravisita_master.csv

            // 2) Mostrar rutas de los maestros consolidados
            File segMaster = SessionCsv.masterFile(this);
            File pvMaster = SessionCsvPrimera.fMaster(this);

            String rutaSeg = (segMaster != null) ? segMaster.getAbsolutePath() : "(sin archivo)";
            String rutaPv = (pvMaster != null) ? pvMaster.getAbsolutePath() : "(sin archivo)";

            Toast.makeText(
                    this,
                    "Guardado:\n- Seguimiento: " + rutaSeg + "\n- Primera visita: " + rutaPv,
                    Toast.LENGTH_LONG
            ).show();

            // 3) Limpiar archivos de sesión (staging) para próxima encuesta
            SessionCsv.clearSession(this);
            SessionCsvPrimera.clearSession(this);

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
            SessionCsv.commitToMaster(this);          // seguimiento -> master.csv
            SessionCsvPrimera.commitToMaster(this);   // primera visita -> primeravisita_master.csv

            // 2) (Opcional) Exportar a Descargas/FiltrosAgua (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                File segMaster = SessionCsv.masterFile(this);
                if (segMaster.exists() && segMaster.length() > 0) {
                    ExportUtils.exportToDownloads(
                            this,
                            segMaster,
                            "seguimientos_master_" + System.currentTimeMillis() + ".csv"
                    );
                }

                File pvMaster = SessionCsvPrimera.fMaster(this);
                if (pvMaster.exists() && pvMaster.length() > 0) {
                    ExportUtils.exportToDownloads(
                            this,
                            pvMaster,
                            "primeravisita_master_" + System.currentTimeMillis() + ".csv"
                    );
                }

                Toast.makeText(this, "Exportado(s) a Descargas/FiltrosAgua", Toast.LENGTH_LONG).show();
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
