package com.example.filtrosagua.seguimiento;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.filtrosagua.PrimeraVisitaDemografiaActivity;
import com.example.filtrosagua.R;
import com.google.android.material.button.MaterialButton;

public class SeguimientoObservacionesTecnicasActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seguimiento_observaciones_tecnicas);

        MaterialButton btnAnterior = findViewById(R.id.btnAnteriorObsTec);
        MaterialButton btnSiguiente = findViewById(R.id.btnSiguienteObsTec);

        btnAnterior.setOnClickListener(v -> {
            startActivity(new Intent(this, SeguimientoMantenimientoActivity.class));
            finish();
        });

        btnSiguiente.setOnClickListener(v -> {
            startActivity(new Intent(this, SeguimientoUbicacionActivity.class));
            finish();
        });
    }
}
