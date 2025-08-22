package com.example.filtrosagua;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class PrimeraVisitaHigieneActivity extends AppCompatActivity {

    private CheckBox cbCapacitacionSi, cbCapacitacionNo;
    private CheckBox cbLavadoManos, cbLimpiezaHogar, cbCepillado, cbBanoDiario, cbOtro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita_higiene);

        // Referencias
        cbCapacitacionSi  = findViewById(R.id.cbCapacitacionSi);
        cbCapacitacionNo  = findViewById(R.id.cbCapacitacionNo);
        cbLavadoManos     = findViewById(R.id.cbLavadoManos);
        cbLimpiezaHogar   = findViewById(R.id.cbLimpiezaHogar);
        cbCepillado       = findViewById(R.id.cbCepillado);
        cbBanoDiario      = findViewById(R.id.cbBanoDiario);
        cbOtro            = findViewById(R.id.cbOtro);

        // Pares Sí/No exclusivos
        setExclusivePair(cbCapacitacionSi, cbCapacitacionNo);

        // Botones
        MaterialButton btnAnterior  = findViewById(R.id.btnAnteriorHigiene);
        MaterialButton btnSiguiente = findViewById(R.id.btnSiguienteHigiene);

        btnAnterior.setOnClickListener(v -> {
            startActivity(new Intent(this, PrimeraVisitaSaneamientoActivity.class));
            finish();
        });

        btnSiguiente.setOnClickListener(v -> {
            startActivity(new Intent(this, PrimeraVisitaSaludActivity.class));
            finish();
        });
    }

    private void setExclusivePair(CheckBox a, CheckBox b) {
        a.setOnCheckedChangeListener((btn, checked) -> { if (checked) b.setChecked(false); });
        b.setOnCheckedChangeListener((btn, checked) -> { if (checked) a.setChecked(false); });
    }
}
