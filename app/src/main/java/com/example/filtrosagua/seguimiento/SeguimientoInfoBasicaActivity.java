package com.example.filtrosagua.seguimiento;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.filtrosagua.PrimeraVisitaUbicacionActivity;
import com.example.filtrosagua.R;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class SeguimientoInfoBasicaActivity extends AppCompatActivity {

    private EditText etFechaSeg, etResponsableSeg, etEmpresaSeg, etNumeroSeg, etNumero2Seg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seguimiento_info_basica);

        // Binds con verificación (si un id no existe, lanza error claro)
        etFechaSeg       = require(R.id.etFechaSeg);
        etResponsableSeg = require(R.id.etResponsableSeg);
        etEmpresaSeg     = require(R.id.etEmpresaSeg);
        etNumeroSeg      = require(R.id.etNumeroSeg);
        etNumero2Seg     = require(R.id.etNumero2Seg);

        // Fecha actual
        String hoy = new SimpleDateFormat("MM/dd/yyyy", new Locale("es", "CO"))
                .format(new Date());
        etFechaSeg.setText(hoy);

        // Datos entrantes
        String user   = getIntent().getStringExtra("user");
        String cedula = getIntent().getStringExtra("cedula");

        if (user != null)   etResponsableSeg.setText(user);
        if (cedula != null) etNumeroSeg.setText(cedula);

        // Botones
        MaterialButton btnAnterior  = require(R.id.btnAnteriorSeg);
        MaterialButton btnSiguiente = require(R.id.btnSiguienteSeg);

        btnAnterior.setOnClickListener(v -> finish());

        btnSiguiente.setOnClickListener(v -> {
            startActivity(new Intent(this, SeguimientoAccesoAguaFiltroActivity.class));
            finish();
        });
    }

    private <T> T require(int id) {
        T v = (T) findViewById(id);
        return Objects.requireNonNull(v, "Falta la vista con id: " + getResources().getResourceEntryName(id));
    }
}
