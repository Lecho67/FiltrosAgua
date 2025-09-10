package com.example.filtrosagua;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.filtrosagua.util.Prefs;
import com.example.filtrosagua.util.SessionCsvPrimera;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class PrimeraVisitaDemografiaActivity extends AppCompatActivity {

    private TextInputEditText etMenor5, et6a17, et18a64, et65Mas;
    private MaterialButton btnAnterior, btnSiguiente;

    // autosave keys
    private static final String K_MENOR5 = "pv_demo_menor5";
    private static final String K_6A17   = "pv_demo_6a17";
    private static final String K_18A64  = "pv_demo_18a64";
    private static final String K_65MAS  = "pv_demo_65mas";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita_demografia);

        etMenor5   = req(R.id.etMenor5);
        et6a17     = req(R.id.et6a17);
        et18a64    = req(R.id.et18a64);
        et65Mas    = req(R.id.et65Mas);
        btnAnterior = req(R.id.btnPvAnterior);
        btnSiguiente= req(R.id.btnPvSiguiente);

        // Rellenar desde Prefs
        etMenor5.setText(Prefs.get(this, K_MENOR5));
        et6a17.setText(Prefs.get(this, K_6A17));
        et18a64.setText(Prefs.get(this, K_18A64));
        et65Mas.setText(Prefs.get(this, K_65MAS));

        // Autosave
        auto(etMenor5, K_MENOR5);
        auto(et6a17,   K_6A17);
        auto(et18a64,  K_18A64);
        auto(et65Mas,  K_65MAS);

        // Navegación
        btnAnterior.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, PrimeraVisitaUbicacionActivity.class));
            finish();
        });

        btnSiguiente.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, PrimeraVisitaAccesoAguaActivity.class));
            finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSectionNow();
    }

    private void auto(TextInputEditText et, String key) {
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { Prefs.put(PrimeraVisitaDemografiaActivity.this, key, s.toString()); }
        });
    }

    private void saveSectionNow() {
        try {
            Map<String, String> data = new LinkedHashMap<>();
            data.put("menor_5",  t(etMenor5));
            data.put("entre_6_17", t(et6a17));
            data.put("entre_18_64", t(et18a64));
            data.put("mayor_65", t(et65Mas));

            SessionCsvPrimera.saveSection(this, "demografia", data);
        } catch (Exception e) {
            Toast.makeText(this, "Error guardando: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String t(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    @SuppressWarnings("unchecked")
    private <T> T req(int id) {
        T v = (T) findViewById(id);
        return Objects.requireNonNull(v, "Falta la vista con id: " + getResources().getResourceEntryName(id));
    }
}
