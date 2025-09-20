package com.example.filtrosagua;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.filtrosagua.util.Prefs;
import com.example.filtrosagua.util.SessionCsvPrimera;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class PrimeraVisitaUbicacionActivity extends AppCompatActivity {

    private MaterialAutoCompleteTextView acDepartamento, acMunicipio, acVereda;
    private TextInputEditText etDireccion;
    private MaterialButton btnAnterior, btnSiguiente;

    private static final String K_DEP = "pv_ubi_departamento";
    private static final String K_MUN = "pv_ubi_municipio";
    private static final String K_VER = "pv_ubi_vereda";
    private static final String K_DIR = "pv_ubi_direccion";

    // === GPS ===
    private static final int REQ_LOC = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private String currentLat = "";
    private String currentAlt = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita_ubicacion);

        acDepartamento = findViewById(R.id.acDepartamento);
        acMunicipio    = findViewById(R.id.acMunicipio);
        acVereda       = findViewById(R.id.acVereda);
        etDireccion    = findViewById(R.id.etDireccion);
        btnAnterior    = findViewById(R.id.btnPvAnterior);
        btnSiguiente   = findViewById(R.id.btnPvSiguiente);

        // Init GPS client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocationIfPermitted();

        String depSaved = Prefs.get(this, K_DEP);
        if (depSaved.isEmpty()) depSaved = "Valle del Cauca";
        acDepartamento.setText(depSaved, false);

        loadMunicipiosValle();

        String munSaved = Prefs.get(this, K_MUN);
        String verSaved = Prefs.get(this, K_VER);
        String dirSaved = Prefs.get(this, K_DIR);

        if (!munSaved.isEmpty()) {
            acMunicipio.setText(munSaved, false);
            loadCorregimientosFor(munSaved);
        }
        if (!verSaved.isEmpty()) acVereda.setText(verSaved, false);
        if (!dirSaved.isEmpty()) etDireccion.setText(dirSaved);

        addAutosave(acDepartamento, K_DEP);
        addAutosave(acMunicipio,    K_MUN);
        addAutosave(acVereda,       K_VER);
        addAutosave(etDireccion,    K_DIR);

        acDepartamento.setOnItemClickListener((p, v, pos, id) -> {
            Prefs.put(this, K_DEP, text(acDepartamento));
            acMunicipio.setText("", false);
            acVereda.setText("", false);
            Prefs.put(this, K_MUN, "");
            Prefs.put(this, K_VER, "");
            loadMunicipiosValle();
        });

        acMunicipio.setOnItemClickListener((p, v, pos, id) -> {
            String municipio = text(acMunicipio);
            Prefs.put(this, K_MUN, municipio);
            acVereda.setText("", false);
            Prefs.put(this, K_VER, "");
            loadCorregimientosFor(municipio);
        });

        btnAnterior.setOnClickListener(v -> {
            saveSectionNow();
            SessionCsvPrimera.fMasterWide(this); // compat
            startActivity(new Intent(this, PrimeraVisitaBeneficiarioActivity.class));
            finish();
        });

        btnSiguiente.setOnClickListener(v -> {
            saveSectionNow();
            SessionCsvPrimera.fMasterWide(this); // compat
            startActivity(new Intent(this, PrimeraVisitaDemografiaActivity.class));
            finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSectionNow();
        SessionCsvPrimera.fMasterWide(this); // compat
    }

    /* ==================== GPS helpers ==================== */

    private void requestLocationIfPermitted() {
        boolean fineOk = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarseOk = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (!fineOk && !coarseOk) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQ_LOC);
            return;
        }

        // Obtener última ubicación conocida (rápido y suficiente para tu caso)
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, loc -> {
                    if (loc != null) {
                        currentLat = String.valueOf(loc.getLatitude());
                        currentAlt = String.valueOf(loc.getAltitude());
                    }
                })
                .addOnFailureListener(e -> {
                    // silencioso; mantenemos cadenas vacías si falla
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOC) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationIfPermitted();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado. Lat/Alt quedarán vacíos.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /* ================= Helpers ================= */

    private void addAutosave(MaterialAutoCompleteTextView v, String key) {
        v.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                Prefs.put(PrimeraVisitaUbicacionActivity.this, key, s == null ? "" : s.toString().trim());
            }
        });
    }

    private void addAutosave(EditText v, String key) {
        v.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                Prefs.put(PrimeraVisitaUbicacionActivity.this, key, s == null ? "" : s.toString().trim());
            }
        });
    }

    private void setAdapterFromArray(MaterialAutoCompleteTextView view, int arrayResId) {
        String[] items = (arrayResId == 0) ? new String[]{} : getResources().getStringArray(arrayResId);
        view.setAdapter(new ArrayAdapter<>(this, R.layout.list_item_dropdown, items));
    }

    private void loadMunicipiosValle() {
        int resId = getResources().getIdentifier("municipios_valle", "array", getPackageName());
        setAdapterFromArray(acMunicipio, resId);
        if (resId == 0) {
            Toast.makeText(this, "No se encontró el array municipios_valle", Toast.LENGTH_LONG).show();
        }
    }

    private void loadCorregimientosFor(String municipio) {
        String slug = slug(municipio);
        int resId = getResources().getIdentifier("corregimientos_" + slug, "array", getPackageName());
        if (resId == 0) resId = getResources().getIdentifier(slug, "array", getPackageName());
        if (resId == 0 && municipio.toLowerCase(Locale.ROOT).contains("calima"))
            resId = getResources().getIdentifier("calima", "array", getPackageName());
        if (resId == 0 && slug.contains("_")) {
            String first = slug.split("_", 2)[0];
            resId = getResources().getIdentifier(first, "array", getPackageName());
        }

        setAdapterFromArray(acVereda, resId);

        if (resId == 0) {
            acVereda.setText("", false);
            Toast.makeText(this, "Sin corregimientos para: " + municipio, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveSectionNow() {
        try {
            Map<String, String> data = new LinkedHashMap<>();
            data.put("departamento",         text(acDepartamento));
            data.put("municipio",            text(acMunicipio));
            data.put("vereda_corregimiento", text(acVereda));
            data.put("direccion",            text(etDireccion)); // -> ubicacion.direccion
            data.put("latitud",              currentLat);        // -> ubicacion.latitud
            data.put("altitud",              currentAlt);        // -> ubicacion.altitud
            SessionCsvPrimera.saveSection(this, "ubicacion", data);
        } catch (Exception ignored) {}
    }

    private String text(MaterialAutoCompleteTextView v) {
        return v.getText() == null ? "" : v.getText().toString().trim();
    }

    private String text(EditText v) {
        return v.getText() == null ? "" : v.getText().toString().trim();
    }

    private String slug(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        n = n.toLowerCase(Locale.ROOT).trim();
        n = n.replaceAll("[()]", "");
        n = n.replaceAll("[^a-z0-9\\s_\\-]", "");
        n = n.replaceAll("\\s+", "_");
        return n;
    }
}
