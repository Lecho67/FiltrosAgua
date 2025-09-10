package com.example.filtrosagua;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.filtrosagua.util.Prefs;
import com.example.filtrosagua.util.SessionCsvPrimera;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PrimeraVisitaContaminacionActivity extends AppCompatActivity {

    // Contaminación
    private CheckBox cbAnimales, cbAguasRes, cbBasuras, cbQuimicos, cbOtros, cbNinguno;
    // Protegida e Importante
    private RadioGroup rgProtegida, rgImportante;
    // Beneficios
    private CheckBox cbMejorSalud, cbMejorSabor, cbMenosEnf, cbAhorroEcon, cbBenefOtro;

    // Prefs keys
    private static final String K_CONT = "pv9_contaminacion";      // CSV
    private static final String K_PROT = "pv9_fuente_protegida";   // Si/No
    private static final String K_IMP  = "pv9_importante";         // Si/No
    private static final String K_BEN  = "pv9_beneficios";         // CSV

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita_contaminacion);

        // Bind
        cbAnimales   = req(R.id.cbAnimales);
        cbAguasRes   = req(R.id.cbAguasRes);
        cbBasuras    = req(R.id.cbBasuras);
        cbQuimicos   = req(R.id.cbQuimicos);
        cbOtros      = req(R.id.cbOtros);
        cbNinguno    = req(R.id.cbNingunoCont);

        rgProtegida  = req(R.id.rgProtegida);
        rgImportante = req(R.id.rgImportante);

        cbMejorSalud = req(R.id.cbMejorSalud);
        cbMejorSabor = req(R.id.cbMejorSabor);
        cbMenosEnf   = req(R.id.cbMenosEnf);
        cbAhorroEcon = req(R.id.cbAhorroEcon);
        cbBenefOtro  = req(R.id.cbBenefOtro);

        MaterialButton btnAnt = req(R.id.btnAnteriorPv9);
        MaterialButton btnSig = req(R.id.btnSiguientePv9);

        // ---- Rellenar prefs
        setChecksFromCsv(Prefs.get(this, K_CONT), true);
        setRadioFromPref(rgProtegida, K_PROT, R.id.rbProtSi, R.id.rbProtNo);
        setRadioFromPref(rgImportante, K_IMP, R.id.rbImpSi, R.id.rbImpNo);
        setChecksFromCsv(Prefs.get(this, K_BEN), false);

        // ---- Autosave
        // Exclusión de "Ninguno" para contaminación:
        cbNinguno.setOnCheckedChangeListener((v, ch) -> {
            if (ch) {
                cbAnimales.setChecked(false);
                cbAguasRes.setChecked(false);
                cbBasuras.setChecked(false);
                cbQuimicos.setChecked(false);
                cbOtros.setChecked(false);
            }
            saveContaminacionPref();
        });
        cbAnimales.setOnCheckedChangeListener((v, c) -> { if (c) cbNinguno.setChecked(false); saveContaminacionPref(); });
        cbAguasRes.setOnCheckedChangeListener((v, c) -> { if (c) cbNinguno.setChecked(false); saveContaminacionPref(); });
        cbBasuras.setOnCheckedChangeListener((v, c)   -> { if (c) cbNinguno.setChecked(false); saveContaminacionPref(); });
        cbQuimicos.setOnCheckedChangeListener((v, c)  -> { if (c) cbNinguno.setChecked(false); saveContaminacionPref(); });
        cbOtros.setOnCheckedChangeListener((v, c)     -> { if (c) cbNinguno.setChecked(false); saveContaminacionPref(); });

        rgProtegida.setOnCheckedChangeListener((g, id) ->
                Prefs.put(this, K_PROT, id == R.id.rbProtSi ? "Si" : "No"));
        rgImportante.setOnCheckedChangeListener((g, id) ->
                Prefs.put(this, K_IMP, id == R.id.rbImpSi ? "Si" : "No"));

        // Beneficios (múltiple)
        cbMejorSalud.setOnCheckedChangeListener((v, c) -> saveBeneficiosPref());
        cbMejorSabor.setOnCheckedChangeListener((v, c) -> saveBeneficiosPref());
        cbMenosEnf.setOnCheckedChangeListener((v, c)   -> saveBeneficiosPref());
        cbAhorroEcon.setOnCheckedChangeListener((v, c) -> saveBeneficiosPref());
        cbBenefOtro.setOnCheckedChangeListener((v, c)  -> saveBeneficiosPref());

        // ---- Navegación
        btnAnt.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, PrimeraVisitaAlmacenamientoActivity.class));
            finish();
        });
        btnSig.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, PrimeraVisitaSaneamientoActivity.class));
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
            // >>>> Claves normalizadas para el CSV <<<<
            data.put("contaminacion.contacto_fuentes",           getContaminacionCsv());
            data.put("contaminacion.fuente_protegida",           getRadioCsv(rgProtegida,  R.id.rbProtSi, R.id.rbProtNo));
            data.put("contaminacion.importancia_consumir_buena", getRadioCsv(rgImportante, R.id.rbImpSi,  R.id.rbImpNo));
            data.put("contaminacion.beneficios",                 getBeneficiosCsv());

            // Guardar en la sección "contaminacion"
            SessionCsvPrimera.saveSection(this, "contaminacion", data);
        } catch (Exception e) {
            Toast.makeText(this, "Error guardando: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /* ---------------- helpers ---------------- */

    private void saveContaminacionPref() { Prefs.put(this, K_CONT, getContaminacionCsv()); }
    private void saveBeneficiosPref()    { Prefs.put(this, K_BEN,  getBeneficiosCsv()); }

    private String getContaminacionCsv() {
        if (cbNinguno.isChecked()) return "Ninguno";
        List<String> l = new ArrayList<>();
        if (cbAnimales.isChecked()) l.add("Animales");
        if (cbAguasRes.isChecked()) l.add("Aguas Residuales");
        if (cbBasuras.isChecked())  l.add("Basuras");
        if (cbQuimicos.isChecked()) l.add("Químicos");
        if (cbOtros.isChecked())    l.add("Otros");
        return String.join(",", l);
    }

    private String getBeneficiosCsv() {
        List<String> l = new ArrayList<>();
        if (cbMejorSalud.isChecked()) l.add("Mejor Salud");
        if (cbMejorSabor.isChecked()) l.add("Mejor Sabor");
        if (cbMenosEnf.isChecked())   l.add("Menos enfermedades");
        if (cbAhorroEcon.isChecked()) l.add("Ahorro Económico");
        if (cbBenefOtro.isChecked())  l.add("Otro");
        return String.join(",", l);
    }

    private void setChecksFromCsv(String csv, boolean isContaminacion) {
        if (csv == null) csv = "";
        if (isContaminacion) {
            cbAnimales.setChecked(csv.contains("Animales"));
            cbAguasRes.setChecked(csv.contains("Aguas Residuales"));
            cbBasuras.setChecked(csv.contains("Basuras"));
            cbQuimicos.setChecked(csv.contains("Químicos"));
            cbOtros.setChecked(csv.contains("Otros"));
            cbNinguno.setChecked(csv.contains("Ninguno"));
        } else {
            cbMejorSalud.setChecked(csv.contains("Mejor Salud"));
            cbMejorSabor.setChecked(csv.contains("Mejor Sabor"));
            cbMenosEnf.setChecked(csv.contains("Menos enfermedades"));
            cbAhorroEcon.setChecked(csv.contains("Ahorro Económico"));
            cbBenefOtro.setChecked(csv.contains("Otro"));
        }
    }

    private void setRadioFromPref(RadioGroup rg, String key, int idSi, int idNo) {
        String v = Prefs.get(this, key);
        if ("Si".equalsIgnoreCase(v)) rg.check(idSi);
        else if ("No".equalsIgnoreCase(v)) rg.check(idNo);
        else rg.clearCheck();
    }

    private String getRadioCsv(RadioGroup rg, int idSi, int idNo) {
        int id = rg.getCheckedRadioButtonId();
        if (id == -1) return "";
        return id == idSi ? "Si" : "No";
    }

    @SuppressWarnings("unchecked")
    private <T> T req(int id) {
        T v = (T) findViewById(id);
        return Objects.requireNonNull(v,
                "Falta la vista con id: " + getResources().getResourceEntryName(id));
    }
}
