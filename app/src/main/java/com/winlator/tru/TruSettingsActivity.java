package com.winlator.tru;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.winlator.R;
import com.winlator.contentdialog.ContentDialog;
import com.winlator.core.AppUtils;
import com.winlator.core.LocaleHelper;

import java.io.File;

/**
 * Окно «Настройки» лаунчера: управление, драйверы, графика, язык.
 */
public class TruSettingsActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LocaleHelper.setSystemLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tru_settings_activity);

        LinearLayout root = findViewById(R.id.LLSettings);

        addSection(root, R.string.tru_set_controls);
        addLink(root, R.string.tru_set_choose_profile, this::showProfileDialog);
        addLink(root, R.string.tru_set_open_editor, () ->
            startActivity(new android.content.Intent(this, TruControlsEditorActivity.class)));
        addLink(root, R.string.tru_set_reset_profile, this::resetProfileToDefault);

        addDivider(root);
        addSection(root, R.string.tru_set_graphics);
        addLink(root, R.string.tru_set_drivers, this::showDriversDialog);
        addLink(root, R.string.tru_set_import_driver, this::showImportDriverDialog);
        addLink(root, R.string.tru_set_resolution, this::showResolutionDialog);
        addLink(root, R.string.tru_set_performance, this::showPerformanceDialog);
        addLink(root, R.string.tru_set_hud, this::showHudDialog);

        addDivider(root);
        addSection(root, R.string.tru_set_misc);
        addLink(root, R.string.tru_set_language, this::showLanguageDialog);
        addLink(root, R.string.tru_set_game_folder, this::showPickFolderDialog);
        addLink(root, R.string.tru_set_about, this::showAboutDialog);

        addDivider(root);
        findViewById(R.id.BTBack).setOnClickListener(v -> finish());
    }

    /* ---------- Управление ---------- */

    private void showProfileDialog() {
        com.winlator.inputcontrols.InputControlsManager manager =
            new com.winlator.inputcontrols.InputControlsManager(this);
        java.util.ArrayList<com.winlator.inputcontrols.ControlsProfile> profiles = manager.getProfiles(true);
        if (profiles.isEmpty()) {
            ContentDialog dialog = new ContentDialog(this);
            dialog.setMessage(R.string.tru_no_profiles);
            dialog.show();
            return;
        }

        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        int checked = 0;
        for (int i = 0; i < profiles.size(); i++) {
            labels.add(profiles.get(i).getName());
            if (profiles.get(i).id == prefs().getControlsProfileId()) checked = i;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.tru_set_choose_profile)
            .setSingleChoiceItems(labels.toArray(new String[0]), checked, (d, which) -> {
                prefs().setControlsProfileId(profiles.get(which).id);
                d.dismiss();
            })
            .show();
    }

    private void resetProfileToDefault() {
        prefs().setControlsProfileId(TruEngine.PROFILE_BATTLE_ID);
        ContentDialog dialog = new ContentDialog(this);
        dialog.setMessage(R.string.tru_profile_reset);
        dialog.show();
    }

    /* ---------- Графика/драйверы ---------- */

    private void showDriversDialog() {
        TruPrefs p = prefs();
        final String[] vulkanLabels = {
            getString(R.string.trt_driver_turnip),
            getString(R.string.trt_driver_vortek)
        };
        final String[] vulkanValues = {
            com.winlator.container.GraphicsDrivers.TURNIP,
            com.winlator.container.GraphicsDrivers.VORTEK
        };
        final String[] openglLabels = {
            getString(R.string.trt_driver_zink),
            getString(R.string.trt_driver_virgl),
            getString(R.string.trt_driver_gladio)
        };
        final String[] openglValues = {
            com.winlator.container.GraphicsDrivers.ZINK,
            com.winlator.container.GraphicsDrivers.VIRGL,
            com.winlator.container.GraphicsDrivers.GLADIO
        };

        int vkChecked0 = 0, glChecked0 = openglValues.length - 1;
        for (int i = 0; i < vulkanValues.length; i++) if (vulkanValues[i].equals(p.getVulkanDriver())) vkChecked0 = i;
        for (int i = 0; i < openglValues.length; i++) if (openglValues[i].equals(p.getOpenGLDriver())) glChecked0 = i;
        final int vkChecked = vkChecked0, glChecked = glChecked0;

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.tru_set_drivers)
            .setMessage("Vulkan: " + vulkanLabels[vkChecked] + "  |  OpenGL: " + openglLabels[glChecked] + "\nВыберите Vulkan-драйвер:")
            .setSingleChoiceItems(vulkanLabels, vkChecked, (d, which) -> {
                p.setVulkanDriver(vulkanValues[which]);
                d.dismiss();
                showOpenGLDialog(vulkanLabels[which], openglLabels, openglValues, glChecked);
            })
            .setPositiveButton(R.string.tru_done, null)
            .show();
    }

    private void showOpenGLDialog(String vulkanName, String[] openglLabels, String[] openglValues, int checked) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.tru_set_drivers)
            .setMessage("Выбран Vulkan: " + vulkanName + "\nВыберите OpenGL-драйвер:")
            .setSingleChoiceItems(openglLabels, checked, (d, which) -> {
                prefs().setOpenGLDriver(openglValues[which]);
                d.dismiss();
                AppUtils.showToast(this, vulkanName + " + " + openglLabels[which]);
            })
            .setPositiveButton(R.string.tru_done, null)
            .show();
    }

    private void showImportDriverDialog() {
        ContentDialog dialog = new ContentDialog(this);
        dialog.setTitle(getString(R.string.tru_set_import_driver));
        dialog.setMessage(getString(R.string.tru_import_driver_text));
        dialog.show();
    }

    private void showResolutionDialog() {
        String[] values = TruPrefs.resolutions();
        String[] labels = {
            getString(R.string.trt_res_720p),
            getString(R.string.trt_res_1080p),
            getString(R.string.trt_res_1440p)
        };
        int checked = 0;
        for (int i = 0; i < values.length; i++) if (values[i].equals(prefs().getResolution())) checked = i;

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.tru_resolution_title)
            .setSingleChoiceItems(labels, checked, (d, which) -> {
                prefs().setResolution(values[which]);
                d.dismiss();
            })
            .show();
    }

    private void showPerformanceDialog() {
        String[] values = TruPrefs.perfPresets();
        String[] labels = {
            getString(R.string.trt_perf_stability),
            getString(R.string.trt_perf_medium),
            getString(R.string.trt_perf_performance)
        };
        int checked = 0;
        for (int i = 0; i < values.length; i++) if (values[i].equals(prefs().getPerfPreset())) checked = i;

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.tru_perf_title)
            .setSingleChoiceItems(labels, checked, (d, which) -> {
                prefs().setPerfPreset(values[which]);
                d.dismiss();
            })
            .show();
    }

    private void showHudDialog() {
        int[] values = {
            com.winlator.widget.FrameRating.Mode.DISABLED.ordinal(),
            com.winlator.widget.FrameRating.Mode.SIMPLE.ordinal(),
            com.winlator.widget.FrameRating.Mode.FULL.ordinal()
        };
        String[] labels = {
            getString(R.string.trt_hud_off),
            getString(R.string.trt_hud_simple),
            getString(R.string.trt_hud_full)
        };
        int checked = 0;
        for (int i = 0; i < values.length; i++) if (values[i] == prefs().getHudMode()) checked = i;

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.tru_hud_title)
            .setSingleChoiceItems(labels, checked, (d, which) -> {
                prefs().setHudMode(values[which]);
                d.dismiss();
            })
            .show();
    }

    /* ---------- Прочее ---------- */

    private void showLanguageDialog() {
        int index = LocaleHelper.getLocaleIndex(this);
        int checked = index == 2 ? 1 : 0;
        String[] labels = new String[]{"English", "Русский"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.tru_set_language)
            .setSingleChoiceItems(labels, checked, (d, which) -> {
                SharedPreferences.Editor editor =
                    androidx.preference.PreferenceManager.getDefaultSharedPreferences(this).edit();
                editor.putInt("lc_index", which == 1 ? 2 : 0).apply();
                d.dismiss();
                recreate();
            })
            .show();
    }

    private void showPickFolderDialog() {
        final ContentDialog dialog = new ContentDialog(this, R.layout.tru_folder_dialog);
        dialog.setTitle(getString(R.string.tru_set_game_folder));

        final EditText etPath = dialog.findViewById(R.id.ETGamePath);
        etPath.setText(prefs().getGameDir());

        dialog.setOnConfirmCallback(() -> {
            String path = etPath.getText().toString().trim();
            if (path.isEmpty()) return;
            File dir = new File(path);
            if (!dir.isDirectory()) {
                AppUtils.showToast(this, R.string.tru_game_not_found);
                return;
            }
            prefs().setGameDir(dir.getAbsolutePath());
        });

        dialog.show();
    }

    private void showAboutDialog() {
        ContentDialog dialog = new ContentDialog(this);
        dialog.setTitle(getString(R.string.tru_set_about));
        dialog.setMessage(getString(R.string.tru_about_text, AppUtils.getVersionName(this)));
        dialog.show();
    }

    /* ---------- UI helpers ---------- */

    private TruPrefs prefs() { return new TruPrefs(this); }

    private void addSection(LinearLayout root, int resId) {
        android.widget.TextView b = new android.widget.TextView(this);
        b.setText(resId);
        b.setTextColor(getResources().getColor(R.color.tru_gold));
        b.setAllCaps(true);
        b.setTextSize(15f);
        b.setLetterSpacing(0.18f);
        b.setPadding(dp(4), dp(18), dp(4), dp(6));
        root.addView(b);
    }

    private void addLink(LinearLayout root, int resId, Runnable action) {
        Button b = new Button(this, null, 0);
        b.setText(resId);
        b.setBackgroundResource(R.drawable.tru_btn_secondary);
        b.setTextColor(getResources().getColor(R.color.tru_text));
        b.setAllCaps(false);
        b.setTextSize(13f);
        b.setPadding(dp(14), 0, dp(14), 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(46));
        params.topMargin = dp(8);
        b.setLayoutParams(params);
        b.setOnClickListener(v -> action.run());
        root.addView(b);
    }

    private void addDivider(LinearLayout root) {
        android.view.View v = new android.view.View(this);
        v.setBackgroundResource(R.color.tru_gold_dim);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, dp(1)));
        params.topMargin = dp(16);
        v.setLayoutParams(params);
        root.addView(v);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
