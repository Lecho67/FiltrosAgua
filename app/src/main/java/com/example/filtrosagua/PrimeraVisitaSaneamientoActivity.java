package com.example.filtrosagua;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.filtrosagua.util.Prefs;
import com.example.filtrosagua.util.SessionCsvPrimera;
import com.google.android.material.button.MaterialButton;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class PrimeraVisitaSaneamientoActivity extends AppCompatActivity {

    private CheckBox cbTazaSi, cbTazaNo, cbDispSi, cbDispNo;

    private static final String K_TAZA = "pv10_taza";
    private static final String K_DISP = "pv10_disposicion";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita_saneamiento);

        cbTazaSi = req(R.id.cbTazaSi);
        cbTazaNo = req(R.id.cbTazaNo);
        cbDispSi = req(R.id.cbDisposicionSi);
        cbDispNo = req(R.id.cbDisposicionNo);

        MaterialButton btnAnt = req(R.id.btnAnteriorPv10);
        MaterialButton btnSig = req(R.id.btnSiguientePv10);

        // ----- Rellenar desde Prefs
        setChecksFromPref(K_TAZA, cbTazaSi, cbTazaNo);
        setChecksFromPref(K_DISP, cbDispSi, cbDispNo);

        // ----- Autosave + exclusión mutua
        cbTazaSi.setOnCheckedChangeListener((b, c) -> { if (c) cbTazaNo.setChecked(false); savePair(K_TAZA, cbTazaSi, cbTazaNo); });
        cbTazaNo.setOnCheckedChangeListener((b, c) -> { if (c) cbTazaSi.setChecked(false); savePair(K_TAZA, cbTazaSi, cbTazaNo); });

        cbDispSi.setOnCheckedChangeListener((b, c) -> { if (c) cbDispNo.setChecked(false); savePair(K_DISP, cbDispSi, cbDispNo); });
        cbDispNo.setOnCheckedChangeListener((b, c) -> { if (c) cbDispSi.setChecked(false); savePair(K_DISP, cbDispSi, cbDispNo); });

        // Navegación (solo guardamos sección; NO consolidamos aquí)
        btnAnt.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, PrimeraVisitaContaminacionActivity.class));
            finish();
        });
        btnSig.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, PrimeraVisitaHigieneActivity.class));
            finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Solo guardamos en staging + buffer (wide en memoria)
        saveSectionNow();
    }

    private void saveSectionNow() {
        try {
            Map<String, String> data = new LinkedHashMap<>();
            // sección = "saneamiento"
            data.put("taza",             pairValue(cbTazaSi, cbTazaNo));   // "Si"/"No"/""
            data.put("sistema_residuos", pairValue(cbDispSi, cbDispNo));   // "Si"/"No"/""
            SessionCsvPrimera.saveSection(this, "saneamiento", data);
        } catch (Exception e) {
            Toast.makeText(this, "Error guardando: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /* ---------- helpers ---------- */

    private void setChecksFromPref(String key, CheckBox si, CheckBox no) {
        String v = Prefs.get(this, key);
        si.setChecked("Si".equalsIgnoreCase(v));
        no.setChecked("No".equalsIgnoreCase(v));
    }

    private void savePair(String key, CheckBox si, CheckBox no) {
        String v = si.isChecked() ? "Si" : (no.isChecked() ? "No" : "");
        Prefs.put(this, key, v);
    }

    private String pairValue(CheckBox si, CheckBox no) {
        return si.isChecked() ? "Si" : (no.isChecked() ? "No" : "");
    }

    @SuppressWarnings("unchecked")
    private <T> T req(int id) {
        T v = (T) findViewById(id);
        return Objects.requireNonNull(v,
                "Falta la vista con id: " + getResources().getResourceEntryName(id));
    }
}
