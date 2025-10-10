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
import com.example.filtrosagua.util.SessionCsvSeguimiento;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class SeguimientoInfoBasicaActivity extends AppCompatActivity {

    private EditText etFechaSeg, etResponsableSeg, etEmpresaSeg, etNumeroSeg, etNumero2Seg;
    private ImageButton btnPickDate;

    private static final String K_FECHA = "seg_info_fecha";
    private static final String K_RESP  = "seg_info_resp";
    private static final String K_EMP   = "seg_info_emp";
    private static final String K_NUM1  = "seg_info_num1";
    private static final String K_NUM2  = "seg_info_num2";

    private long lastSaveMs = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seguimiento_info_basica);

        etFechaSeg       = require(R.id.etFechaSeg);
        etResponsableSeg = require(R.id.etResponsableSeg);
        etEmpresaSeg     = require(R.id.etEmpresaSeg);
        etNumeroSeg      = require(R.id.etNumeroSeg);
        etNumero2Seg     = require(R.id.etNumero2Seg);
        btnPickDate      = require(R.id.btnPvPickDate);

        String fechaPref = Prefs.get(this, K_FECHA);
        if (fechaPref.isEmpty()) {
            String hoy = new SimpleDateFormat("MM/dd/yyyy", Locale.US).format(new Date());
            etFechaSeg.setText(hoy);
        } else {
            etFechaSeg.setText(fechaPref);
        }
        etResponsableSeg.setText(Prefs.get(this, K_RESP));
        etEmpresaSeg.setText(Prefs.get(this, K_EMP));
        etNumeroSeg.setText(Prefs.get(this, K_NUM1));
        etNumero2Seg.setText(Prefs.get(this, K_NUM2));

        String user   = getIntent().getStringExtra("user");
        String cedula = getIntent().getStringExtra("cedula");
        if (etResponsableSeg.getText().toString().isEmpty() && user != null) {
            etResponsableSeg.setText(user);
            Prefs.put(this, K_RESP, user);
        }
        if (etNumeroSeg.getText().toString().isEmpty() && cedula != null) {
            etNumeroSeg.setText(cedula);
            Prefs.put(this, K_NUM1, cedula);
        }

        attachAutosave(etFechaSeg,       K_FECHA);
        attachAutosave(etResponsableSeg, K_RESP);
        attachAutosave(etEmpresaSeg,     K_EMP);
        attachAutosave(etNumeroSeg,      K_NUM1);
        attachAutosave(etNumero2Seg,     K_NUM2);

        MaterialButton btnAnterior  = require(R.id.btnAnteriorSeg);
        MaterialButton btnSiguiente = require(R.id.btnSiguienteSeg);

        // DatePicker
        btnPickDate.setOnClickListener(v -> showDatePicker());

        btnAnterior.setOnClickListener(v -> { saveSectionNow(); finish(); });

        btnSiguiente.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, SeguimientoAccesoAguaFiltroActivity.class));
            finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSectionNow();
    }

    private Map<String, String> buildData() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("fecha",       t(etFechaSeg));
        data.put("responsable", t(etResponsableSeg));
        data.put("empresa",     t(etEmpresaSeg));
        data.put("cedula",     t(etNumeroSeg));
        data.put("telefono",     t(etNumero2Seg));
        return data;
    }

    private void saveSectionNow() {
        long now = System.currentTimeMillis();
        if (now - lastSaveMs < 300) return; // anti-rebote opcional
        lastSaveMs = now;
        try { SessionCsvSeguimiento.saveSection(this, "info_responsable", buildData()); } catch (Exception ignored) {}
    }

    private void attachAutosave(EditText et, String key) {
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { Prefs.put(SeguimientoInfoBasicaActivity.this, key, s.toString()); }
        });
        Prefs.put(this, key, t(et));
    }

    private String t(EditText et) {
        return (et.getText() == null) ? "" : et.getText().toString().trim();
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    Calendar sel = Calendar.getInstance();
                    sel.set(y, m, d);
                    String fmt = new SimpleDateFormat("MM/dd/yyyy", Locale.US).format(sel.getTime());
                    etFechaSeg.setText(fmt);
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        );
        dp.show();
    }

    private <T> T require(int id) {
        T v = (T) findViewById(id);
        return Objects.requireNonNull(v, "Falta la vista con id: " + getResources().getResourceEntryName(id));
    }
}