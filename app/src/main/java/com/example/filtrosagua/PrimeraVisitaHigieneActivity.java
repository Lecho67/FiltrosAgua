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

public class PrimeraVisitaHigieneActivity extends AppCompatActivity {

    private CheckBox cbCapSi, cbCapNo,
            cbLavadoManos, cbLimpiezaHogar, cbCepillado, cbOtro, cbBanoDiario;

    private static final String K_CAP   = "pv11_capacitacion";
    private static final String K_LM    = "pv11_lavado_manos";
    private static final String K_LH    = "pv11_limpieza_hogar";
    private static final String K_CEP   = "pv11_cepillado_dientes";
    private static final String K_OTRO  = "pv11_otro";
    private static final String K_BANO  = "pv11_bano_diario";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita_higiene);

        cbCapSi         = req(R.id.cbCapSi);
        cbCapNo         = req(R.id.cbCapNo);
        cbLavadoManos   = req(R.id.cbLavadoManos);
        cbLimpiezaHogar = req(R.id.cbLimpiezaHogar);
        cbCepillado     = req(R.id.cbCepillado);
        cbOtro          = req(R.id.cbOtro);
        cbBanoDiario    = req(R.id.cbBanoDiario);

        MaterialButton btnAnt = req(R.id.btnAnteriorPv11);
        MaterialButton btnSig = req(R.id.btnSiguientePv11);

        // Prefill desde Prefs
        setPairFromPref(K_CAP, cbCapSi, cbCapNo);
        cbLavadoManos.setChecked(boolPref(K_LM));
        cbLimpiezaHogar.setChecked(boolPref(K_LH));
        cbCepillado.setChecked(boolPref(K_CEP));
        cbOtro.setChecked(boolPref(K_OTRO));
        cbBanoDiario.setChecked(boolPref(K_BANO));

        // Autosave + exclusión mutua
        cbCapSi.setOnCheckedChangeListener((b, c) -> { if (c) cbCapNo.setChecked(false); savePair(K_CAP, cbCapSi, cbCapNo); });
        cbCapNo.setOnCheckedChangeListener((b, c) -> { if (c) cbCapSi.setChecked(false); savePair(K_CAP, cbCapSi, cbCapNo); });

        setAuto(cbLavadoManos,   K_LM);
        setAuto(cbLimpiezaHogar, K_LH);
        setAuto(cbCepillado,     K_CEP);
        setAuto(cbOtro,          K_OTRO);
        setAuto(cbBanoDiario,    K_BANO);

        // Navegación (solo guardar sección; NO consolidar aquí)
        btnAnt.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, PrimeraVisitaSaneamientoActivity.class));
            finish();
        });
        btnSig.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, PrimeraVisitaSaludActivity.class));
            finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Guardar en staging + buffer (sin consolidar)
        saveSectionNow();
    }

    private void saveSectionNow() {
        try {
            Map<String, String> data = new LinkedHashMap<>();
            // Claves SIN prefijo; saveSection("higiene", ...) añadirá "higiene."
            data.put("capacitacion_higiene",        pairValue(cbCapSi, cbCapNo)); // "Si"/"No"/""
            data.put("practica_lavado_manos",       yn(cbLavadoManos));           // "Si"/"No"
            data.put("practica_limpieza_hogar",     yn(cbLimpiezaHogar));
            data.put("practica_cepillado_dientes",  yn(cbCepillado));
            data.put("practica_otro",               yn(cbOtro));
            data.put("practica_bano_diario",        yn(cbBanoDiario));

            SessionCsvPrimera.saveSection(this, "higiene", data);
        } catch (Exception e) {
            Toast.makeText(this, "Error guardando: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /* --------- helpers --------- */

    private boolean boolPref(String key) {
        return "true".equalsIgnoreCase(Prefs.get(this, key));
    }

    private void setAuto(CheckBox cb, String key) {
        cb.setOnCheckedChangeListener((b, c) -> Prefs.put(this, key, c ? "true" : "false"));
    }

    private void setPairFromPref(String key, CheckBox si, CheckBox no) {
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

    private static String yn(CheckBox cb){ return cb.isChecked() ? "Si" : "No"; }

    @SuppressWarnings("unchecked")
    private <T> T req(int id) {
        T v = (T) findViewById(id);
        return Objects.requireNonNull(v,
                "Falta la vista con id: " + getResources().getResourceEntryName(id));
    }
}
