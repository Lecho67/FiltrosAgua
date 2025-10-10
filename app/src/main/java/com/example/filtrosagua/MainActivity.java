package com.example.filtrosagua;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.filtrosagua.util.Prefs;
import com.example.filtrosagua.util.SessionCsvPrimera;
import com.example.filtrosagua.util.SessionCsvSeguimiento;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etUsuario, etContrasena;
    private MaterialButton btnIngresar, btnActualizar, btnCompartir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etUsuario     = findViewById(R.id.etUsuario);
        etContrasena  = findViewById(R.id.etContrasena);
        btnIngresar   = findViewById(R.id.btnIngresar);
        btnActualizar = findViewById(R.id.btnActualizar);
        btnCompartir  = findViewById(R.id.btnCompartir);

        // INGRESAR
        btnIngresar.setOnClickListener(v -> {
            String cedula = text(etUsuario);
            String passUser = text(etContrasena);

            if (TextUtils.isEmpty(cedula)) {
                etUsuario.setError("Ingresa la cédula");
                etUsuario.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(passUser)) {
                etContrasena.setError("Ingresa la contraseña");
                etContrasena.requestFocus();
                return;
            }

            hideKeyboard(v);

            // Validar formato de la cédula
            if (!cedula.matches("\\d{7,}")) {
                Toast.makeText(this, "La cédula debe tener mínimo 7 dígitos y solo números", Toast.LENGTH_LONG).show();
                return;
            }

            // Generar contraseña según tu algoritmo
            String generated = generarContrasena(cedula);

            if (passUser.equals(generated)) {
                Toast.makeText(this, "Acceso concedido ✅", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(MainActivity.this, HomeActivity.class);
                i.putExtra("user", cedula);
                startActivity(i);
            } else {
                Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
            }
        });

        // ACTUALIZAR: limpiar campos
        btnActualizar.setOnClickListener(v -> {
            etUsuario.setText("");
            etContrasena.setText("");
            etUsuario.setError(null);
            etContrasena.setError(null);
            etUsuario.requestFocus();
        });

        // COMPARTIR: generar maestro_unificado y compartirlo
        btnCompartir.setOnClickListener(v -> compartirMaestroUnificado());
    }

    // ------------------ GENERAR CONTRASEÑA ------------------
    private String generarContrasena(String cedula) {
        // mapa de reemplazo
        String[] mapa = {"H","L","J","T","V","Z","M","I","R","C"};

        StringBuilder reemplazada = new StringBuilder();
        for (char c : cedula.toCharArray()) {
            int idx = Character.getNumericValue(c);
            if (idx >= 0 && idx <= 9) {
                reemplazada.append(mapa[idx]);
            }
        }

        String ultimos6 = reemplazada.substring(Math.max(reemplazada.length() - 6, 0));
        String primeros5 = ultimos6.substring(0, Math.min(5, ultimos6.length()));

        // Contraseña: cedula[3] + primeros5 + cedula[0]
        return cedula.charAt(3) + primeros5 + cedula.charAt(0);
    }
    // --------------------------------------------------------

    // ---------------- COMPARTIR UNIFICADO -------------------
    private void compartirMaestroUnificado() {
        try {
            // Intentar consolidar lo que haya en staging a la versión WIDE unificada
            try {
                SessionCsvPrimera.commitToMasterWide(this);
            } catch (Exception ignored) {}
            try {
                SessionCsvSeguimiento.commitToMasterWide(this);
            } catch (Exception ignored) {}

            File unificado = new File(getFilesDir(), "csv/encuestas_master_wide.csv");
            if (!unificado.exists() || unificado.length() == 0L) {
                Toast.makeText(this, "No hay datos para compartir todavía.", Toast.LENGTH_LONG).show();
                return;
            }

            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    unificado
            );

            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/csv");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, "Compartir encuestas (unificado)"));

            Toast.makeText(this, "Archivo: \n" + unificado.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error al compartir: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    // --------------------------------------------------------

    // Utilidades
    private String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void hideKeyboard(View view) {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } catch (Exception ignored) { }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Prefs.clearAll(this);
    }
}
