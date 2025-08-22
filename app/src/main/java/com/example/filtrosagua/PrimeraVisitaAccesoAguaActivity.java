package com.example.filtrosagua;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.method.DigitsKeyListener;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class PrimeraVisitaAccesoAguaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita_acceso_agua);

        TextInputEditText etFuenteAgua     = findViewById(R.id.etFuenteAgua);
        TextInputEditText etAdminAcueducto = findViewById(R.id.etAdminAcueducto);
        TextInputEditText etHorasAgua      = findViewById(R.id.etHorasAgua);

        MaterialButton btnAnterior  = findViewById(R.id.btnAnterior);
        MaterialButton btnSiguiente = findViewById(R.id.btnSiguiente);

        // Horas: solo 0-9 y máximo 2 dígitos (0..24 a validar si quieres)
        etHorasAgua.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
        etHorasAgua.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(2) });

        // Navegación
        btnAnterior.setOnClickListener(v -> {
            startActivity(new Intent(this, PrimeraVisitaDemografiaActivity.class));
            finish();
        });

        btnSiguiente.setOnClickListener(v -> {
            startActivity(new Intent(this, PrimeraVisitaDesplazamientoActivity.class));
            finish();
        });

    }
}
