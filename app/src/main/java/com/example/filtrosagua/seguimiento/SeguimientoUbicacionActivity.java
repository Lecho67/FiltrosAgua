package com.example.filtrosagua.seguimiento;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.filtrosagua.HomeActivity;
import com.example.filtrosagua.PrimeraVisitaDemografiaActivity;
import com.example.filtrosagua.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class SeguimientoUbicacionActivity extends AppCompatActivity {

    private TextInputEditText etUbicacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seguimiento_ubicacion);

        etUbicacion = findViewById(R.id.etUbicacionSeg);
        MaterialButton btnAnterior = findViewById(R.id.btnAnteriorUbicacion);
        MaterialButton btnEnviar   = findViewById(R.id.btnEnviarEncuesta);

        btnAnterior.setOnClickListener(v -> {
            startActivity(new Intent(this,SeguimientoObservacionesTecnicasActivity.class));
            finish();
        });

        btnEnviar.setOnClickListener(v -> {
            String ubicacion = etUbicacion.getText() == null ? "" : etUbicacion.getText().toString().trim();
            // Aquí guardarías/mandarías tus datos…
            Toast.makeText(this, "Encuesta enviada", Toast.LENGTH_SHORT).show();

            // Volver al Home (opcional)
            Intent i = new Intent(this, HomeActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });
    }
}
