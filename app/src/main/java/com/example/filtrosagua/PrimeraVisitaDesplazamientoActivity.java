package com.example.filtrosagua;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.RadioButton;
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

public class PrimeraVisitaDesplazamientoActivity extends AppCompatActivity {

    // UI (IDs exactamente como están en tu XML)
    private RadioGroup rgDesplaza;
    private RadioButton rbDesplazaSi, rbDesplazaNo;
    private TextInputEditText etMedioDesplaza, etTiempoMin;
    private MaterialButton btnAnterior, btnSiguiente;

    // Autosave keys
    private static final String K_NEC   = "pv_des_necesita";
    private static final String K_MEDIO = "pv_des_medio";
    private static final String K_MIN   = "pv_des_min";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita_desplazamiento);

        // Referencias
        rgDesplaza      = req(R.id.rgDesplaza);
        rbDesplazaSi    = req(R.id.rbDesplazaSi);
        rbDesplazaNo    = req(R.id.rbDesplazaNo);
        etMedioDesplaza = req(R.id.etMedioDesplaza);
        etTiempoMin     = req(R.id.etTiempoMin);

        btnAnterior     = req(R.id.btnAnterior);
        btnSiguiente    = req(R.id.btnSiguiente);

        // Rellenar autosave
        setRadioFromPrefs(rgDesplaza, Prefs.get(this, K_NEC), rbDesplazaSi, rbDesplazaNo);
        etMedioDesplaza.setText(Prefs.get(this, K_MEDIO));
        etTiempoMin.setText(Prefs.get(this, K_MIN));

        // Autosave textos
        auto(etMedioDesplaza, K_MEDIO);
        auto(etTiempoMin,     K_MIN);

        // Autosave radios
        rgDesplaza.setOnCheckedChangeListener((g, id) ->
                Prefs.put(this, K_NEC, radioSiNoFromId(id)));

        // Navegación: Anterior → Acceso al agua, Siguiente → Percepción del agua
        btnAnterior.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, PrimeraVisitaAccesoAguaActivity.class));
            finish();
        });
        btnSiguiente.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, PrimeraVisitaPercepcionAguaActivity.class));
            finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSectionNow();
    }

    /** Guarda esta sección con claves canónicas. */
    private void saveSectionNow() {
        try {
            Map<String, String> data = new LinkedHashMap<>();
            data.put("necesita_desplazarse", getRadioSiNo(rgDesplaza)); // "Si"/"No"
            data.put("medio_utiliza",        t(etMedioDesplaza));
            data.put("tiempo_min",           t(etTiempoMin));

            SessionCsvPrimera.saveSection(this, "desplazamiento", data);
        } catch (Exception e) {
            Toast.makeText(this, "Error guardando: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /* ================= Helpers ================= */

    private void auto(TextInputEditText et, String key) {
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                Prefs.put(PrimeraVisitaDesplazamientoActivity.this, key, s.toString());
            }
        });
    }

    private void setRadioFromPrefs(RadioGroup group, String val, RadioButton rbSi, RadioButton rbNo) {
        if ("Si".equalsIgnoreCase(val) || "Sí".equalsIgnoreCase(val)) rbSi.setChecked(true);
        else if ("No".equalsIgnoreCase(val)) rbNo.setChecked(true);
        else group.clearCheck();
    }

    /** "Si"/"No" desde grupo (por texto del RadioButton). */
    private String getRadioSiNo(RadioGroup g) {
        int id = g.getCheckedRadioButtonId();
        if (id == -1) return "";
        RadioButton rb = findViewById(id);
        if (rb == null || rb.getText() == null) return "";
        String tx = rb.getText().toString().trim().toLowerCase();
        if (tx.startsWith("si") || tx.startsWith("s\u00ed")) return "Si";
        return "No";
    }

    /** Para autosave: convertir id → "Si"/"No". */
    private String radioSiNoFromId(int id) {
        if (id <= 0) return "";
        RadioButton rb = findViewById(id);
        if (rb == null || rb.getText() == null) return "";
        String tx = rb.getText().toString().trim().toLowerCase();
        return (tx.startsWith("si") || tx.startsWith("s\u00ed")) ? "Si" : "No";
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
