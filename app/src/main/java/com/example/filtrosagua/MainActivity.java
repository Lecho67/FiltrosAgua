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
import com.example.filtrosagua.util.SessionCsv;
import com.example.filtrosagua.util.SessionCsvPrimera;
import com.example.filtrosagua.util.CsvMerge;   // <-- IMPORTANTE
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

        // Ingresar
        btnIngresar.setOnClickListener(v -> {
            String u = text(etUsuario);
            String p = text(etContrasena);

            if (TextUtils.isEmpty(u)) {
                etUsuario.setError("Ingresa el usuario");
                etUsuario.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(p)) {
                etContrasena.setError("Ingresa la contraseña");
                etContrasena.requestFocus();
                return;
            }

            hideKeyboard(v);

            // DEMO simple: cambia por tu validación real
            if (u.equals("admin") && p.equals("1234")) {
                Intent i = new Intent(MainActivity.this, HomeActivity.class);
                i.putExtra("user", u);
                startActivity(i);
            } else {
                Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
            }
        });

        // Actualizar (limpiar campos y errores)
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

    // ---------------- COMPARTIR UNIFICADO ----------------
    private void compartirMaestroUnificado() {
        try {
            // Opcional: asegúrate de que lo último en staging esté en los maestros
            // (si el usuario no cerró sesión todavía)
            try {
                SessionCsv.commitToMaster(this);
                SessionCsvPrimera.commitToMaster(this);
            } catch (Exception ignored) {}

            // Generar /files/csv/maestro_unificado_YYYYMMDD_HHMMSS.csv
            File unificado = CsvMerge.crearMaestroUnificado(this);
            if (unificado == null || !unificado.exists() || unificado.length() == 0L) {
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

            startActivity(Intent.createChooser(send, "Compartir maestro unificado"));
            Toast.makeText(this, "Generado:\n" + unificado.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error al compartir: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    // -----------------------------------------------------

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
