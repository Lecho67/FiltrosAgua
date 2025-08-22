package com.example.filtrosagua;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class PrimeraVisitaDesplazamientoActivity extends AppCompatActivity {

    private RadioButton rbSi, rbNo;
    private TextInputEditText etMedio, etMinutos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita_desplazamiento);

        rbSi = findViewById(R.id.rbSi);
        rbNo = findViewById(R.id.rbNo);
        etMedio = findViewById(R.id.etMedio);
        etMinutos = findViewById(R.id.etMinutos);

        MaterialButton btnAnterior = findViewById(R.id.btnAnteriorDesplazamiento);
        MaterialButton btnSiguiente = findViewById(R.id.btnSiguienteDesplazamiento);

        // Estado inicial: nada seleccionado -> deshabilitamos campos
        setCamposHabilitados(false);

        rbSi.setOnCheckedChangeListener((b, checked) -> {
            if (checked) setCamposHabilitados(true);
        });

        rbNo.setOnCheckedChangeListener((b, checked) -> {
            if (checked) {
                setCamposHabilitados(false);
                if (etMedio.getText() != null) etMedio.setText("");
                if (etMinutos.getText() != null) etMinutos.setText("");
            }
        });

        // Volver a la sección anterior
        btnAnterior.setOnClickListener(v -> {
        startActivity(new Intent(this, PrimeraVisitaAccesoAguaActivity.class));
        finish();
        });

        // Ir a la siguiente sección (cámbiala por tu siguiente Activity)
        btnSiguiente.setOnClickListener(v -> {
            startActivity(new Intent(this, PrimeraVisitaPercepcionAguaActivity.class));
            finish();
        });
    }

    private void setCamposHabilitados(boolean enabled) {
        etMedio.setEnabled(enabled);
        etMinutos.setEnabled(enabled);
        etMedio.setAlpha(enabled ? 1f : 0.5f);
        etMinutos.setAlpha(enabled ? 1f : 0.5f);
    }
}
