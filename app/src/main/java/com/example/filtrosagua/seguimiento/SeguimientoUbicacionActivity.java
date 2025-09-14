package com.example.filtrosagua.seguimiento;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.filtrosagua.MainActivity;
import com.example.filtrosagua.R;
import com.example.filtrosagua.util.Prefs;
import com.example.filtrosagua.util.SessionCsvSeguimiento;
import com.google.android.material.button.MaterialButton;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class SeguimientoUbicacionActivity extends AppCompatActivity {

    private EditText etLatitud, etAltitud;

    private static final String K_LAT = "seg6_latitud";
    private static final String K_ALT = "seg6_altitud";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seguimiento_ubicacion);

        etLatitud = req(R.id.etLatitud);
        etAltitud = req(R.id.etAltitud);
        MaterialButton btnAnterior = req(R.id.btnAnteriorSeg6);
        MaterialButton btnEnviar   = req(R.id.btnEnviarEncuesta);

        // Rellenar + autosave
        etLatitud.setText(Prefs.get(this, K_LAT));
        etAltitud.setText(Prefs.get(this, K_ALT));
        watch(etLatitud, K_LAT);
        watch(etAltitud, K_ALT);

        btnAnterior.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, SeguimientoObservacionesTecnicasActivity.class));
            finish();
        });

        btnEnviar.setOnClickListener(v -> {
            try {
                // 1) Guardar esta sección en STAGING (formato "long": seccion,campo,valor)
                saveSectionNow();

                // 2) Consolidar todo STAGING a WIDE (una fila) -> seguimiento_master_wide.csv
                SessionCsvSeguimiento.commitToMasterWide(this);

                // 3) Limpiar staging/autosave y volver a inicio
                SessionCsvSeguimiento.clearSession(this);
                Prefs.clearAll(this);

                Toast.makeText(this, "Encuesta guardada.", Toast.LENGTH_LONG).show();

                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();

            } catch (Exception e) {
                Toast.makeText(this, "Error al enviar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSectionNow();
    }

    /** Guarda la sección 'ubicacion' con claves canónicas 'ubicacion.latitud' y 'ubicacion.altitud'. */
    private void saveSectionNow() {
        try {
            Map<String, String> data = new LinkedHashMap<>();
            data.put("ubicacion.latitud", t(etLatitud));
            data.put("ubicacion.altitud", t(etAltitud));
            SessionCsvSeguimiento.saveSection(this, "ubicacion", data);
        } catch (Exception ignored) {}
    }

    /* ---------- helpers ---------- */

    private void watch(EditText et, String key) {
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { Prefs.put(SeguimientoUbicacionActivity.this, key, s.toString()); }
        });
    }

    private String t(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    @SuppressWarnings("unchecked")
    private <T> T req(int id) {
        T v = (T) findViewById(id);
        return Objects.requireNonNull(v, "Falta la vista con id: " + getResources().getResourceEntryName(id));
    }
}
