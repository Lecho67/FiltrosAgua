package com.example.filtrosagua;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.filtrosagua.util.Prefs;
import com.example.filtrosagua.util.SessionCsvPrimera;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PrimeraVisitaAlmacenamientoActivity extends AppCompatActivity {

    private RadioGroup rgTanque;
    private CheckBox cbHervir, cbFiltrar, cbClorar, cbOtro, cbNinguno;
    private TextInputEditText etHervirEmplea, etQuienLabores, etGastoMensual;

    // Prefs keys
    private static final String K_TANQUE   = "pv8_tanque";
    private static final String K_TRATS    = "pv8_tratamientos";
    private static final String K_HERVIR   = "pv8_hervir_emplea";
    private static final String K_QUIEN    = "pv8_quien_labores";
    private static final String K_GASTO    = "pv8_gasto_mensual";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita_almacenamiento);

        rgTanque       = req(R.id.rgTanquePv);
        cbHervir       = req(R.id.cbHervirPv);
        cbFiltrar      = req(R.id.cbFiltrarPv);
        cbClorar       = req(R.id.cbClorarPv);
        cbOtro         = req(R.id.cbOtroPv);
        cbNinguno      = req(R.id.cbNingunoPv);
        etHervirEmplea = req(R.id.etHervirEmpleaPv);
        etQuienLabores = req(R.id.etQuienLaboresPv);
        etGastoMensual = req(R.id.etGastoMensualPv);

        MaterialButton btnAnt = req(R.id.btnAnteriorPv8);
        MaterialButton btnSig = req(R.id.btnSiguientePv8);

        // ---- Rellenar desde Prefs
        setRadioFromPrefs();
        setChecksFromPrefs();
        etHervirEmplea.setText(Prefs.get(this, K_HERVIR));
        etQuienLabores.setText(Prefs.get(this, K_QUIEN));
        etGastoMensual.setText(Prefs.get(this, K_GASTO));

        // ---- Autosave
        rgTanque.setOnCheckedChangeListener((g, id) ->
                Prefs.put(this, K_TANQUE, id == R.id.rbTanqueSiPv ? "Si" : "No"));

        // “Ninguno” exclusivo
        cbNinguno.setOnCheckedChangeListener((v, checked) -> {
            if (checked) {
                cbHervir.setChecked(false);
                cbFiltrar.setChecked(false);
                cbClorar.setChecked(false);
                cbOtro.setChecked(false);
            }
            saveTratamientosPref();
        });
        cbHervir.setOnCheckedChangeListener((v, c) -> { if (c) cbNinguno.setChecked(false); saveTratamientosPref(); });
        cbFiltrar.setOnCheckedChangeListener((v, c) -> { if (c) cbNinguno.setChecked(false); saveTratamientosPref(); });
        cbClorar.setOnCheckedChangeListener((v, c) -> { if (c) cbNinguno.setChecked(false); saveTratamientosPref(); });
        cbOtro.setOnCheckedChangeListener((v, c) -> { if (c) cbNinguno.setChecked(false); saveTratamientosPref(); });

        auto(etHervirEmplea, K_HERVIR);
        auto(etQuienLabores, K_QUIEN);
        auto(etGastoMensual, K_GASTO);

        // ---- Navegación
        btnAnt.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, PrimeraVisitaPercepcionAguaActivity.class));
            finish();
        });
        btnSig.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, PrimeraVisitaContaminacionActivity.class));
            finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSectionNow();
    }

    /** Guarda con claves SIN prefijo; la sección se pasa aparte. */
    private void saveSectionNow() {
        try {
            Map<String, String> data = new LinkedHashMap<>();
            // claves canónicas que espera el unificador:
            data.put("tanque",        getRadioSiNo(rgTanque, R.id.rbTanqueSiPv, R.id.rbTanqueNoPv));
            data.put("tratamientos",  getTratamientos());
            data.put("hierve_como",   t(etHervirEmplea));   // antes "hervir_emplea" -> unificador usa hierve_como
            data.put("quien_labores", t(etQuienLabores));
            data.put("gasto_mensual", t(etGastoMensual));

            SessionCsvPrimera.saveSection(this, "almacenamiento_tratamiento", data);
        } catch (Exception e) {
            Toast.makeText(this, "Error guardando: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // -------- helpers --------
    private void setRadioFromPrefs() {
        String v = Prefs.get(this, K_TANQUE);
        if ("Si".equalsIgnoreCase(v)) rgTanque.check(R.id.rbTanqueSiPv);
        else if ("No".equalsIgnoreCase(v)) rgTanque.check(R.id.rbTanqueNoPv);
        else rgTanque.clearCheck();
    }

    private void setChecksFromPrefs() {
        String csv = Prefs.get(this, K_TRATS);
        if (csv == null) csv = "";
        cbHervir.setChecked(csv.contains("Hervir"));
        cbFiltrar.setChecked(csv.contains("Filtrar"));
        cbClorar.setChecked(csv.contains("Clorar"));
        cbOtro.setChecked(csv.contains("Otro"));
        cbNinguno.setChecked(csv.contains("Ninguno"));
    }

    private void saveTratamientosPref() {
        Prefs.put(this, K_TRATS, getTratamientos());
    }

    private String getTratamientos() {
        if (cbNinguno.isChecked()) return "Ninguno";
        List<String> l = new ArrayList<>();
        if (cbHervir.isChecked())  l.add("Hervir");
        if (cbFiltrar.isChecked()) l.add("Filtrar");
        if (cbClorar.isChecked())  l.add("Clorar");
        if (cbOtro.isChecked())    l.add("Otro");
        return String.join(",", l);
    }

    private void auto(TextInputEditText et, String key) {
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                Prefs.put(PrimeraVisitaAlmacenamientoActivity.this, key, s.toString());
            }
        });
    }

    private String getRadioSiNo(RadioGroup rg, int idSi, int idNo) {
        int id = rg.getCheckedRadioButtonId();
        if (id == -1) return "";
        return id == idSi ? "Si" : "No";
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
