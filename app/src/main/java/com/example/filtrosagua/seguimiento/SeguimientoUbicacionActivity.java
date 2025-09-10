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
import com.example.filtrosagua.util.SessionCsv;
import com.google.android.material.button.MaterialButton;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class SeguimientoUbicacionActivity extends AppCompatActivity {

    private EditText etUbicacion;

    // autosave
    private static final String K_UBIC = "seg6_ubicacion";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seguimiento_ubicacion);

        etUbicacion = req(R.id.etUbicacion);
        MaterialButton btnAnterior = req(R.id.btnAnteriorSeg6);
        MaterialButton btnEnviar   = req(R.id.btnEnviarEncuesta);

        // Rellenar + autosave
        etUbicacion.setText(Prefs.get(this, K_UBIC));
        etUbicacion.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                Prefs.put(SeguimientoUbicacionActivity.this, K_UBIC, s.toString());
            }
        });

        btnAnterior.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, SeguimientoObservacionesTecnicasActivity.class));
            finish();
        });

        btnEnviar.setOnClickListener(v -> {
            try {
                // 1) Guardar sección
                saveSectionNow();
                // 2) Consolidar seguimiento.csv -> master.csv
                SessionCsv.commitToMaster(this);

                // 3) Mostrar ruta del maestro
                java.io.File m = SessionCsv.masterFile(this);
                if (m != null) {
                    Toast.makeText(
                            this,
                            "Encuesta guardada en:\n" + m.getAbsolutePath(),
                            Toast.LENGTH_LONG
                    ).show();
                }

                // 4) Limpiar archivo de sesión para la próxima encuesta (opcional pero recomendado)
                SessionCsv.clearSession(this);

            } catch (Exception e) {
                Toast.makeText(this, "Error al enviar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }

            // 5) Limpiar autosave y volver al login
            Prefs.clearAll(this);
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSectionNow();
    }

    private void saveSectionNow() {
        try {
            Map<String, String> data = new LinkedHashMap<>();
            data.put("direccion_referencias", t(etUbicacion));
            SessionCsv.saveSection(this, "ubicacion", data);
        } catch (Exception ignored) {}
    }

    private String t(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    @SuppressWarnings("unchecked")
    private <T> T req(int id) {
        T v = (T) findViewById(id);
        return Objects.requireNonNull(
                v,
                "Falta la vista con id: " + getResources().getResourceEntryName(id)
        );
    }
}
