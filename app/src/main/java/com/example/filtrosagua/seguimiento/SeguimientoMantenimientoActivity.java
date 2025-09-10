package com.example.filtrosagua.seguimiento;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.filtrosagua.R;
import com.example.filtrosagua.util.Prefs;
import com.example.filtrosagua.util.SessionCsv;
import com.google.android.material.button.MaterialButton;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class SeguimientoMantenimientoActivity extends AppCompatActivity {

    // Vistas (según tu XML)
    private EditText   etFrecuenciaMant, etProdArcilla, etProdPlastico;
    private RadioGroup rgVidaUtil, rgRepuestos;
    private RadioButton rbVidaSi, rbVidaNo, rbRepSi, rbRepNo;

    // Claves Prefs
    private static final String K_FREC  = "seg4_frecuencia";
    private static final String K_ARC   = "seg4_prod_arcilla";
    private static final String K_PLAS  = "seg4_prod_plastico";
    private static final String K_VIDA  = "seg4_vida_util";   // "Si" / "No" / ""
    private static final String K_REP   = "seg4_repuestos";   // "Si" / "No" / ""

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seguimiento_mantenimiento);

        // Binds
        etFrecuenciaMant = req(R.id.etFrecuenciaMantenimiento);
        etProdArcilla    = req(R.id.etProductosArcilla);
        etProdPlastico   = req(R.id.etProductosPlastico);

        rgVidaUtil       = req(R.id.rgVidaUtil);
        rbVidaSi         = req(R.id.rbVidaSi);
        rbVidaNo         = req(R.id.rbVidaNo);

        rgRepuestos      = req(R.id.rgRepuestos);
        rbRepSi          = req(R.id.rbRepSi);
        rbRepNo          = req(R.id.rbRepNo);

        MaterialButton btnAnterior  = req(R.id.btnAnteriorSeg4);
        MaterialButton btnSiguiente = req(R.id.btnSiguienteSeg4);

        // Rellenar desde Prefs
        etFrecuenciaMant.setText(Prefs.get(this, K_FREC));
        etProdArcilla.setText(Prefs.get(this, K_ARC));
        etProdPlastico.setText(Prefs.get(this, K_PLAS));
        setRadioFromPrefs(rgVidaUtil, Prefs.get(this, K_VIDA), rbVidaSi, rbVidaNo);
        setRadioFromPrefs(rgRepuestos, Prefs.get(this, K_REP), rbRepSi, rbRepNo);

        // Autosave texto
        autosaveText(etFrecuenciaMant, K_FREC);
        autosaveText(etProdArcilla,    K_ARC);
        autosaveText(etProdPlastico,   K_PLAS);

        // Autosave radios
        rgVidaUtil.setOnCheckedChangeListener((g, id) -> {
            Prefs.put(this, K_VIDA, id == R.id.rbVidaSi ? "Si" : (id == R.id.rbVidaNo ? "No" : ""));
            saveSectionNow();
        });
        rgRepuestos.setOnCheckedChangeListener((g, id) -> {
            Prefs.put(this, K_REP, id == R.id.rbRepSi ? "Si" : (id == R.id.rbRepNo ? "No" : ""));
            saveSectionNow();
        });

        // Navegación
        btnAnterior.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, SeguimientoPercepcionesCambiosActivity.class));
            finish();
        });

        btnSiguiente.setOnClickListener(v -> {
            try {
                saveSectionNow();
                startActivity(new Intent(this, SeguimientoObservacionesTecnicasActivity.class));
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "Error guardando: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSectionNow();
    }

    // ---- Guardado en CSV (upsert de sección "mantenimiento") ----
    private void saveSectionNow() {
        try {
            Map<String, String> data = new LinkedHashMap<>();
            data.put("frecuencia_mantenimiento", t(etFrecuenciaMant));
            data.put("productos_limpieza_arcilla", t(etProdArcilla));
            data.put("productos_limpieza_plastico", t(etProdPlastico));
            data.put("conoce_vida_util_arcilla", selected(rgVidaUtil));
            data.put("sabe_donde_conseguir_repuestos", selected(rgRepuestos));

            // Guarda/actualiza sin duplicar filas de esta sección
            SessionCsv.saveSection(this, "mantenimiento", data);

        } catch (Exception ignored) {
            // evita crasheos por IO momentáneo
        }
    }

    // ---- Helpers ----
    private void autosaveText(EditText et, String key) {
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                Prefs.put(SeguimientoMantenimientoActivity.this, key, s.toString());
                saveSectionNow();
            }
        });
        // Guarda valor actual si ya venía con datos
        Prefs.put(this, key, t(et));
    }

    private void setRadioFromPrefs(RadioGroup group, String val, RadioButton rbSi, RadioButton rbNo) {
        if ("Si".equalsIgnoreCase(val)) rbSi.setChecked(true);
        else if ("No".equalsIgnoreCase(val)) rbNo.setChecked(true);
        else group.clearCheck();
    }

    private String selected(RadioGroup group) {
        int id = group.getCheckedRadioButtonId();
        if (id == -1) return "";
        RadioButton rb = findViewById(id);
        String txt = rb == null ? "" : rb.getText().toString().trim();
        // Normalizar a "Si"/"No" por si el texto cambia
        if ("si".equalsIgnoreCase(txt)) return "Si";
        if ("no".equalsIgnoreCase(txt)) return "No";
        return txt;
    }

    private String t(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    @SuppressWarnings("unchecked")
    private <T> T req(int id) {
        T v = (T) findViewById(id);
        return Objects.requireNonNull(v,
                "Falta la vista con id: " + getResources().getResourceEntryName(id));
    }
}
