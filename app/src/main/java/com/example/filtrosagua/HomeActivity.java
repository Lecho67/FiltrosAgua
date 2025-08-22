package com.example.filtrosagua;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.filtrosagua.seguimiento.SeguimientoInfoBasicaActivity;
import com.google.android.material.button.MaterialButton;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Botones del layout
        MaterialButton btnPrimera     = findViewById(R.id.btnPrimera);
        MaterialButton btnSeguimiento = findViewById(R.id.btnSeguimiento);
        MaterialButton btnRegresar    = findViewById(R.id.btnRegresar);

        // Recibe datos del login
        final String user   = getIntent().getStringExtra("user");
        final String cedula = getIntent().getStringExtra("cedula"); // opcional

        // Ocultar (o desactivar) Seguimiento de forma segura
        if (btnSeguimiento != null) {
            btnSeguimiento.setVisibility(View.VISIBLE);  // Lo haces visible
            btnSeguimiento.setEnabled(true);             // Y habilitado
        }
        btnSeguimiento.setOnClickListener(v -> {
            Intent i = new Intent(HomeActivity.this, SeguimientoInfoBasicaActivity.class);
            if (user != null)   i.putExtra("user", user);
            if (cedula != null) i.putExtra("cedula", cedula);
            startActivity(i);
        });



        // Abrir Primera Visita reenviando datos
        btnPrimera.setOnClickListener(v -> {
            Intent i = new Intent(HomeActivity.this, PrimeraVisitaActivity.class);
            if (user != null)   i.putExtra("user", user);
            if (cedula != null) i.putExtra("cedula", cedula);
            startActivity(i);
        });

        // Volver al login
        btnRegresar.setOnClickListener(v -> finish());
    }
}
