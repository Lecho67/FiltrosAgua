package com.example.filtrosagua;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class PrimeraVisitaSaludActivity extends AppCompatActivity {

    // Dolor estómago (exclusivos)
    private CheckBox cbDolorSi, cbDolorNo;

    // Enfermedades (múltiple) + Ninguna (excluyente)
    private CheckBox cbEnfDiarrea, cbEnfVomito, cbEnfColera, cbEnfHepatitis, cbEnfParasitosis, cbEnfOtro, cbEnfNinguna;

    private EditText etObservaciones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita_salud);

        // Referencias
        cbDolorSi       = findViewById(R.id.cbDolorSi);
        cbDolorNo       = findViewById(R.id.cbDolorNo);

        cbEnfDiarrea       = findViewById(R.id.cbEnfDiarrea);
        cbEnfVomito        = findViewById(R.id.cbEnfVomito);
        cbEnfColera        = findViewById(R.id.cbEnfColera);
        cbEnfHepatitis     = findViewById(R.id.cbEnfHepatitis);
        cbEnfParasitosis   = findViewById(R.id.cbEnfParasitosis);
        cbEnfOtro          = findViewById(R.id.cbEnfOtro);
        cbEnfNinguna       = findViewById(R.id.cbEnfNinguna);

        etObservaciones = findViewById(R.id.etObservaciones);

        // Reglas: Sí/No exclusivos
        setExclusivePair(cbDolorSi, cbDolorNo);

        // Reglas: 'Ninguna' desmarca el resto
        cbEnfNinguna.setOnCheckedChangeListener((b, checked) -> {
            if (checked) {
                cbEnfDiarrea.setChecked(false);
                cbEnfVomito.setChecked(false);
                cbEnfColera.setChecked(false);
                cbEnfHepatitis.setChecked(false);
                cbEnfParasitosis.setChecked(false);
                cbEnfOtro.setChecked(false);
            }
        });
        // Si marca cualquiera, desmarcar 'Ninguna'
        setUncheck(cbEnfDiarrea, cbEnfNinguna);
        setUncheck(cbEnfVomito, cbEnfNinguna);
        setUncheck(cbEnfColera, cbEnfNinguna);
        setUncheck(cbEnfHepatitis, cbEnfNinguna);
        setUncheck(cbEnfParasitosis, cbEnfNinguna);
        setUncheck(cbEnfOtro, cbEnfNinguna);

        // Botones
        MaterialButton btnAnterior = findViewById(R.id.btnAnteriorSalud);
        MaterialButton btnEnviar   = findViewById(R.id.btnEnviarEncuesta);

        btnAnterior.setOnClickListener(v -> {
            startActivity(new Intent(this, PrimeraVisitaHigieneActivity.class));
            finish();
        });

        btnEnviar.setOnClickListener(v -> {
            // Aquí podrías validar/guardar/enviar al servidor.
            // Ejemplo simple de confirmación:
            Toast.makeText(this, "Encuesta enviada. ¡Gracias!", Toast.LENGTH_LONG).show();


            startActivity(new Intent(this, HomeActivity.class));
            finishAffinity();
        });
    }

    private void setExclusivePair(CheckBox a, CheckBox b) {
        a.setOnCheckedChangeListener((btn, checked) -> { if (checked) b.setChecked(false); });
        b.setOnCheckedChangeListener((btn, checked) -> { if (checked) a.setChecked(false); });
    }

    private void setUncheck(CheckBox source, CheckBox target) {
        source.setOnCheckedChangeListener((btn, checked) -> { if (checked) target.setChecked(false); });
    }
}
