package com.example.filtrosagua;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.filtrosagua.util.Prefs;
import com.example.filtrosagua.util.SessionCsvPrimera;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class PrimeraVisitaPercepcionAguaActivity extends AppCompatActivity {

    private TextInputEditText etPercepcion, etSabor, etClaridad;
    private RadioGroup rgOlores;

    private static final String K_PERCEP  = "pv7_percepcion";
    private static final String K_SABOR   = "pv7_sabor";
    private static final String K_CLAR    = "pv7_claridad";
    private static final String K_OLORES  = "pv7_olores";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita_percepcion_agua);

        etPercepcion = req(R.id.etPercepcionAguaPv);
        etSabor      = req(R.id.etSaborPv);
        etClaridad   = req(R.id.etClaridadPv);
        rgOlores     = req(R.id.rgOloresPv);
        MaterialButton btnAnt = req(R.id.btnAnteriorPv7);
        MaterialButton btnSig = req(R.id.btnSiguientePv7);

        // Rellenar Prefs
        etPercepcion.setText(Prefs.get(this, K_PERCEP));
        etSabor.setText(Prefs.get(this, K_SABOR));
        etClaridad.setText(Prefs.get(this, K_CLAR));
        setRadioFromPrefs();

        // Autosave
        auto(etPercepcion, K_PERCEP);
        auto(etSabor,      K_SABOR);
        auto(etClaridad,   K_CLAR);
        rgOlores.setOnCheckedChangeListener((g, id) ->
                Prefs.put(this, K_OLORES, id == R.id.rbOloresSiPv ? "Si" : "No"));

        // Navegación
        btnAnt.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, PrimeraVisitaDesplazamientoActivity.class));
            finish();
        });
        btnSig.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, PrimeraVisitaAlmacenamientoActivity.class));
            finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSectionNow();
    }

    /** Guarda esta sección con claves canónicas para el CSV en filas. */
    private void saveSectionNow() {
        try {
            Map<String, String> data = new LinkedHashMap<>();
            data.put("percepcion_agua.percepcion", t(etPercepcion));
            data.put("percepcion_agua.sabor",      t(etSabor));
            data.put("percepcion_agua.aspecto",    t(etClaridad)); // clara/oscura/por temporada
            data.put("percepcion_agua.olor",       getRadioSiNo());

            SessionCsvPrimera.saveSection(this, "percepcion_agua", data);
        } catch (Exception e) {
            Toast.makeText(this, "Error guardando: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ---- helpers ----
    private void setRadioFromPrefs() {
        String v = Prefs.get(this, K_OLORES);
        if ("Si".equalsIgnoreCase(v)) rgOlores.check(R.id.rbOloresSiPv);
        else if ("No".equalsIgnoreCase(v)) rgOlores.check(R.id.rbOloresNoPv);
        else rgOlores.clearCheck();
    }

    private String getRadioSiNo() {
        int id = rgOlores.getCheckedRadioButtonId();
        if (id == -1) return "";
        return (id == R.id.rbOloresSiPv) ? "Si" : "No";
    }

    private void auto(TextInputEditText et, String key) {
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                Prefs.put(PrimeraVisitaPercepcionAguaActivity.this, key, s.toString());
            }
        });
    }

    private String t(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    @SuppressWarnings("unchecked")
    private <T> T req(int id) {
        T v = (T) findViewById(id);
        return Objects.requireNonNull(v,
                "Falta la vista con id: " + getResources().getResourceEntryName(id));
    }
}
