package com.winlator.tru;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.winlator.R;
import com.winlator.contentdialog.ContentDialog;
import com.winlator.core.AppUtils;
import com.winlator.core.LocaleHelper;
import com.winlator.core.PreloaderDialog;
import com.winlator.xserver.ScreenInfo;

import java.io.File;
import java.util.ArrayList;

public class TruLauncherActivity extends AppCompatActivity {
    private static final int REQUEST_PICK_FOLDER = 51;

    private TruPrefs prefs;
    private PreloaderDialog preloader;
    private TextView tvStatus;
    private TextView tvGameDir;
    private TextView tvVersion;

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LocaleHelper.setSystemLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tru_main_activity);
        prefs = new TruPrefs(this);
        preloader = new PreloaderDialog(this);

        requestStoragePermissions();

        tvStatus = findViewById(R.id.TVStatus);
        tvGameDir = findViewById(R.id.TVGameDir);
        tvVersion = findViewById(R.id.TVVersion);
        tvVersion.setText(AppUtils.getVersionName(this)+" | WINLATOR ENGINE 11.2");

        findViewById(R.id.BTPlay).setOnClickListener(v -> onPlay());
        findViewById(R.id.BTSettings).setOnClickListener(v -> onSettings());
        findViewById(R.id.BTExit).setOnClickListener(v -> onExit());

        findViewById(R.id.BTChipResolution).setOnClickListener(v -> showResolutionDialog());
        findViewById(R.id.BTChipPerformance).setOnClickListener(v -> showPerformanceDialog());
        findViewById(R.id.BTChipHud).setOnClickListener(v -> showHudDialog());

        if (!prefs.getFirstRunDone()) {
            showWelcomeDialog();
            prefs.setFirstRunDone(true);
        }

        refreshStatus();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        refreshStatus();
    }

    /* ---------------- РАЗРЕШЕНИЯ ---------------- */

    private static final int REQUEST_STORAGE = 101;

    private void requestStoragePermissions() {
        boolean writeOk = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        boolean readOk = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        if (!writeOk || !readOk) {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("TRU", "onRequestPermissionsResult: requestCode=" + requestCode + " results=" + java.util.Arrays.toString(grantResults));
    }

    /* ---------------- РЕПЛИКИ ---------------- */

    private void onPlay() {
        File exe = TruEngine.getExecutableFile(prefs);
        if (exe == null) {
            showPickFolderDialog();
            return;
        }
        startGamePipeline();
    }

    private void onSettings() {
        startActivity(new Intent(this, TruSettingsActivity.class));
    }

    private void onExit() {
        finishAffinity();
    }

    /* ---------------- ЗАПУСК ---------------- */

    private void startGamePipeline() {
        if (!TruEngine.isRootFsInstalled(this)) {
            AppUtils.keepScreenOn(this);
            preloader.show(R.string.installing_system_files);
            TruEngine.installRootFs(this, ok -> {
                preloader.closeOnUiThread();
                if (ok != Boolean.TRUE) {
                    Log.e("TRU", "installRootFs failed");
                    showFatal("Ошибка установки системных файлов (rootfs).\nПроверьте интернет-соединение и попробуйте снова.");
                    return;
                }
                ensureContainerAndRun();
            });
        }
        else ensureContainerAndRun();
    }

    private void ensureContainerAndRun() {
        AppUtils.keepScreenOn(this);
        preloader.show(R.string.loading);
        TruEngine.ensureContainer(this, (container, created) -> {
            preloader.closeOnUiThread();
            if (container == null) {
                Log.e("TRU", "ensureContainer returned null");
                showFatal("Не удалось создать контейнер механизма Winlator.");
                return;
            }
            TruEngine.runGame(this, container);
        });
    }

    /* ---------------- ВЫБОР ПАПКИ ---------------- */

    private void showPickFolderDialog() {
        final ContentDialog dialog = new ContentDialog(this, R.layout.tru_folder_dialog);
        dialog.setTitle(getString(R.string.tru_pick_folder));

        final EditText etPath = dialog.findViewById(R.id.ETGamePath);
        etPath.setText(prefs.getGameDir());

        dialog.findViewById(R.id.BTBrowse).setOnClickListener(v ->
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_PICK_FOLDER));

        dialog.setOnConfirmCallback(() -> applyGameDir(etPath.getText().toString().trim()));
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @androidx.annotation.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_FOLDER && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) applyGameDir(TruEngine.treeUriToPath(uri));
        }
    }

    private void applyGameDir(String path) {
        if (path.isEmpty()) return;
        File dir = new File(path);
        if (!dir.isDirectory()) {
            Log.e("TRU", "applyGameDir: path is not a directory: " + path + " (exists=" + dir.exists() + ", canRead=" + dir.canRead() + ")");
            showFatal("Папка не найдена или недоступна:\n" + path + "\n\nПроверьте разрешение на доступ к файлам.");
            return;
        }
        prefs.setGameDir(dir.getAbsolutePath());

        File exe = TruEngine.findGameExecutable(dir.getAbsolutePath());
        if (exe == null) {
            Log.e("TRU", "applyGameDir: no .exe found in " + dir.getAbsolutePath());
            showFatal("Не найден .exe в папке:\n" + dir.getAbsolutePath());
            return;
        }
        prefs.setExeName(exe.getName());

        refreshStatus();
        startGamePipeline();
    }

    /* ---------------- ДИАЛОГИ (Быстрые настройки) ---------------- */

    private void showWelcomeDialog() {
        ContentDialog dialog = new ContentDialog(this);
        dialog.setTitle(getString(R.string.tru_welcome_title));
        dialog.setMessage(getString(R.string.tru_welcome_text));
        dialog.show();
    }

    private void showFatal(String message) {
        ContentDialog dialog = new ContentDialog(this);
        dialog.setMessage(message);
        dialog.show();
    }

    public void showResolutionDialog() {
        String[] values = TruPrefs.resolutions();
        String[] labels = {
            getString(R.string.trt_res_720p),
            getString(R.string.trt_res_1080p),
            getString(R.string.trt_res_1440p)
        };
        int checked = indexOf(values, prefs.getResolution());
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.tru_resolution_title)
            .setSingleChoiceItems(labels, checked, (d, which) -> {
                prefs.setResolution(values[which]);
                d.dismiss();
                refreshStatus();
            })
            .show();
    }

    public void showPerformanceDialog() {
        String[] values = TruPrefs.perfPresets();
        String[] labels = {
            getString(R.string.trt_perf_stability),
            getString(R.string.trt_perf_medium),
            getString(R.string.trt_perf_performance)
        };
        int checked = indexOf(values, prefs.getPerfPreset());
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.tru_perf_title)
            .setSingleChoiceItems(labels, checked, (d, which) -> {
                prefs.setPerfPreset(values[which]);
                d.dismiss();
                refreshStatus();
            })
            .show();
    }

    public void showHudDialog() {
        int[] values = new int[]{
            com.winlator.widget.FrameRating.Mode.DISABLED.ordinal(),
            com.winlator.widget.FrameRating.Mode.SIMPLE.ordinal(),
            com.winlator.widget.FrameRating.Mode.FULL.ordinal()
        };
        String[] labels = {
            getString(R.string.trt_hud_off),
            getString(R.string.trt_hud_simple),
            getString(R.string.trt_hud_full)
        };
        int checked = indexOfInt(values, prefs.getHudMode());
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.tru_hud_title)
            .setSingleChoiceItems(labels, checked, (d, which) -> {
                prefs.setHudMode(values[which]);
                d.dismiss();
                refreshStatus();
            })
            .show();
    }

    private static int indexOf(String[] arr, String value) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(value)) return i;
        return 0;
    }

    private static int indexOfInt(int[] arr, int value) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == value) return i;
        return 0;
    }

    /* ---------------- СТАТУС ---------------- */

    private void refreshStatus() {
        String gameDir = prefs.getGameDir();
        tvGameDir.setText(gameDir.isEmpty() ? getString(R.string.tru_pick_folder_hint) : gameDir);
        tvStatus.setText(TruEngine.getContainerStatus(this, prefs));
    }
}
