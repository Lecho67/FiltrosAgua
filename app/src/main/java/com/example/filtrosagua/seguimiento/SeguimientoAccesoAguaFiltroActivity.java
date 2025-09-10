package com.example.filtrosagua.seguimiento;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.filtrosagua.R;
import com.example.filtrosagua.util.Prefs;
import com.example.filtrosagua.util.SessionCsv;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class SeguimientoAccesoAguaFiltroActivity extends AppCompatActivity {

    private EditText etFechaSeg2, etFuenteAgua, etPorqueArcilla, etDuracionDias,
            etVecesRecarga, etMiembroEncargado, etUsoAgua;

    private static final String K_FECHA   = "seg2_fecha";
    private static final String K_FUENTE  = "seg2_fuente";
    private static final String K_PORQUE  = "seg2_porque_arcilla";
    private static final String K_DIAS    = "seg2_dias_almacenada";
    private static final String K_VECES   = "seg2_veces_recarga";
    private static final String K_MIEMBRO = "seg2_miembro_recarga";
    private static final String K_USO     = "seg2_uso_agua";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seguimiento_acceso_agua_filtro);

        etFechaSeg2        = req(R.id.etFechaSeg2);
        ImageButton btnCal = req(R.id.btnPickDate);
        etFuenteAgua       = req(R.id.etFuenteAgua);
        etPorqueArcilla    = req(R.id.etPorqueArcilla);
        etDuracionDias     = req(R.id.etDuracionDias);
        etVecesRecarga     = req(R.id.etVecesRecarga);
        etMiembroEncargado = req(R.id.etMiembroEncargado);
        etUsoAgua          = req(R.id.etUsoAgua);

        MaterialButton btnAnterior  = req(R.id.btnAnteriorSeg2);
        MaterialButton btnSiguiente = req(R.id.btnSiguienteSeg2);

        // Prefill desde Prefs
        etFechaSeg2.setText(Prefs.get(this, K_FECHA));
        etFuenteAgua.setText(Prefs.get(this, K_FUENTE));
        etPorqueArcilla.setText(Prefs.get(this, K_PORQUE));
        etDuracionDias.setText(Prefs.get(this, K_DIAS));
        etVecesRecarga.setText(Prefs.get(this, K_VECES));
        etMiembroEncargado.setText(Prefs.get(this, K_MIEMBRO));
        etUsoAgua.setText(Prefs.get(this, K_USO));

        // Autosave a Prefs
        autosave(etFechaSeg2,        K_FECHA);
        autosave(etFuenteAgua,       K_FUENTE);
        autosave(etPorqueArcilla,    K_PORQUE);
        autosave(etDuracionDias,     K_DIAS);
        autosave(etVecesRecarga,     K_VECES);
        autosave(etMiembroEncargado, K_MIEMBRO);
        autosave(etUsoAgua,          K_USO);

        // DatePicker
        btnCal.setOnClickListener(v -> showDatePicker());

        btnAnterior.setOnClickListener(v -> {
            saveSectionNow(); // guardar ANTES de volver
            startActivity(new Intent(this, SeguimientoInfoBasicaActivity.class));
            finish();
        });

        btnSiguiente.setOnClickListener(v -> {
            saveSectionNow(); // guardar ANTES de avanzar
            startActivity(new Intent(this, SeguimientoPercepcionesCambiosActivity.class));
            finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSectionNow(); // guarda también si se minimiza / cambia
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveSectionNow(); // refuerzo extra
    }

    private Map<String, String> buildData() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("fecha",           fn(t(etFechaSeg2),        Prefs.get(this, K_FECHA)));
        data.put("fuente_agua",     fn(t(etFuenteAgua),       Prefs.get(this, K_FUENTE)));
        data.put("porque_arcilla",  fn(t(etPorqueArcilla),    Prefs.get(this, K_PORQUE)));
        data.put("dias_almacenada", fn(t(etDuracionDias),     Prefs.get(this, K_DIAS)));
        data.put("veces_recarga",   fn(t(etVecesRecarga),     Prefs.get(this, K_VECES)));
        data.put("miembro_recarga", fn(t(etMiembroEncargado), Prefs.get(this, K_MIEMBRO)));
        data.put("uso_del_agua",    fn(t(etUsoAgua),          Prefs.get(this, K_USO)));
        return data;
    }

    private void saveSectionNow() {
        try {
            SessionCsv.saveSection(this, "acceso_agua_filtro", buildData());
            // DEBUG útil: confirma que sí se escribió y muestra la ruta
            // (puedes comentarlo cuando verifiques que ya guarda bien)
            // Toast.makeText(this, "Guardado acceso_agua_filtro → " + SessionCsv.file(this).getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // Si algo sale mal, evita crash pero avisa en desarrollo
            // Toast.makeText(this, "Error guardando sección: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ===== helpers =====

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    Calendar sel = Calendar.getInstance();
                    sel.set(y, m, d);
                    String fmt = new SimpleDateFormat("MM/dd/yyyy", Locale.US).format(sel.getTime());
                    etFechaSeg2.setText(fmt);
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        );
        dp.show();
    }

    private void autosave(EditText et, String key) {
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                Prefs.put(SeguimientoAccesoAguaFiltroActivity.this, key, s.toString());
            }
        });
    }

    private static String t(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private static String fn(String a, String b) {
        return (a == null || a.isEmpty()) ? (b == null ? "" : b) : a;
    }

    @SuppressWarnings("unchecked")
    private <T> T req(int id) {
        T v = (T) findViewById(id);
        return Objects.requireNonNull(v,
                "Falta la vista con id: " + getResources().getResourceEntryName(id));
    }
}
