package com.example.filtrosagua;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class PrimeraVisitaDemografiaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita_demografia);

        TextInputEditText etMenor5 = findViewById(R.id.etMenor5);
        TextInputEditText et6a17   = findViewById(R.id.et6a17);
        TextInputEditText et18a64  = findViewById(R.id.et18a64);
        TextInputEditText et65mas  = findViewById(R.id.et65mas);

        MaterialButton btnAnterior = findViewById(R.id.btnAnterior);
        MaterialButton btnSiguiente = findViewById(R.id.btnSiguiente);

        // Navegación
        btnAnterior.setOnClickListener(v -> {
            startActivity(new Intent(this, PrimeraVisitaUbicacionActivity.class));
            finish();
        });

        btnSiguiente.setOnClickListener(v -> {
            startActivity(new Intent(this, PrimeraVisitaAccesoAguaActivity.class));
            finish();
        });

    }
}
