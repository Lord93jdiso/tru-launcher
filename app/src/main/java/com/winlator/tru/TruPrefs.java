package com.winlator.tru;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.winlator.box64.Box64Preset;
import com.winlator.container.GraphicsDrivers;
import com.winlator.widget.FrameRating;

/**
 * Единое хранилище настроек TRU Launcher.
 */
public class TruPrefs {
    public static final String KEY_INSTALLED = "tru_installed";
    public static final String KEY_CONTAINER_ID = "tru_container_id";
    public static final String KEY_GAME_DIR = "tru_game_dir";
    public static final String KEY_EXE_NAME = "tru_exe_name";
    public static final String KEY_RESOLUTION = "tru_resolution";
    public static final String KEY_PERF_PRESET = "tru_perf_preset";
    public static final String KEY_HUD_MODE = "tru_hud_mode";
    public static final String KEY_VULKAN_DRIVER = "tru_vulkan_driver";
    public static final String KEY_OPENGL_DRIVER = "tru_opengl_driver";
    public static final String KEY_DXWRAPPER = "tru_dxwrapper";
    public static final String KEY_CONTROLS_PROFILE_ID = "tru_controls_profile_id";
    public static final String KEY_MENU_BUTTON = "tru_menu_button";
    public static final String KEY_FIRST_RUN_DONE = "tru_first_run_done";

    public static final String DEFAULT_RESOLUTION = "1280x720";
    public static final String RES_720 = "1280x720";
    public static final String RES_1080 = "1920x1080";
    public static final String RES_1440 = "2560x1440";

    /** Пресеты производительности: "stability" | "medium" | "performance" */
    public static final String PERF_STABILITY = "stability";
    public static final String PERF_MEDIUM = "medium";
    public static final String PERF_PERFORMANCE = "performance";

    public static final String DEFAULT_PERF_PRESET = PERF_MEDIUM;

    public static final String DXVK = "dxvk";
    public static final String WINED3D = "wined3d";

    private final SharedPreferences prefs;

    public TruPrefs(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean getInstalled() { return prefs.getBoolean(KEY_INSTALLED, false); }
    public void setInstalled(boolean value) { prefs.edit().putBoolean(KEY_INSTALLED, value).apply(); }

    public int getContainerId() { return prefs.getInt(KEY_CONTAINER_ID, 0); }
    public void setContainerId(int id) { prefs.edit().putInt(KEY_CONTAINER_ID, id).apply(); }

    public String getGameDir() { return prefs.getString(KEY_GAME_DIR, ""); }
    public void setGameDir(String dir) { prefs.edit().putString(KEY_GAME_DIR, dir).apply(); }

    public String getExeName() { return prefs.getString(KEY_EXE_NAME, "tomb9.exe"); }
    public void setExeName(String name) { prefs.edit().putString(KEY_EXE_NAME, name).apply(); }

    public String getResolution() { return prefs.getString(KEY_RESOLUTION, DEFAULT_RESOLUTION); }
    public void setResolution(String res) { prefs.edit().putString(KEY_RESOLUTION, res).apply(); }

    public String getPerfPreset() { return prefs.getString(KEY_PERF_PRESET, DEFAULT_PERF_PRESET); }
    public void setPerfPreset(String preset) { prefs.edit().putString(KEY_PERF_PRESET, preset).apply(); }

    /** @see FrameRating.Mode ordinal */
    public int getHudMode() { return prefs.getInt(KEY_HUD_MODE, FrameRating.Mode.SIMPLE.ordinal()); }
    public void setHudMode(int mode) { prefs.edit().putInt(KEY_HUD_MODE, mode).apply(); }

    public String getVulkanDriver() { return prefs.getString(KEY_VULKAN_DRIVER, GraphicsDrivers.TURNIP); }
    public void setVulkanDriver(String driver) { prefs.edit().putString(KEY_VULKAN_DRIVER, driver).apply(); }

    public String getOpenGLDriver() { return prefs.getString(KEY_OPENGL_DRIVER, GraphicsDrivers.GLADIO); }
    public void setOpenGLDriver(String driver) { prefs.edit().putString(KEY_OPENGL_DRIVER, driver).apply(); }

    public String getDXWrapper() { return prefs.getString(KEY_DXWRAPPER, DXVK); }
    public void setDXWrapper(String dxwrapper) { prefs.edit().putString(KEY_DXWRAPPER, dxwrapper).apply(); }

    public int getControlsProfileId() { return prefs.getInt(KEY_CONTROLS_PROFILE_ID, TruEngine.PROFILE_BATTLE_ID); }
    public void setControlsProfileId(int id) { prefs.edit().putInt(KEY_CONTROLS_PROFILE_ID, id).apply(); }

    public boolean getMenuButtonEnabled() { return prefs.getBoolean(KEY_MENU_BUTTON, true); }
    public void setMenuButtonEnabled(boolean enabled) { prefs.edit().putBoolean(KEY_MENU_BUTTON, enabled).apply(); }

    public boolean getFirstRunDone() { return prefs.getBoolean(KEY_FIRST_RUN_DONE, false); }
    public void setFirstRunDone(boolean done) { prefs.edit().putBoolean(KEY_FIRST_RUN_DONE, done).apply(); }

    /** Маппинг пресета TRU -> пресет Box64 движка Winlator. */
    public String getBox64Preset() {
        switch (getPerfPreset()) {
            case PERF_STABILITY: return Box64Preset.STABILITY;
            case PERF_PERFORMANCE: return Box64Preset.PERFORMANCE;
            case PERF_MEDIUM:
            default: return Box64Preset.INTERMEDIATE;
        }
    }

    public static String[] resolutions() { return new String[]{RES_720, RES_1080, RES_1440}; }

    public static String[] perfPresets() { return new String[]{PERF_STABILITY, PERF_MEDIUM, PERF_PERFORMANCE}; }
}
