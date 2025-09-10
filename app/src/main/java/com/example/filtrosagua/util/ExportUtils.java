package com.example.filtrosagua.util;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public final class ExportUtils {

    private ExportUtils() { }

    // ---------- API 29+ (MediaStore) y <=28 (carpeta Descargas clásica) ----------
    /** Copia el archivo 'src' a Descargas con nombre 'displayName' y devuelve la Uri destino. */
    @Nullable
    public static Uri exportToDownloads(Context ctx, File src, String displayName) throws IOException {
        if (src == null || !src.exists() || src.length() == 0) {
            throw new IOException("Archivo de origen inválido.");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ : MediaStore (no requiere permisos)
            ContentResolver cr = ctx.getContentResolver();
            Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);

            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, displayName);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
            values.put(MediaStore.Downloads.IS_PENDING, 1);

            Uri uri = cr.insert(collection, values);
            if (uri == null) throw new IOException("No se pudo crear el destino en Descargas.");

            try (OutputStream os = cr.openOutputStream(uri);
                 FileInputStream fis = new FileInputStream(src)) {

                byte[] buf = new byte[8192];
                int n;
                while ((n = fis.read(buf)) != -1) {
                    os.write(buf, 0, n);
                }
                os.flush();
            }

            // Marcar como listo (visible)
            ContentValues done = new ContentValues();
            done.put(MediaStore.Downloads.IS_PENDING, 0);
            cr.update(uri, done, null, null);

            return uri;

        } else {
            // Android 9 o menor: escribir directo a /sdcard/Download/FiltrosAgua/
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File outDir = new File(downloads, "FiltrosAgua");
            if (!outDir.exists() && !outDir.mkdirs()) {
                throw new IOException("No se pudo crear la carpeta: " + outDir.getAbsolutePath());
            }
            File dst = new File(outDir, displayName);

            try (FileInputStream fis = new FileInputStream(src);
                 FileOutputStream fos = new FileOutputStream(dst)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = fis.read(buf)) != -1) {
                    fos.write(buf, 0, n);
                }
                fos.flush();
            }

            // Hacer visible en galería/gestores de archivos
            MediaScannerConnection.scanFile(ctx,
                    new String[]{dst.getAbsolutePath()},
                    new String[]{"text/csv"},
                    null);

            return Uri.fromFile(dst);
        }
    }

    // ---------- Permiso legacy (sólo necesario en API <= 28) ----------
    /** Pide WRITE_EXTERNAL_STORAGE si hace falta. Devuelve true si ya se tiene o no se requiere. */
    public static boolean ensureLegacyWritePermission(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true; // no se necesita permiso con MediaStore
        }
        int p = ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (p == PERMISSION_GRANTED) return true;

        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                requestCode);
        return false;
    }
}
