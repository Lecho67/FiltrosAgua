package com.example.filtrosagua.seguimiento;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.filtrosagua.PrimeraVisitaDemografiaActivity;
import com.example.filtrosagua.PrimeraVisitaUbicacionActivity;
import com.example.filtrosagua.R;
import com.google.android.material.button.MaterialButton;

public class SeguimientoPercepcionesCambiosActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seguimiento_percepciones_cambios);

        MaterialButton btnAnterior  = findViewById(R.id.btnAnteriorSeg3);
        MaterialButton btnSiguiente = findViewById(R.id.btnSiguienteSeg3);

        btnAnterior.setOnClickListener(v -> {
            startActivity(new Intent(this, SeguimientoAccesoAguaFiltroActivity.class));
            finish();
        });

        btnSiguiente.setOnClickListener(v -> {
            startActivity(new Intent(this, SeguimientoMantenimientoActivity.class));
            finish();
        });
    }
}
