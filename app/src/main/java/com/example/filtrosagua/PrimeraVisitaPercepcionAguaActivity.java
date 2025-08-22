package com.example.filtrosagua;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class PrimeraVisitaPercepcionAguaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita_percepcion_agua);

        MaterialButton btnAnterior  = findViewById(R.id.btnAnterior);
        MaterialButton btnSiguiente = findViewById(R.id.btnSiguiente);

        // Volver a la sección anterior (ajusta si tu flujo tiene una sección 6 diferente)
        btnAnterior.setOnClickListener(v -> {
            startActivity(new Intent(this, PrimeraVisitaDesplazamientoActivity.class));
            finish();
        });

        // Ir a la siguiente pantalla del flujo
        btnSiguiente.setOnClickListener(v -> {
            startActivity(new Intent(this, PrimeraVisitaAlmacenamientoActivity.class));
            finish();
        });
    }
}
