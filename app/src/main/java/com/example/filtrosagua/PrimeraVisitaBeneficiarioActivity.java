package com.example.filtrosagua;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class PrimeraVisitaBeneficiarioActivity extends AppCompatActivity {

    private EditText etTipoBeneficiario, etGrupoPoblacional, etNombreBeneficiario,
            etCedulaBeneficiario, etTelefonoBeneficiario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita_beneficiario);

        // === EditTexts ===
        etTipoBeneficiario     = findViewById(R.id.etTipoBeneficiario);
        etGrupoPoblacional     = findViewById(R.id.etGrupoPoblacional);
        etNombreBeneficiario   = findViewById(R.id.etNombreBeneficiario);
        etCedulaBeneficiario   = findViewById(R.id.etCedulaBeneficiario);
        etTelefonoBeneficiario = findViewById(R.id.etTelefonoBeneficiario);

        // === Botones (IDs EXACTOS del XML) ===
        MaterialButton btnAnterior  = findViewById(R.id.btnAnterior);
        MaterialButton btnSiguiente = findViewById(R.id.btnSiguiente);

        // Botón "Anterior" → regresa a la pantalla anterior
        btnAnterior.setOnClickListener(v -> finish());

        // Botón "Siguiente" → abrirá la sección 3
        btnSiguiente.setOnClickListener(v -> {
            startActivity(new Intent(this, PrimeraVisitaUbicacionActivity.class));
            finish();
        });

    }
}
