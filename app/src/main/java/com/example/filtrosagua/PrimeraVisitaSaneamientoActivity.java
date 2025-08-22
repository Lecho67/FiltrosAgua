package com.example.filtrosagua;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class PrimeraVisitaSaneamientoActivity extends AppCompatActivity {

    private CheckBox cbSanitarioSi, cbSanitarioNo;
    private CheckBox cbSistemaSi, cbSistemaNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita_saneamiento);

        cbSanitarioSi = findViewById(R.id.cbSanitarioSi);
        cbSanitarioNo = findViewById(R.id.cbSanitarioNo);
        cbSistemaSi   = findViewById(R.id.cbSistemaSi);
        cbSistemaNo   = findViewById(R.id.cbSistemaNo);

        // Pares Sí/No exclusivos
        setExclusivePair(cbSanitarioSi, cbSanitarioNo);
        setExclusivePair(cbSistemaSi, cbSistemaNo);

        MaterialButton btnAnterior  = findViewById(R.id.btnAnteriorSaneamiento);
        MaterialButton btnSiguiente = findViewById(R.id.btnSiguienteSaneamiento);

        btnAnterior.setOnClickListener(v -> {
            startActivity(new Intent(this, PrimeraVisitaContaminacionActivity.class));
            finish();
        });

        btnSiguiente.setOnClickListener(v -> {
            startActivity(new Intent(this, PrimeraVisitaHigieneActivity.class));
            finish();
        });
    }

    private void setExclusivePair(CheckBox a, CheckBox b) {
        a.setOnCheckedChangeListener((btn, checked) -> { if (checked) b.setChecked(false); });
        b.setOnCheckedChangeListener((btn, checked) -> { if (checked) a.setChecked(false); });
    }
}
