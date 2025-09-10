package com.example.filtrosagua;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.filtrosagua.util.Prefs;
import com.example.filtrosagua.util.SessionCsvPrimera;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class PrimeraVisitaActivity extends AppCompatActivity {

    // Vistas
    private EditText etFecha, etResp, etEmp, etCed;
    private ImageButton btnPickDate;
    private MaterialButton btnAnterior, btnSiguiente;

    // Claves Prefs
    private static final String K_FECHA = "pv_fecha";
    private static final String K_RESP  = "pv_resp";
    private static final String K_EMP   = "pv_emp";
    private static final String K_CED   = "pv_ced";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita);

        // Bind
        etFecha     = req(R.id.etPvFecha);
        etResp      = req(R.id.etPvResponsable);
        etEmp       = req(R.id.etPvEmpresa);
        etCed       = req(R.id.etPvCedula);
        btnPickDate = req(R.id.btnPvPickDate);
        btnAnterior = req(R.id.btnPvAnterior);
        btnSiguiente= req(R.id.btnPvSiguiente);

        // Relleno inicial (autosave)
        etFecha.setText(Prefs.get(this, K_FECHA));
        etResp.setText(Prefs.get(this, K_RESP));
        etEmp.setText(Prefs.get(this, K_EMP));
        etCed.setText(Prefs.get(this, K_CED));

        autosave(etFecha, K_FECHA);
        autosave(etResp,  K_RESP);
        autosave(etEmp,   K_EMP);
        autosave(etCed,   K_CED);

        // DatePicker
        btnPickDate.setOnClickListener(v -> showDatePicker());

        // Navegación
        btnAnterior.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });

        btnSiguiente.setOnClickListener(v -> {
            try {
                saveSectionNow(); // guarda sección en primeravisita.csv
                // AVANZAR a la siguiente página de Primera Visita
                startActivity(new Intent(this, PrimeraVisitaBeneficiarioActivity.class));
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

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    Calendar sel = Calendar.getInstance();
                    sel.set(y, m, d);
                    String fmt = new SimpleDateFormat("MM/dd/yyyy", Locale.US).format(sel.getTime());
                    etFecha.setText(fmt);
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
                Prefs.put(PrimeraVisitaActivity.this, key, s.toString());
            }
        });
    }

    private Map<String, String> buildData() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("fecha",       t(etFecha));
        data.put("responsable", t(etResp));
        data.put("empresa",     t(etEmp));
        data.put("cedula",      t(etCed));
        return data;
    }

    private void saveSectionNow() {
        try {
            // guarda/actualiza sección "info_responsable" en primeravisita.csv (staging)
            SessionCsvPrimera.saveSection(this, "info_responsable", buildData());
        } catch (Exception ignored) {}
    }

    private String t(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    @SuppressWarnings("unchecked")
    private <T> T req(int id) {
        T v = (T) findViewById(id);
        return Objects.requireNonNull(v,
                "Falta la vista con id: " + getResources().getResourceEntryName(id));
    }
}
