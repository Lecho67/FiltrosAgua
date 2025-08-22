package com.example.filtrosagua;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class PrimeraVisitaAlmacenamientoActivity extends AppCompatActivity {

    // Checks
    private CheckBox cbAlmacenaSi, cbAlmacenaNo;
    private CheckBox cbHervir, cbFiltrar, cbClorar, cbOtro, cbNinguno;

    // Fields
    private EditText etMetodoHervir, etResponsableLabores, etInversionMensual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita_almacenamiento);

        // === Referencias ===
        cbAlmacenaSi  = findViewById(R.id.cbAlmacenaSi);
        cbAlmacenaNo  = findViewById(R.id.cbAlmacenaNo);
        cbHervir      = findViewById(R.id.cbHervir);
        cbFiltrar     = findViewById(R.id.cbFiltrar);
        cbClorar      = findViewById(R.id.cbClorar);
        cbOtro        = findViewById(R.id.cbOtro);
        cbNinguno     = findViewById(R.id.cbNinguno);

        etMetodoHervir       = findViewById(R.id.etMetodoHervir);
        etResponsableLabores = findViewById(R.id.etResponsableLabores);
        etInversionMensual   = findViewById(R.id.etInversionMensual);

        // === Reglas: Si/No almacena (exclusivos) ===
        cbAlmacenaSi.setOnCheckedChangeListener((b, checked) -> {
            if (checked) cbAlmacenaNo.setChecked(false);
        });
        cbAlmacenaNo.setOnCheckedChangeListener((b, checked) -> {
            if (checked) cbAlmacenaSi.setChecked(false);
        });

        // === Reglas: Tratamientos (Ninguno desmarca otros) ===
        cbNinguno.setOnCheckedChangeListener((b, checked) -> {
            if (checked) {
                cbHervir.setChecked(false);
                cbFiltrar.setChecked(false);
                cbClorar.setChecked(false);
                cbOtro.setChecked(false);
                setMetodoHervirEnabled(false);
            }
        });

        // Habilitar/deshabilitar "En caso de hervir..." según cbHervir
        cbHervir.setOnCheckedChangeListener((b, checked) -> setMetodoHervirEnabled(checked));

        // === Botones navegación ===
        MaterialButton btnAnterior  = findViewById(R.id.btnAnteriorAlmacenamiento);
        MaterialButton btnSiguiente = findViewById(R.id.btnSiguienteAlmacenamiento);

        btnAnterior.setOnClickListener(v -> {
            startActivity(new Intent(this, PrimeraVisitaPercepcionAguaActivity.class));
            finish();
        });

        btnSiguiente.setOnClickListener(v -> {
            startActivity(new Intent(this, PrimeraVisitaContaminacionActivity.class));
            finish();
        });

        // Estado inicial
        setMetodoHervirEnabled(cbHervir.isChecked());
    }

    private void setMetodoHervirEnabled(boolean enabled) {
        etMetodoHervir.setEnabled(enabled);
        etMetodoHervir.setAlpha(enabled ? 1f : 0.5f);
        if (!enabled) etMetodoHervir.setText("");
    }
}
