package com.example.filtrosagua;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

public class PrimeraVisitaUbicacionActivity extends AppCompatActivity {

    private static final String TAG = "UbicacionActivity";

    /** Normaliza: quita tildes, pasa a minúsculas y recorta */
    private String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.toLowerCase().trim();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primera_visita_ubicacion);

        TextInputEditText etDepartamento = findViewById(R.id.etDepartamento);
        MaterialAutoCompleteTextView acMunicipio = findViewById(R.id.acMunicipio);
        MaterialAutoCompleteTextView acVereda    = findViewById(R.id.acVereda);
        TextInputLayout tilVereda                = findViewById(R.id.tilVereda);
        MaterialButton btnAnterior               = findViewById(R.id.btnAnterior);
        MaterialButton btnSiguiente              = findViewById(R.id.btnSiguiente);

        // Departamento fijo
        etDepartamento.setText("Valle del Cauca");
        etDepartamento.setEnabled(false);

        // MUNICIPIOS
        ArrayAdapter<String> municipiosAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.municipios_valle)
        );
        acMunicipio.setAdapter(municipiosAdapter);
        acMunicipio.setThreshold(0); // mostrar sugerencias sin escribir
        acMunicipio.setOnClickListener(v -> acMunicipio.showDropDown());
        acMunicipio.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) acMunicipio.showDropDown(); });

        // VEREDAS: estado inicial
        acVereda.setText("");
        acVereda.setThreshold(0);
        // Por defecto mostramos la flecha; si no hay lista la ocultamos
        tilVereda.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
        acVereda.setOnClickListener(v -> {
            if (tilVereda.getEndIconMode() == TextInputLayout.END_ICON_DROPDOWN_MENU) {
                acVereda.showDropDown();
            }
        });
        acVereda.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && tilVereda.getEndIconMode() == TextInputLayout.END_ICON_DROPDOWN_MENU) {
                acVereda.showDropDown();
            }
        });

        // MAPA municipio -> arreglo de veredas (usa claves normalizadas)
        Map<String, Integer> mapVeredas = new HashMap<>();
        mapVeredas.put(normalize("Cali"),    R.array.corregimientos_cali);
        mapVeredas.put(normalize("Palmira"), R.array.corregimientos_palmira);
        mapVeredas.put(normalize("Jamundí"), R.array.corregimientos_jamundi);
        // Agrega más: mapVeredas.put(normalize("Tuluá"), R.array.corregimientos_tulua);

        // Cuando elijan MUNICIPIO:
        acMunicipio.setOnItemClickListener((parent, view, position, id) -> {
            String muni   = (String) parent.getItemAtPosition(position);
            String key    = normalize(muni);
            Integer resId = mapVeredas.get(key);

            Log.d(TAG, "Municipio seleccionado: " + muni + "  (key=" + key + "), resId=" + resId);
            Toast.makeText(this,
                    resId != null ? "Municipio con lista: " + muni : "Municipio sin lista: " + muni,
                    Toast.LENGTH_SHORT).show();

            if (resId != null) {
                // HAY LISTA → Modo dropdown
                ArrayAdapter<String> veredasAdapter = new ArrayAdapter<>(
                        this, android.R.layout.simple_list_item_1,
                        getResources().getStringArray(resId)
                );
                acVereda.setAdapter(veredasAdapter);
                acVereda.setText("", false);

                tilVereda.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
                acVereda.setInputType(InputType.TYPE_NULL); // evita teclado
                acVereda.clearFocus();
                acVereda.post(acVereda::showDropDown); // asegura apertura
            } else {
                // SIN LISTA → Campo editable
                acVereda.setAdapter(null);
                acVereda.setText("", false);

                tilVereda.setEndIconMode(TextInputLayout.END_ICON_NONE); // oculta flecha
                acVereda.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                acVereda.requestFocus();
            }
        });

        // Navegación
        btnAnterior.setOnClickListener(v -> {
            startActivity(new Intent(this, PrimeraVisitaBeneficiarioActivity.class));
            finish();
        });
        btnSiguiente.setOnClickListener(v -> {
            startActivity(new Intent(this, PrimeraVisitaDemografiaActivity.class));
            finish();
        });
    }
}
