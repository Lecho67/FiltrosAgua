package com.example.filtrosagua;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

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

            // Validaciones básicas
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

            // Ocultar teclado para mejor UX
            hideKeyboard(v);

            // DEMO: credenciales fijas. Cambia por tu backend cuando lo tengas.
            if (u.equals("admin") && p.equals("1234")) {
                Intent i = new Intent(MainActivity.this, HomeActivity.class);
                i.putExtra("user", u);         // enviamos el nombre de usuario
                // Si más adelante tienes la cédula desde el login, descomenta:
                // i.putExtra("cedula", "1234567890");
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

        // Compartir (incluye el usuario si ya está escrito)
        btnCompartir.setOnClickListener(v -> {
            String u = text(etUsuario);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            String cuerpo = "Estoy usando la app FiltrosAgua";
            if (!u.isEmpty()) cuerpo += " con el usuario: " + u;
            send.putExtra(Intent.EXTRA_TEXT, cuerpo);
            startActivity(Intent.createChooser(send, "Compartir con"));
        });
    }

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
}
