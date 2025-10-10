package com.example.filtrosagua;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.filtrosagua.util.Prefs;
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
            SessionCsvPrimera.clearSession(this); // primera long
            SessionCsvSeguimiento.clearSession(this); // seguimiento long

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
}
