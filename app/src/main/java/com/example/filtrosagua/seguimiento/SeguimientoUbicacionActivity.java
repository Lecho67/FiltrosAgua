package com.example.filtrosagua.seguimiento;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.filtrosagua.MainActivity;
import com.example.filtrosagua.R;
import com.example.filtrosagua.util.SessionCsvSeguimiento;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class SeguimientoUbicacionActivity extends AppCompatActivity {

    private static final int REQ_LOCATION = 501;

    // NUEVOS: dirección y observaciones
    private EditText etDireccion, etObservaciones;
    private boolean submitting = false;
    // combos
    private MaterialAutoCompleteTextView actDepartamentoSeg, actMunicipioSeg, actVeredaSeg;

    // GPS (sin campos visibles)
    private FusedLocationProviderClient fused;
    private final CancellationTokenSource cts = new CancellationTokenSource();
    private String latStr = "", altStr = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seguimiento_ubicacion);

        // refs UI

        actDepartamentoSeg = req(R.id.actDepartamentoSeg);
        actMunicipioSeg    = req(R.id.actMunicipioSeg);
        actVeredaSeg       = req(R.id.actVeredaSeg);
        etDireccion        = req(R.id.etDireccion);
        etObservaciones    = req(R.id.etObservaciones);
        MaterialButton btnAnterior = req(R.id.btnAnteriorSeg6);
        MaterialButton btnEnviar   = req(R.id.btnEnviarEncuesta);

        // Adapters
        int depArrayResId = getResources().getIdentifier("departamentos_valle", "array", getPackageName());
        ArrayAdapter<CharSequence> depAdapter =
                depArrayResId != 0
                        ? ArrayAdapter.createFromResource(this, depArrayResId, android.R.layout.simple_list_item_1)
                        : new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new String[]{"Valle del Cauca"});
        actDepartamentoSeg.setAdapter(depAdapter);
        if (TextUtils.isEmpty(actDepartamentoSeg.getText())) {
            actDepartamentoSeg.setText(depAdapter.getItem(0).toString(), false);
        }

        ArrayAdapter<CharSequence> munAdapter = ArrayAdapter.createFromResource(
                this, R.array.municipios_valle, android.R.layout.simple_list_item_1);
        actMunicipioSeg.setAdapter(munAdapter);

        actMunicipioSeg.setOnItemClickListener((p, v, pos, id) -> cargarVeredasDesdeMunicipio());
        actDepartamentoSeg.setOnClickListener(v -> actDepartamentoSeg.showDropDown());
        actMunicipioSeg.setOnClickListener(v -> actMunicipioSeg.showDropDown());
        actVeredaSeg.setOnClickListener(v -> actVeredaSeg.showDropDown());

        if (!TextUtils.isEmpty(textOf(actMunicipioSeg))) cargarVeredasDesdeMunicipio();

        // GPS
        fused = LocationServices.getFusedLocationProviderClient(this);
        ensureLocationAndFill();

        // Botones
        btnAnterior.setOnClickListener(v -> {
            saveSectionNow();
            startActivity(new Intent(this, SeguimientoObservacionesTecnicasActivity.class));
            finish();
        });

        btnEnviar.setOnClickListener(v -> {
            try {
                submitting = true;
                cts.cancel();
                saveSectionNow();
                SessionCsvSeguimiento.commitToMasterWide(this);
                SessionCsvSeguimiento.clearSession(this);
                Toast.makeText(this, "Encuesta guardada.", Toast.LENGTH_LONG).show();
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "Error al enviar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override protected void onPause() {
        super.onPause();
        if (!submitting) saveSectionNow(); // no escribir staging si estamos enviando
    }

    @Override protected void onDestroy() {
        cts.cancel(); // asegurar que no queden callbacks
        super.onDestroy();
    }

    // ====== GPS ======
    private void ensureLocationAndFill() {
        boolean fine = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQ_LOCATION);
            return;
        }
        fetchLocation();
    }

    private void fetchLocation() {
        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(loc -> {
                    if (loc != null) setLatAlt(loc);
                    else fused.getLastLocation().addOnSuccessListener(this::setLatAlt);
                })
                .addOnFailureListener(e ->
                        fused.getLastLocation().addOnSuccessListener(this::setLatAlt));
    }

    private void setLatAlt(Location loc) {
        if (loc == null) return;
        latStr = String.valueOf(loc.getLatitude());
        altStr = String.valueOf(loc.getAltitude());
        if (!submitting) saveSectionNow(); // evitar re-escrituras post-clear
    }

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) ensureLocationAndFill();
    }

    // ====== CSV ======
    /** Guarda la sección 'ubicacion' con todas las claves canónicas (GPS + texto usuario). */
    private void saveSectionNow() {
        if (submitting) return; // guardia extra
        try {
            Map<String, String> data = new LinkedHashMap<>();
            data.put("ubicacion.latitud", latStr);
            data.put("ubicacion.altitud", altStr);
            data.put("ubicacion.departamento", textOf(actDepartamentoSeg));
            data.put("ubicacion.municipio", textOf(actMunicipioSeg));
            data.put("ubicacion.vereda_corregimiento", textOf(actVeredaSeg));
            data.put("ubicacion.direccion", t(etDireccion));
            data.put("ubicacion.observaciones", t(etObservaciones));
            SessionCsvSeguimiento.saveSection(this, "ubicacion", data);
        } catch (Exception ignored) {}
    }

    // ====== Veredas dependientes ======
    private void cargarVeredasDesdeMunicipio() {
        String municipio = textOf(actMunicipioSeg);
        actVeredaSeg.setText("", false);

        String key = normalizarClaveRecurso(municipio);
        if ("calima_el_darien".equals(key)) key = "calima";

        int verArrayResId = getResources().getIdentifier(key, "array", getPackageName());
        if (verArrayResId != 0) {
            ArrayAdapter<CharSequence> verAdapter = ArrayAdapter.createFromResource(
                    this, verArrayResId, android.R.layout.simple_list_item_1);
            actVeredaSeg.setEnabled(true);
            actVeredaSeg.setAdapter(verAdapter);
        } else {
            actVeredaSeg.setEnabled(false);
            actVeredaSeg.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new String[]{}));
            Toast.makeText(this, "Sin catálogo de veredas para " + municipio, Toast.LENGTH_SHORT).show();
        }
    }

    private String normalizarClaveRecurso(String s) {
        if (s == null) return "";
        String t = s.replaceAll("\\(.*?\\)", "");
        t = Normalizer.normalize(t, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();
        t = t.replace("/", "_").replace("-", "_").trim();
        t = t.replaceAll("[^a-z0-9_\\s]", "");
        t = t.replaceAll("\\s+", "_");
        return t;
    }

    // helpers
    private String t(EditText et) { return et.getText() == null ? "" : et.getText().toString().trim(); }
    private String textOf(MaterialAutoCompleteTextView at) { return at.getText() == null ? "" : at.getText().toString().trim(); }
    @SuppressWarnings("unchecked") private <T> T req(int id) { T v = (T) findViewById(id); return Objects.requireNonNull(v); }
}
