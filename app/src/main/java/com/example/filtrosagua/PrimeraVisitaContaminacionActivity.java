package com.example.filtrosagua;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class PrimeraVisitaContaminacionActivity extends AppCompatActivity {

    // Contaminación (múltiple) + 'Ninguno'
    private CheckBox cbContAnimales, cbContAguasRes, cbContBasuras, cbContQuimicos, cbContOtros, cbContNinguno;

    // Fuente protegida (Sí/No)
    private CheckBox cbProtegidaSi, cbProtegidaNo;

    // Importancia (Sí/No)
    private CheckBox cbImportanciaSi, cbImportanciaNo;

    // Beneficios (múltiple)
    private CheckBox cbBenMejorSalud, cbBenMejorSabor, cbBenMenosEnfermedades, cbBenAhorroEconomico, cbBenOtro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita_contaminacion);

        // === Referencias ===
        cbContAnimales  = findViewById(R.id.cbContAnimales);
        cbContAguasRes  = findViewById(R.id.cbContAguasRes);
        cbContBasuras   = findViewById(R.id.cbContBasuras);
        cbContQuimicos  = findViewById(R.id.cbContQuimicos);
        cbContOtros     = findViewById(R.id.cbContOtros);
        cbContNinguno   = findViewById(R.id.cbContNinguno);

        cbProtegidaSi   = findViewById(R.id.cbProtegidaSi);
        cbProtegidaNo   = findViewById(R.id.cbProtegidaNo);

        cbImportanciaSi = findViewById(R.id.cbImportanciaSi);
        cbImportanciaNo = findViewById(R.id.cbImportanciaNo);

        cbBenMejorSalud         = findViewById(R.id.cbBenMejorSalud);
        cbBenMejorSabor         = findViewById(R.id.cbBenMejorSabor);
        cbBenMenosEnfermedades  = findViewById(R.id.cbBenMenosEnfermedades);
        cbBenAhorroEconomico    = findViewById(R.id.cbBenAhorroEconomico);
        cbBenOtro               = findViewById(R.id.cbBenOtro);

        // === Reglas ===

        // 'Ninguno' desmarca el resto de fuentes de contaminación
        cbContNinguno.setOnCheckedChangeListener((b, checked) -> {
            if (checked) {
                cbContAnimales.setChecked(false);
                cbContAguasRes.setChecked(false);
                cbContBasuras.setChecked(false);
                cbContQuimicos.setChecked(false);
                cbContOtros.setChecked(false);
            }
        });

        // Si el usuario marca cualquiera distinto de 'Ninguno', se desmarca 'Ninguno'
        setUncheckNingunoWhenOthersChecked(cbContAnimales, cbContNinguno);
        setUncheckNingunoWhenOthersChecked(cbContAguasRes, cbContNinguno);
        setUncheckNingunoWhenOthersChecked(cbContBasuras, cbContNinguno);
        setUncheckNingunoWhenOthersChecked(cbContQuimicos, cbContNinguno);
        setUncheckNingunoWhenOthersChecked(cbContOtros, cbContNinguno);

        // Pares Sí/No exclusivos
        setExclusivePair(cbProtegidaSi, cbProtegidaNo);
        setExclusivePair(cbImportanciaSi, cbImportanciaNo);

        // === Navegación ===
        MaterialButton btnAnterior  = findViewById(R.id.btnAnteriorContaminacion);
        MaterialButton btnSiguiente = findViewById(R.id.btnSiguienteContaminacion);

        btnAnterior.setOnClickListener(v -> {
            startActivity(new Intent(this, PrimeraVisitaAlmacenamientoActivity.class));
            finish();
        });

        btnSiguiente.setOnClickListener(v -> {
            startActivity(new Intent(this, PrimeraVisitaSaneamientoActivity.class));
            finish();
        });
    }

    private void setExclusivePair(CheckBox a, CheckBox b) {
        a.setOnCheckedChangeListener((btn, checked) -> { if (checked) b.setChecked(false); });
        b.setOnCheckedChangeListener((btn, checked) -> { if (checked) a.setChecked(false); });
    }

    private void setUncheckNingunoWhenOthersChecked(CheckBox other, CheckBox ninguno) {
        other.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) ninguno.setChecked(false);
        });
    }
}
