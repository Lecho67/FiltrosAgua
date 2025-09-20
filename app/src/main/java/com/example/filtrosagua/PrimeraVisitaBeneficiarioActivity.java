package com.example.filtrosagua;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.filtrosagua.util.Prefs;
import com.example.filtrosagua.util.SessionCsvPrimera;
import com.google.android.material.button.MaterialButton;

import java.util.LinkedHashMap;
import java.util.Map;

public class PrimeraVisitaBeneficiarioActivity extends AppCompatActivity {

    private EditText etTipoBeneficiario, etGrupoPoblacional, etNombreBeneficiario,
            etCedulaBeneficiario, etTelefonoBeneficiario;
    private MaterialButton btnAnterior, btnSiguiente;

    private static final String K_TIPO   = "pv_ben_tipo";
    private static final String K_GRUPO  = "pv_ben_grupo";
    private static final String K_NOMBRE = "pv_ben_nombre";
    private static final String K_CED    = "pv_ben_cedula";
    private static final String K_TEL    = "pv_ben_telefono";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita_beneficiario);

        // Bind (acepta ids alternativos por si difieren en tu layout)
        etTipoBeneficiario     = bindEt("etTipoBeneficiario", "etTipo", "etTipoBen");
        etGrupoPoblacional     = bindEt("etGrupoPoblacional", "etGrupo", "etGrupoBen");
        etNombreBeneficiario   = bindEt("etNombreBeneficiario", "etNombre", "etNombreBen");
        etCedulaBeneficiario   = bindEt("etCedulaBeneficiario", "etCedula", "etCedulaBen");
        etTelefonoBeneficiario = bindEt("etTelefonoBeneficiario", "etTelefono", "etTelefonoBen");

        btnAnterior  = bindBtn("btnPvAnterior", "btnAnterior", "btnAnteriorSeg");
        btnSiguiente = bindBtn("btnPvSiguiente", "btnSiguiente", "btnSiguienteSeg");

        // Prefill desde Prefs
        setIfNotNull(etTipoBeneficiario,     Prefs.get(this, K_TIPO));
        setIfNotNull(etGrupoPoblacional,     Prefs.get(this, K_GRUPO));
        setIfNotNull(etNombreBeneficiario,   Prefs.get(this, K_NOMBRE));
        setIfNotNull(etCedulaBeneficiario,   Prefs.get(this, K_CED));
        setIfNotNull(etTelefonoBeneficiario, Prefs.get(this, K_TEL));

        // Autosave
        autosave(etTipoBeneficiario,     K_TIPO);
        autosave(etGrupoPoblacional,     K_GRUPO);
        autosave(etNombreBeneficiario,   K_NOMBRE);
        autosave(etCedulaBeneficiario,   K_CED);
        autosave(etTelefonoBeneficiario, K_TEL);

        // Navegación (solo guarda sección; NO consolidar aquí)
        if (btnAnterior != null) {
            btnAnterior.setOnClickListener(v -> {
                saveSectionNow();
                startActivity(new Intent(this, PrimeraVisitaActivity.class));
                finish();
            });
        }
        if (btnSiguiente != null) {
            btnSiguiente.setOnClickListener(v -> {
                try {
                    saveSectionNow();
                    startActivity(new Intent(this, PrimeraVisitaUbicacionActivity.class));
                    finish();
                } catch (Exception e) {
                    Toast.makeText(this, "Error guardando: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSectionNow();
    }

    /** Guarda beneficiario.* y también info_basica.cedula para poblar la columna base. */
    private void saveSectionNow() {
        try {
            // beneficiario.*
            Map<String, String> ben = new LinkedHashMap<>();
            ben.put("tipo_beneficiario",   getTextOrEmpty(etTipoBeneficiario));
            ben.put("grupo_poblacional",   getTextOrEmpty(etGrupoPoblacional));
            ben.put("nombre_beneficiario", getTextOrEmpty(etNombreBeneficiario));
            ben.put("cedula",              getTextOrEmpty(etCedulaBeneficiario));
            ben.put("telefono",            getTextOrEmpty(etTelefonoBeneficiario));
            SessionCsvPrimera.saveSection(this, "beneficiario", ben);

            // info_basica.cedula  (columna prioritaria del wide)
            String ced = getTextOrEmpty(etCedulaBeneficiario);
            if (!ced.isEmpty()) {
                Map<String, String> info = new LinkedHashMap<>();
                info.put("cedula", ced);
                SessionCsvPrimera.saveSection(this, "info_basica", info);
            }
        } catch (Exception ignored) {}
    }

    // ===== Helpers =====
    private EditText bindEt(String... ids) {
        for (String id : ids) {
            int res = getId(id);
            if (res != 0) {
                EditText et = findViewById(res);
                if (et != null) return et;
            }
        }
        return null;
    }

    private MaterialButton bindBtn(String... ids) {
        for (String id : ids) {
            int res = getId(id);
            if (res != 0) {
                MaterialButton b = findViewById(res);
                if (b != null) return b;
            }
        }
        return null;
    }

    private int getId(String name) {
        return getResources().getIdentifier(name, "id", getPackageName());
    }

    private void setIfNotNull(EditText et, String val) {
        if (et != null) et.setText(val == null ? "" : val);
    }

    private void autosave(EditText et, String key) {
        if (et == null) return;
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                Prefs.put(PrimeraVisitaBeneficiarioActivity.this, key, s.toString());
            }
        });
    }

    private String getTextOrEmpty(EditText et) {
        return (et == null || et.getText() == null) ? "" : et.getText().toString().trim();
    }
}
