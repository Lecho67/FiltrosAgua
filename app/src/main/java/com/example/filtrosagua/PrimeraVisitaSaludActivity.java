package com.example.filtrosagua;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.filtrosagua.util.Prefs;
import com.example.filtrosagua.util.SessionCsvPrimera;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PrimeraVisitaSaludActivity extends AppCompatActivity {

    // Dolor estómago (exclusivos)
    private CheckBox cbDolorSi, cbDolorNo;

    // Enfermedades (múltiple) + Ninguna (excluyente)
    private CheckBox cbEnfDiarrea, cbEnfVomito, cbEnfColera, cbEnfHepatitis,
            cbEnfParasitosis, cbEnfOtro, cbEnfNinguna;

    private EditText etObservaciones;

    // ---- Claves de autosave (Prefs)
    private static final String K_DOLOR      = "pv12_dolor_estomago";
    private static final String K_DIARREA    = "pv12_enf_diarrea";
    private static final String K_VOMITO     = "pv12_enf_vomito";
    private static final String K_COLERA     = "pv12_enf_colera";
    private static final String K_HEPATITIS  = "pv12_enf_hepatitis";
    private static final String K_PARASITOS  = "pv12_enf_parasitosis";
    private static final String K_OTRO       = "pv12_enf_otro";
    private static final String K_NINGUNA    = "pv12_enf_ninguna";
    private static final String K_OBS        = "pv12_observaciones";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita_salud);

        // Referencias
        cbDolorSi       = req(R.id.cbDolorSi);
        cbDolorNo       = req(R.id.cbDolorNo);

        cbEnfDiarrea    = req(R.id.cbEnfDiarrea);
        cbEnfVomito     = req(R.id.cbEnfVomito);
        cbEnfColera     = req(R.id.cbEnfColera);
        cbEnfHepatitis  = req(R.id.cbEnfHepatitis);
        cbEnfParasitosis= req(R.id.cbEnfParasitosis);
        cbEnfOtro       = req(R.id.cbEnfOtro);
        cbEnfNinguna    = req(R.id.cbEnfNinguna);

        etObservaciones = req(R.id.etObservaciones);

        MaterialButton btnAnterior = req(R.id.btnAnteriorSalud);
        MaterialButton btnEnviar   = req(R.id.btnEnviarEncuesta);

        // ---- Relleno desde Prefs
        setPairFromPref(K_DOLOR, cbDolorSi, cbDolorNo);
        cbEnfDiarrea.setChecked(boolPref(K_DIARREA));
        cbEnfVomito.setChecked(boolPref(K_VOMITO));
        cbEnfColera.setChecked(boolPref(K_COLERA));
        cbEnfHepatitis.setChecked(boolPref(K_HEPATITIS));
        cbEnfParasitosis.setChecked(boolPref(K_PARASITOS));
        cbEnfOtro.setChecked(boolPref(K_OTRO));
        cbEnfNinguna.setChecked(boolPref(K_NINGUNA));
        etObservaciones.setText(Prefs.get(this, K_OBS));

        // ---- Reglas + autosave
        // Sí/No exclusivos (Dolor)
        cbDolorSi.setOnCheckedChangeListener((b, c) -> { if (c) cbDolorNo.setChecked(false); savePair(K_DOLOR, cbDolorSi, cbDolorNo); });
        cbDolorNo.setOnCheckedChangeListener((b, c) -> { if (c) cbDolorSi.setChecked(false); savePair(K_DOLOR, cbDolorSi, cbDolorNo); });

        // 'Ninguna' desmarca resto
        cbEnfNinguna.setOnCheckedChangeListener((b, checked) -> {
            if (checked) {
                cbEnfDiarrea.setChecked(false);
                cbEnfVomito.setChecked(false);
                cbEnfColera.setChecked(false);
                cbEnfHepatitis.setChecked(false);
                cbEnfParasitosis.setChecked(false);
                cbEnfOtro.setChecked(false);
            }
            Prefs.put(this, K_NINGUNA, checked ? "true" : "false");
        });
        // Si marca cualquiera, desmarcar 'Ninguna'
        setUncheckAndAuto(cbEnfDiarrea,    K_DIARREA,   cbEnfNinguna);
        setUncheckAndAuto(cbEnfVomito,     K_VOMITO,    cbEnfNinguna);
        setUncheckAndAuto(cbEnfColera,     K_COLERA,    cbEnfNinguna);
        setUncheckAndAuto(cbEnfHepatitis,  K_HEPATITIS, cbEnfNinguna);
        setUncheckAndAuto(cbEnfParasitosis,K_PARASITOS, cbEnfNinguna);
        setUncheckAndAuto(cbEnfOtro,       K_OTRO,      cbEnfNinguna);

        etObservaciones.addTextChangedListener(new SimpleTextWatcher(s -> Prefs.put(this, K_OBS, s)));

        // ---- Navegación
        btnAnterior.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, PrimeraVisitaHigieneActivity.class));
            finish();
        });

        // ENVIAR = guardar sección + consolidar maestro + limpiar + volver a inicio
        btnEnviar.setOnClickListener(v -> {
            try {
                saveSectionNow();                         // 1) guarda "salud" en primeravisita.csv
                SessionCsvPrimera.commitToMasterWide(this); // WIDE: 1 fila por encuesta
                // 2) consolida a primeravisita_master.csv
                File m = SessionCsvPrimera.fMaster(this);
                if (m != null) {
                    Toast.makeText(this, "Guardada en: " + m.getAbsolutePath(), Toast.LENGTH_LONG).show();
                }
                SessionCsvPrimera.clearSession(this);     // 3) limpia staging
                Prefs.clearAll(this);                     //    limpia autosave

                // 4) regreso a inicio
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

    // ---- Persistencia de la sección
    private void saveSectionNow() {
        try {
            Map<String, String> data = new LinkedHashMap<>();

            // Normalizadas (lo que pediste)
            data.put("salud.dolor_estomago", pairValue(cbDolorSi, cbDolorNo));
            data.put("salud.enfermedades", enfermedadesCsv()); // <- agregado clave agregada
            data.put("salud.observaciones", s(etObservaciones));

            // Compat (claves detalladas que ya tenías)
            data.put("dolor_estomago",        pairValue(cbDolorSi, cbDolorNo));
            data.put("enf_diarrea",           yn(cbEnfDiarrea));
            data.put("enf_vomito",            yn(cbEnfVomito));
            data.put("enf_colera",            yn(cbEnfColera));
            data.put("enf_hepatitis",         yn(cbEnfHepatitis));
            data.put("enf_parasitosis",       yn(cbEnfParasitosis));
            data.put("enf_otro",              yn(cbEnfOtro));
            data.put("enf_ninguna",           yn(cbEnfNinguna));
            data.put("observaciones",         s(etObservaciones));

            SessionCsvPrimera.saveSection(this, "salud", data);
        } catch (Exception ignored) {}
    }

    /** Construye CSV de enfermedades seleccionadas (o "Ninguna") */
    private String enfermedadesCsv() {
        if (cbEnfNinguna.isChecked()) return "Ninguna";
        List<String> l = new ArrayList<>();
        if (cbEnfDiarrea.isChecked())     l.add("Diarrea");
        if (cbEnfVomito.isChecked())      l.add("Vómito");
        if (cbEnfColera.isChecked())      l.add("Cólera");
        if (cbEnfHepatitis.isChecked())   l.add("Hepatitis");
        if (cbEnfParasitosis.isChecked()) l.add("Parasitosis");
        if (cbEnfOtro.isChecked())        l.add("Otro");
        return String.join(",", l);
    }

    /* ================= helpers ================= */

    private boolean boolPref(String key) { return "true".equalsIgnoreCase(Prefs.get(this, key)); }

    private void setUncheckAndAuto(CheckBox src, String key, CheckBox toUncheck) {
        src.setOnCheckedChangeListener((b, c) -> {
            if (c) toUncheck.setChecked(false);
            Prefs.put(this, key, c ? "true" : "false");
        });
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

    private static String yn(CheckBox cb) { return cb.isChecked() ? "Si" : "No"; }

    private static String pairValue(CheckBox si, CheckBox no) {
        return si.isChecked() ? "Si" : (no.isChecked() ? "No" : "");
    }

    private static String s(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    @SuppressWarnings("unchecked")
    private <T> T req(int id) {
        T v = (T) findViewById(id);
        return Objects.requireNonNull(v,
                "Falta la vista con id: " + getResources().getResourceEntryName(id));
    }

    // TextWatcher corto para autosave de EditText
    private static class SimpleTextWatcher implements android.text.TextWatcher {
        interface F { void on(String s); }
        private final F f;
        SimpleTextWatcher(F f){ this.f = f; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(android.text.Editable s) { f.on(s.toString()); }
    }
}
