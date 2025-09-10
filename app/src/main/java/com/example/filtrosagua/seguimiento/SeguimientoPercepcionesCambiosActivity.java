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

public class SeguimientoPercepcionesCambiosActivity extends AppCompatActivity {

    // EditTexts (IDs tal como están en TU XML)
    private EditText etCambios;     // etCambiosNotados
    private EditText etPercepcion;  // etPercepcionAgua
    private EditText etSabor;       // etSaborAgua
    private EditText etGastoActual; // etGastoActual

    // Radios
    private RadioGroup rgColor, rgOlor, rgEnfDisminuyen, rgGastosDisminuyen;
    private RadioButton rbColorSi, rbColorNo, rbOlorSi, rbOlorNo;

    // Prefs keys (autosave)
    private static final String K_CAMBIOS = "seg3_cambios";
    private static final String K_PERCEP  = "seg3_percepcion";
    private static final String K_SABOR   = "seg3_sabor";
    private static final String K_COLOR   = "seg3_color";   // "Si"/"No"/""
    private static final String K_OLOR    = "seg3_olor";    // "Si"/"No"/""
    private static final String K_ENF     = "seg3_enfermedades_disminuyen"; // "Si"/"No"/""
    private static final String K_GASTOS  = "seg3_gastos_disminuyen";       // "Si"/"No"/""
    private static final String K_GASTO   = "seg3_gasto_actual";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seguimiento_percepciones_cambios);

        // ==== Bind de vistas (IDs exactos del XML) ====
        etCambios     = req(R.id.etCambiosNotados);
        etPercepcion  = req(R.id.etPercepcionAgua);
        etSabor       = req(R.id.etSaborAgua);
        etGastoActual = req(R.id.etGastoActual);

        rgColor            = req(R.id.rgColor);
        rgOlor             = req(R.id.rgOlor);
        rgEnfDisminuyen    = req(R.id.rgEnfDisminuyen);
        rgGastosDisminuyen = req(R.id.rgGastosDisminuyen);

        rbColorSi = req(R.id.rbColorSi);
        rbColorNo = req(R.id.rbColorNo);
        rbOlorSi  = req(R.id.rbOlorSi);
        rbOlorNo  = req(R.id.rbOlorNo);

        MaterialButton btnAnterior  = req(R.id.btnAnteriorSeg3);
        MaterialButton btnSiguiente = req(R.id.btnSiguienteSeg3);

        // ==== Relleno desde Prefs ====
        etCambios.setText(Prefs.get(this, K_CAMBIOS));
        etPercepcion.setText(Prefs.get(this, K_PERCEP));
        etSabor.setText(Prefs.get(this, K_SABOR));
        etGastoActual.setText(Prefs.get(this, K_GASTO));

        setRadioFromPrefs(rgColor,  Prefs.get(this, K_COLOR),  rbColorSi, rbColorNo);
        setRadioFromPrefs(rgOlor,   Prefs.get(this, K_OLOR),   rbOlorSi, rbOlorNo);
        setRadioFromPrefs(rgEnfDisminuyen,    Prefs.get(this, K_ENF),    null, null);
        setRadioFromPrefs(rgGastosDisminuyen, Prefs.get(this, K_GASTOS), null, null);

        // ==== Autosave EditTexts ====
        autosaveText(etCambios,     K_CAMBIOS);
        autosaveText(etPercepcion,  K_PERCEP);
        autosaveText(etSabor,       K_SABOR);
        autosaveText(etGastoActual, K_GASTO);

        // ==== Autosave RadioGroups (Si/No) ====
        rgColor.setOnCheckedChangeListener((g, id) -> {
            Prefs.put(this, K_COLOR, id == R.id.rbColorSi ? "Si" : id == R.id.rbColorNo ? "No" : "");
            saveSectionNow(); // guardado oportunista
        });
        rgOlor.setOnCheckedChangeListener((g, id) -> {
            Prefs.put(this, K_OLOR, id == R.id.rbOlorSi ? "Si" : id == R.id.rbOlorNo ? "No" : "");
            saveSectionNow();
        });
        rgEnfDisminuyen.setOnCheckedChangeListener((g, id) -> {
            Prefs.put(this, K_ENF, checkedToSiNo(g));
            saveSectionNow();
        });
        rgGastosDisminuyen.setOnCheckedChangeListener((g, id) -> {
            Prefs.put(this, K_GASTOS, checkedToSiNo(g));
            saveSectionNow();
        });

        // ==== Navegación ====
        btnAnterior.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, SeguimientoAccesoAguaFiltroActivity.class));
            finish();
        });

        btnSiguiente.setOnClickListener(v -> {
            try {
                saveSectionNow();
                startActivity(new Intent(this, SeguimientoMantenimientoActivity.class));
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "Error guardando: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Guardar también si el usuario minimiza/cambia de pantalla. */
    @Override
    protected void onPause() {
        super.onPause();
        saveSectionNow();
    }

    // ================= helpers =================

    /** Construye el mapa de la sección con valores actuales (o Prefs si tocaron radios). */
    private Map<String, String> buildData() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("cambios",    t(etCambios));
        m.put("percepcion", t(etPercepcion));
        m.put("sabor",      t(etSabor));
        m.put("color",      Prefs.get(this, K_COLOR));
        m.put("olor",       Prefs.get(this, K_OLOR));
        m.put("enfermedades_disminuyen", Prefs.get(this, K_ENF));
        m.put("gastos_disminuyen",       Prefs.get(this, K_GASTOS));
        m.put("gasto_actual", t(etGastoActual));
        return m;
    }

    /** Upsert de la sección en seguimiento.csv (sin duplicados). */
    private void saveSectionNow() {
        try {
            SessionCsv.saveSection(this, "percepciones_cambios", buildData());
        } catch (Exception ignored) {
            // evitamos crasheos por IO momentáneo
        }
    }

    private void autosaveText(EditText et, String key) {
        // guarda el valor actual por si vino prellenado
        Prefs.put(this, key, t(et));
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                Prefs.put(SeguimientoPercepcionesCambiosActivity.this, key, s.toString());
                saveSectionNow(); // guardado oportunista en cada cambio
            }
        });
    }

    /** Pone el radio seleccionado según Prefs ("Si"/"No"). Si no hay radios Si/No, usa el primer/segundo child. */
    private void setRadioFromPrefs(RadioGroup group, String val, RadioButton rbSi, RadioButton rbNo) {
        if ("Si".equalsIgnoreCase(val)) {
            if (rbSi != null) rbSi.setChecked(true);
            else if (group.getChildCount() > 0) ((RadioButton) group.getChildAt(0)).setChecked(true);
        } else if ("No".equalsIgnoreCase(val)) {
            if (rbNo != null) rbNo.setChecked(true);
            else if (group.getChildCount() > 1) ((RadioButton) group.getChildAt(1)).setChecked(true);
        } else {
            group.clearCheck();
        }
    }

    /** Convierte selección del grupo a "Si"/"No"/"" (asumiendo 1er hijo = Sí, 2do = No). */
    private String checkedToSiNo(RadioGroup g) {
        int idx = g.indexOfChild(g.findViewById(g.getCheckedRadioButtonId()));
        if (idx == 0) return "Si";
        if (idx == 1) return "No";
        return "";
        // Si tus grupos no siguen este orden, ajusta a IDs concretos como en color/olor.
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
