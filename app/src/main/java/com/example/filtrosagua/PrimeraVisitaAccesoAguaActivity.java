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

public class PrimeraVisitaAccesoAguaActivity extends AppCompatActivity {

    private RadioGroup rgDispone, rgOtra;
    private RadioButton rbDispSi, rbDispNo, rbOtraSi, rbOtraNo;
    private TextInputEditText etFuente, etAdmin, etHoras;
    private MaterialButton btnAnterior, btnSiguiente;

    // Prefs keys (autosave)
    private static final String K_DISP   = "pv_acc_agua_dispone";
    private static final String K_FUENTE = "pv_acc_agua_fuente";
    private static final String K_OTRA   = "pv_acc_agua_otra";
    private static final String K_ADMIN  = "pv_acc_agua_admin";
    private static final String K_HORAS  = "pv_acc_agua_horas";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita_acceso_agua);

        rgDispone = req(R.id.rgDisponeAgua);
        rgOtra    = req(R.id.rgOtraFuente);
        rbDispSi  = req(R.id.rbDispSi);
        rbDispNo  = req(R.id.rbDispNo);
        rbOtraSi  = req(R.id.rbOtraSi);
        rbOtraNo  = req(R.id.rbOtraNo);

        etFuente  = req(R.id.etFuenteAgua);
        etAdmin   = req(R.id.etAdministrador);
        etHoras   = req(R.id.etHorasDia);

        btnAnterior  = req(R.id.btnAnterior);
        btnSiguiente = req(R.id.btnSiguiente);

        // Rellenar desde Prefs
        setRadioFromPrefs(rgDispone, Prefs.get(this, K_DISP), rbDispSi, rbDispNo);
        setRadioFromPrefs(rgOtra,    Prefs.get(this, K_OTRA), rbOtraSi, rbOtraNo);

        etFuente.setText(Prefs.get(this, K_FUENTE));
        etAdmin.setText(Prefs.get(this, K_ADMIN));
        etHoras.setText(Prefs.get(this, K_HORAS));

        // Autosave textos
        auto(etFuente, K_FUENTE);
        auto(etAdmin,  K_ADMIN);
        auto(etHoras,  K_HORAS);

        // Autosave radios
        rgDispone.setOnCheckedChangeListener((g, id) ->
                Prefs.put(this, K_DISP, radioSiNoFromId(id)));
        rgOtra.setOnCheckedChangeListener((g, id) ->
                Prefs.put(this, K_OTRA, radioSiNoFromId(id)));

        // Navegación (ajusta si tu flujo es distinto)
        btnAnterior.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, PrimeraVisitaDemografiaActivity.class));
            finish();
        });
        btnSiguiente.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, PrimeraVisitaDesplazamientoActivity.class));
            finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSectionNow();
    }

    private void saveSectionNow() {
        try {
            Map<String, String> data = new LinkedHashMap<>();

            // ⚠️ Claves canónicas que espera el unificador
            data.put("acceso_agua.tiene_agua",        getRadioSiNo(rgDispone));  // "Si" / "No"
            data.put("acceso_agua.fuente_respuesta",  t(etFuente));              // texto libre
            data.put("acceso_agua.usa_otra_fuente",   getRadioSiNo(rgOtra));     // "Si" / "No" (opcional)
            data.put("acceso_agua.administra_servicio", t(etAdmin));             // texto
            data.put("acceso_agua.horas_dia",         t(etHoras));               // "8"...

            // Sección: "acceso_agua" (el campo va con el prefijo como acordamos)
            SessionCsvPrimera.saveSection(this, "acceso_agua", data);
        } catch (Exception e) {
            Toast.makeText(this, "Error guardando: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /* ==== Helpers ==== */

    private void auto(TextInputEditText et, String key) {
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                Prefs.put(PrimeraVisitaAccesoAguaActivity.this, key, s.toString());
            }
        });
    }

    private void setRadioFromPrefs(RadioGroup group, String val, RadioButton rbSi, RadioButton rbNo) {
        if ("Si".equalsIgnoreCase(val) || "Sí".equalsIgnoreCase(val)) rbSi.setChecked(true);
        else if ("No".equalsIgnoreCase(val)) rbNo.setChecked(true);
        else group.clearCheck();
    }

    /** Devuelve "Si"/"No" según el texto del RadioButton seleccionado. */
    private String getRadioSiNo(RadioGroup g) {
        int id = g.getCheckedRadioButtonId();
        if (id == -1) return "";
        RadioButton rb = findViewById(id);
        if (rb == null || rb.getText() == null) return "";
        String tx = rb.getText().toString().trim().toLowerCase();
        // Acepta "si", "sí"
        if (tx.startsWith("si") || tx.startsWith("s\u00ed")) return "Si";
        return "No";
    }

    /** Igual que arriba, pero solo con el id (para autosave rápido). */
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
