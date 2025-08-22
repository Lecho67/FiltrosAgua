package com.example.filtrosagua.seguimiento;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.filtrosagua.PrimeraVisitaDemografiaActivity;
import com.example.filtrosagua.R;
import com.google.android.material.button.MaterialButton;

public class SeguimientoMantenimientoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seguimiento_mantenimiento);

        MaterialButton btnAnterior = findViewById(R.id.btnAnteriorSeg4);
        MaterialButton btnSiguiente = findViewById(R.id.btnSiguienteSeg4);

        btnAnterior.setOnClickListener(v -> {
            startActivity(new Intent(this, SeguimientoPercepcionesCambiosActivity.class));
            finish();
        });

        btnSiguiente.setOnClickListener(v -> {
            startActivity(new Intent(this, SeguimientoObservacionesTecnicasActivity.class));
            finish();
        });
    }
}
