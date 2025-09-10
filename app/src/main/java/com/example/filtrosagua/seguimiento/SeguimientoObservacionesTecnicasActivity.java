package com.example.filtrosagua.seguimiento;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.filtrosagua.R;
import com.example.filtrosagua.util.Prefs;
import com.example.filtrosagua.util.SessionCsv;
import com.google.android.material.button.MaterialButton;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class SeguimientoObservacionesTecnicasActivity extends AppCompatActivity {

    private RadioGroup rgEstableSeguro, rgEnsambladoTapado, rgLimpioExterno,
            rgLavadoPrevio, rgManipArcilla, rgLimpiezaTanque, rgLimpiezaVasija,
            rgFisurasArcilla, rgNivelesAgua, rgInstLavadoManos, rgJabon;

    private static final String K_ESTABLE   = "seg5_estable_seguro";
    private static final String K_ENSAMB    = "seg5_ensamblado_tapado";
    private static final String K_LIMPIEXT  = "seg5_limpio_externo";
    private static final String K_LAVPREV   = "seg5_lavado_prev";
    private static final String K_ARCILLA   = "seg5_manip_arcilla";
    private static final String K_TANQUE    = "seg5_limp_tanque";
    private static final String K_VASIJA    = "seg5_limp_vasija";
    private static final String K_FISURAS   = "seg5_fisuras_arcilla";
    private static final String K_NIVELES   = "seg5_niveles_agua";
    private static final String K_INST      = "seg5_inst_lav_manos";
    private static final String K_JABON     = "seg5_jabon";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seguimiento_observaciones_tecnicas);

        rgEstableSeguro    = req(R.id.rgEstableSeguro);
        rgEnsambladoTapado = req(R.id.rgEnsambladoTapado);
        rgLimpioExterno    = req(R.id.rgLimpioExterno);
        rgLavadoPrevio     = req(R.id.rgLavadoPrevio);
        rgManipArcilla     = req(R.id.rgManipArcilla);
        rgLimpiezaTanque   = req(R.id.rgLimpiezaTanque);
        rgLimpiezaVasija   = req(R.id.rgLimpiezaVasija);
        rgFisurasArcilla   = req(R.id.rgFisurasArcilla);
        rgNivelesAgua      = req(R.id.rgNivelesAgua);
        rgInstLavadoManos  = req(R.id.rgInstLavadoManos);
        rgJabon            = req(R.id.rgJabon);

        MaterialButton btnAnt = req(R.id.btnAnteriorSeg5);
        MaterialButton btnSig = req(R.id.btnSiguienteSeg5);

        // restaurar radios
        setRadioFromPrefs(rgEstableSeguro,    Prefs.get(this, K_ESTABLE));
        setRadioFromPrefs(rgEnsambladoTapado, Prefs.get(this, K_ENSAMB));
        setRadioFromPrefs(rgLimpioExterno,    Prefs.get(this, K_LIMPIEXT));
        setRadioFromPrefs(rgLavadoPrevio,     Prefs.get(this, K_LAVPREV));
        setRadioFromPrefs(rgManipArcilla,     Prefs.get(this, K_ARCILLA));
        setRadioFromPrefs(rgLimpiezaTanque,   Prefs.get(this, K_TANQUE));
        setRadioFromPrefs(rgLimpiezaVasija,   Prefs.get(this, K_VASIJA));
        setRadioFromPrefs(rgFisurasArcilla,   Prefs.get(this, K_FISURAS));
        setRadioFromPrefs(rgNivelesAgua,      Prefs.get(this, K_NIVELES));
        setRadioFromPrefs(rgInstLavadoManos,  Prefs.get(this, K_INST));
        setRadioFromPrefs(rgJabon,            Prefs.get(this, K_JABON));

        // autosave
        listenAndSave(rgEstableSeguro,    K_ESTABLE);
        listenAndSave(rgEnsambladoTapado, K_ENSAMB);
        listenAndSave(rgLimpioExterno,    K_LIMPIEXT);
        listenAndSave(rgLavadoPrevio,     K_LAVPREV);
        listenAndSave(rgManipArcilla,     K_ARCILLA);
        listenAndSave(rgLimpiezaTanque,   K_TANQUE);
        listenAndSave(rgLimpiezaVasija,   K_VASIJA);
        listenAndSave(rgFisurasArcilla,   K_FISURAS);
        listenAndSave(rgNivelesAgua,      K_NIVELES);
        listenAndSave(rgInstLavadoManos,  K_INST);
        listenAndSave(rgJabon,            K_JABON);

        btnAnt.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, SeguimientoMantenimientoActivity.class));
            finish();
        });

        btnSig.setOnClickListener(v -> {
            try {
                saveSectionNow();
                startActivity(new Intent(this, SeguimientoUbicacionActivity.class));
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "Error guardando: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
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
            data.put("estable_seguro",                       selected(rgEstableSeguro));
            data.put("ensamblado_tapado",                    selected(rgEnsambladoTapado));
            data.put("limpio_parte_externa",                 selected(rgLimpioExterno));
            data.put("lavado_manos_previo",                  selected(rgLavadoPrevio));
            data.put("manipulacion_arcilla_adecuada",        selected(rgManipArcilla));
            data.put("limpieza_tanque_sin_sedimentos",       selected(rgLimpiezaTanque));
            data.put("limpieza_vasija_sin_sedimentos",       selected(rgLimpiezaVasija));
            data.put("fisuras_arcilla",                      selected(rgFisurasArcilla));
            data.put("niveles_agua_impiden_manipulacion",    selected(rgNivelesAgua));
            data.put("instalacion_lavado_manos",             selected(rgInstLavadoManos));
            data.put("disp_jabon_lavado_manos",              selected(rgJabon));
            SessionCsv.saveSection(this, "observaciones_tecnicas", data);
        } catch (Exception ignored) {}
    }

    // ---------- helpers ----------
    private void setRadioFromPrefs(RadioGroup group, String val) {
        if ("Si".equalsIgnoreCase(val)) checkYes(group);
        else if ("No".equalsIgnoreCase(val)) checkNo(group);
        else group.clearCheck();
    }

    private void listenAndSave(RadioGroup group, String key) {
        group.setOnCheckedChangeListener((g, id) -> {
            Prefs.put(this, key, "Si".equalsIgnoreCase(selectedText(id)) ? "Si"
                    : "No".equalsIgnoreCase(selectedText(id)) ? "No" : "");
            saveSectionNow();
        });
    }

    private String selected(RadioGroup g) {
        return normalize(selectedText(g.getCheckedRadioButtonId()));
    }

    private String selectedText(int id) {
        if (id == -1) return "";
        RadioButton rb = findViewById(id);
        return rb == null ? "" : rb.getText().toString().trim();
    }

    private String normalize(String s) {
        if ("si".equalsIgnoreCase(s)) return "Si";
        if ("no".equalsIgnoreCase(s)) return "No";
        return s == null ? "" : s;
    }

    private void checkYes(RadioGroup g) {
        for (int i = 0; i < g.getChildCount(); i++) {
            if (g.getChildAt(i) instanceof RadioButton) {
                RadioButton rb = (RadioButton) g.getChildAt(i);
                if ("si".equalsIgnoreCase(rb.getText().toString())) { rb.setChecked(true); return; }
            }
        }
        g.clearCheck();
    }

    private void checkNo(RadioGroup g) {
        for (int i = 0; i < g.getChildCount(); i++) {
            if (g.getChildAt(i) instanceof RadioButton) {
                RadioButton rb = (RadioButton) g.getChildAt(i);
                if ("no".equalsIgnoreCase(rb.getText().toString())) { rb.setChecked(true); return; }
            }
        }
        g.clearCheck();
    }

    @SuppressWarnings("unchecked")
    private <T> T req(int id) {
        T v = (T) findViewById(id);
        return Objects.requireNonNull(v,
                "Falta la vista con id: " + getResources().getResourceEntryName(id));
    }
}
