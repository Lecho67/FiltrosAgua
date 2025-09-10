package com.example.filtrosagua;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.filtrosagua.util.Prefs;
import com.example.filtrosagua.util.SessionCsvPrimera;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class PrimeraVisitaUbicacionActivity extends AppCompatActivity {

    private MaterialAutoCompleteTextView acDepartamento, acMunicipio, acVereda;
    private MaterialButton btnAnterior, btnSiguiente;

    private static final String K_DEP = "pv_ubi_departamento";
    private static final String K_MUN = "pv_ubi_municipio";
    private static final String K_VER = "pv_ubi_vereda";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita_ubicacion);

        acDepartamento = findViewById(R.id.acDepartamento);
        acMunicipio    = findViewById(R.id.acMunicipio);
        acVereda       = findViewById(R.id.acVereda);
        btnAnterior    = findViewById(R.id.btnPvAnterior);
        btnSiguiente   = findViewById(R.id.btnPvSiguiente);

        // 1) Departamento: por defecto "Valle del Cauca"
        String depSaved = Prefs.get(this, K_DEP);
        if (depSaved.isEmpty()) depSaved = "Valle del Cauca";
        acDepartamento.setText(depSaved, false);

        // Cargar municipios para Valle del Cauca (siempre usa municipios_valle)
        loadMunicipiosValle();

        // 2) Restaurar municipio/vereda si existen
        String munSaved = Prefs.get(this, K_MUN);
        String verSaved = Prefs.get(this, K_VER);

        if (!munSaved.isEmpty()) {
            acMunicipio.setText(munSaved, false);
            loadCorregimientosFor(munSaved);
        }
        if (!verSaved.isEmpty()) {
            acVereda.setText(verSaved, false);
        }

        // 3) Autosave
        addAutosave(acDepartamento, K_DEP);
        addAutosave(acMunicipio,    K_MUN);
        addAutosave(acVereda,       K_VER);

        // 4) Encadenado
        acDepartamento.setOnItemClickListener((p, v, pos, id) -> {
            Prefs.put(this, K_DEP, acDepartamento.getText().toString());
            acMunicipio.setText("", false);
            acVereda.setText("", false);
            Prefs.put(this, K_MUN, "");
            Prefs.put(this, K_VER, "");
            loadMunicipiosValle(); // siempre usa municipios_valle
        });

        acMunicipio.setOnItemClickListener((p, v, pos, id) -> {
            String municipio = acMunicipio.getText().toString();
            Prefs.put(this, K_MUN, municipio);
            acVereda.setText("", false);
            Prefs.put(this, K_VER, "");
            loadCorregimientosFor(municipio);
        });

        // 5) Navegación
        btnAnterior.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, PrimeraVisitaBeneficiarioActivity.class));
            finish();
        });

        btnSiguiente.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, PrimeraVisitaDemografiaActivity.class));
            finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSectionNow();
    }

    /* ================== Helpers ================== */

    private void addAutosave(MaterialAutoCompleteTextView v, String key) {
        v.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                Prefs.put(PrimeraVisitaUbicacionActivity.this, key, s.toString());
            }
        });
    }

    private void setAdapterFromArray(MaterialAutoCompleteTextView view, int arrayResId) {
        String[] items = (arrayResId == 0) ? new String[]{} : getResources().getStringArray(arrayResId);
        view.setAdapter(new ArrayAdapter<>(this, R.layout.list_item_dropdown, items));
    }

    /** MUNICIPIOS: usa SIEMPRE R.array.municipios_valle */
    private void loadMunicipiosValle() {
        int resId = getResources().getIdentifier("municipios_valle", "array", getPackageName());
        setAdapterFromArray(acMunicipio, resId);
        if (resId == 0) {
            Toast.makeText(this, "No se encontró el array municipios_valle", Toast.LENGTH_LONG).show();
        }
    }

    /** CORREGIMIENTOS: busca R.array.corregimientos_<slug_municipio> */
    private void loadCorregimientosFor(String municipio) {
        String slug = slug(municipio); // p. ej. "cali", "la_buitrera_palmira"
        int resId = getResources().getIdentifier("corregimientos_" + slug, "array", getPackageName());
        setAdapterFromArray(acVereda, resId);
        if (resId == 0) {
            // Si no hay array, dejamos vacío el listado (no es error grave)
            acVereda.setText("", false);
            Toast.makeText(this, "Sin corregimientos para: " + municipio, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveSectionNow() {
        try {
            Map<String, String> data = new LinkedHashMap<>();
            data.put("departamento",         text(acDepartamento));
            data.put("municipio",            text(acMunicipio));
            data.put("vereda_corregimiento", text(acVereda));
            SessionCsvPrimera.saveSection(this, "ubicacion", data);
        } catch (Exception ignored) {}
    }

    private String text(MaterialAutoCompleteTextView v) {
        return v.getText() == null ? "" : v.getText().toString().trim();
    }

    /** slug: minúsculas, sin acentos, paréntesis y símbolos; espacios -> '_' */
    private String slug(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        n = n.toLowerCase(Locale.ROOT).trim();
        n = n.replaceAll("[()]", "");               // quita paréntesis
        n = n.replaceAll("[^a-z0-9\\s_\\-]", "");   // deja letras/números/espacios/guiones
        n = n.replaceAll("\\s+", "_");
        return n;
    }
}
