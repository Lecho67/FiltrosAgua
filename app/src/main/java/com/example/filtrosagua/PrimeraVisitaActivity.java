package com.example.filtrosagua;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PrimeraVisitaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita);

        // ---- Botones ----
        MaterialButton btnAnterior  = findViewById(R.id.btnAnterior);
        MaterialButton btnSiguiente = findViewById(R.id.btnSiguiente);

        btnAnterior.setOnClickListener(v -> finish());

        btnSiguiente.setOnClickListener(v -> {
            Intent i = new Intent(PrimeraVisitaActivity.this,
                    PrimeraVisitaBeneficiarioActivity.class);
            startActivity(i);
        });

        // ---- Campos ----
        TextInputEditText etFecha        = findViewById(R.id.etFecha);
        TextInputEditText etResponsable  = findViewById(R.id.etResponsable);

        // 1) FECHA: hoy en formato dd/MM/yyyy
        String fechaHoy = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                .format(new Date());
        etFecha.setText(fechaHoy);
        etFecha.setEnabled(false); // solo lectura (puedes cambiar por focusable=false si prefieres)

        // 2) RESPONSABLE: pon aquí el nombre real (prefs/login/etc.)
        String nombreResponsable = "Juan Pérez"; // TODO: reemplazar por tu fuente real
        etResponsable.setText(nombreResponsable);
        etResponsable.setEnabled(false); // solo lectura
    }
}
