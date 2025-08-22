package com.example.filtrosagua.seguimiento;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.filtrosagua.PrimeraVisitaDemografiaActivity;
import com.example.filtrosagua.PrimeraVisitaSaneamientoActivity;
import com.example.filtrosagua.R;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SeguimientoAccesoAguaFiltroActivity extends AppCompatActivity {

    private EditText etFechaSeg2, etFuenteAgua, etPorqueArcilla, etDuracionDias,
            etVecesRecarga, etMiembroEncargado, etUsoAgua;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seguimiento_acceso_agua_filtro);

        // Bind
        etFechaSeg2        = findViewById(R.id.etFechaSeg2);
        etFuenteAgua       = findViewById(R.id.etFuenteAgua);
        etPorqueArcilla    = findViewById(R.id.etPorqueArcilla);
        etDuracionDias     = findViewById(R.id.etDuracionDias);
        etVecesRecarga     = findViewById(R.id.etVecesRecarga);
        etMiembroEncargado = findViewById(R.id.etMiembroEncargado);
        etUsoAgua          = findViewById(R.id.etUsoAgua);

        // Fecha (DatePicker)
        findViewById(R.id.btnPickDate).setOnClickListener(v -> showDatePicker());

        // Botones
        MaterialButton btnAnterior  = findViewById(R.id.btnAnteriorSeg2);
        MaterialButton btnSiguiente = findViewById(R.id.btnSiguienteSeg2);

        btnAnterior.setOnClickListener(v -> {
            startActivity(new Intent(this, SeguimientoInfoBasicaActivity.class));
            finish();
        });

        btnSiguiente.setOnClickListener(v -> {
            startActivity(new Intent(this, SeguimientoPercepcionesCambiosActivity.class));
            finish();
        });
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH);
        int d = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dlg = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar sel = Calendar.getInstance();
                    sel.set(year, month, dayOfMonth);
                    String fmt = new SimpleDateFormat("MM/dd/yyyy",
                            new Locale("es", "CO")).format(sel.getTime());
                    etFechaSeg2.setText(fmt);
                },
                y, m, d
        );
        dlg.show();
    }
}
