package com.winlator.tru;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;

import androidx.appcompat.app.AppCompatActivity;

import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.core.AppUtils;
import com.winlator.core.Callback;
import com.winlator.xenvironment.RootFS;
import com.winlator.xenvironment.RootFSInstaller;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Интеграционный слой: установка системных файлов, создание контейнера,
 * настройка и запуск Tomb Raider: Underworld.
 */
public class TruEngine {
    public static final String CONTAINER_NAME = "Tomb Raider Underworld";
    public static final int PROFILE_BATTLE_ID = 100;
    public static final int PROFILE_MINIMAL_ID = 101;
    public static final String EXEC_HINT = "tomb9.exe";
    public static final String GAME_LABEL = "Tomb Raider: Underworld";

    private static final String[] EXE_BLACKLIST = {
        "setup", "unins", "installscript", "updatepatcher", "redistributable",
        "installer", "vcredist", "directx", "dxwebsetup", "config", "crashreport"
    };

    private TruEngine() {}

    public static boolean isRootFsInstalled(Context context) {
        RootFS rootFS = RootFS.find(context);
        return rootFS.isValid() && rootFS.getVersion() >= RootFSInstaller.LATEST_VERSION;
    }

    public static void installRootFs(AppCompatActivity activity, Callback<Boolean> done) {
        RootFSInstaller.installIfNeeded(activity, done);
    }

    public interface ContainerReadyCallback {
        void onReady(Container container, boolean created);
    }

    /** Создаёт (или находит) контейнер игры и применяет к нему текущие настройки. */
    public static void ensureContainer(final AppCompatActivity activity, final ContainerReadyCallback cb) {
        final ContainerManager manager = new ContainerManager(activity);
        final TruPrefs prefs = new TruPrefs(activity);

        Container container = prefs.getContainerId() > 0 ? manager.getContainerById(prefs.getContainerId()) : null;
        if (container == null) {
            for (Container c : manager.getContainers()) {
                if (CONTAINER_NAME.equals(c.getName())) {
                    container = c;
                    break;
                }
            }
        }

        if (container != null) {
            applySettings(activity, container);
            prefs.setContainerId(container.id);
            cb.onReady(container, false);
            return;
        }

        try {
            JSONObject data = new JSONObject();
            data.put("name", CONTAINER_NAME);
            manager.createContainerAsync(data, found -> {
                if (found != null) {
                    applySettings(activity, found);
                    prefs.setContainerId(found.id);
                    cb.onReady(found, true);
                }
                else cb.onReady(null, false);
            });
        }
        catch (Exception e) {
            cb.onReady(null, false);
        }
    }

    /** Проецирует настройки лаунчера на контейнер движка Winlator. */
    public static void applySettings(Context context, Container container) {
        TruPrefs prefs = new TruPrefs(context);
        container.setScreenSize(prefs.getResolution());
        container.setBox64Preset(prefs.getBox64Preset());
        container.setGraphicsDriver(prefs.getVulkanDriver()+","+prefs.getOpenGLDriver());
        container.setDXWrapper(prefs.getDXWrapper());
        container.setHUDMode((byte) prefs.getHudMode());
        container.setDrives(composeDrives(prefs));
        container.saveData();
    }

    public static String composeDrives(TruPrefs prefs) {
        StringBuilder sb = new StringBuilder();
        sb.append("D:").append(AppUtils.DIRECTORY_DOWNLOADS);
        sb.append("E:").append(AppUtils.INTERNAL_STORAGE);
        String gameDir = prefs.getGameDir();
        if (!gameDir.isEmpty()) sb.append("F:").append(gameDir);
        return sb.toString();
    }

    public static File getExecutableFile(TruPrefs prefs) {
        String gameDir = prefs.getGameDir();
        if (gameDir.isEmpty()) return null;
        File dir = new File(gameDir);
        if (!dir.isDirectory()) return null;

        String exeName = prefs.getExeName();
        File direct = new File(dir, exeName);
        if (direct.isFile()) return direct;

        return findGameExecutable(gameDir);
    }

    public static File findGameExecutable(String gameDir) {
        if (gameDir == null || gameDir.isEmpty()) return null;
        File dir = new File(gameDir);
        if (!dir.isDirectory()) return null;

        File hint = new File(dir, EXEC_HINT);
        if (hint.isFile()) return hint;

        File best = null;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName().toLowerCase(Locale.ENGLISH);
                if (!name.endsWith(".exe")) continue;
                if (isSystemInstaller(name)) continue;
                if (best == null) best = file;
            }
        }
        return best;
    }

    public static ArrayList<String> listExecutables(String gameDir) {
        ArrayList<String> list = new ArrayList<>();
        File dir = new File(gameDir);
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName().toLowerCase(Locale.ENGLISH);
                if (name.endsWith(".exe") && !isSystemInstaller(name) && file.isFile()) list.add(file.getName());
            }
        }
        return list;
    }

    public static boolean isSystemInstaller(String name) {
        if (name == null) return true;
        String lowered = name.toLowerCase(Locale.ENGLISH);
        for (String part : EXE_BLACKLIST) if (lowered.contains(part)) return true;
        return false;
    }

    public static void runGame(AppCompatActivity activity, Container container) {
        TruPrefs prefs = new TruPrefs(activity);
        File exeFile = getExecutableFile(prefs);
        if (exeFile == null) {
            AppUtils.showToast(activity, activity.getString(com.winlator.R.string.tru_game_not_found));
            return;
        }
        android.content.Intent intent = new android.content.Intent(activity, com.winlator.XServerDisplayActivity.class);
        intent.putExtra("container_id", container.id);
        intent.putExtra("exec_path", exeFile.getPath());
        activity.startActivity(intent);
    }

    public static String getContainerStatus(Context context, TruPrefs prefs) {
        ContainerManager manager = new ContainerManager(context);
        Container container = prefs.getContainerId() > 0 ? manager.getContainerById(prefs.getContainerId()) : null;
        if (container == null) {
            for (Container c : manager.getContainers()) {
                if (CONTAINER_NAME.equals(c.getName())) {
                    container = c;
                    break;
                }
            }
        }
        if (container == null) return context.getString(com.winlator.R.string.tru_status_no_container);
        File exeFile = getExecutableFile(prefs);
        String exeStatus = exeFile != null && exeFile.isFile()
            ? context.getString(com.winlator.R.string.tru_status_ready)
            : context.getString(com.winlator.R.string.tru_status_no_game);
        String engine = container.getGraphicsDriver();
        return context.getString(com.winlator.R.string.tru_status_complete, engine, exeStatus);
    }

    /** Преобразование дерева SAF-выбора в реальный путь файловой системы. */
    public static String treeUriToPath(Uri treeUri) {
        try {
            String docId = DocumentsContract.getTreeDocumentId(treeUri);
            if (docId == null) return "";
            String[] parts = docId.split(":");
            String sub = parts.length > 1 ? parts[1] : "";
            String base = Environment.getExternalStorageDirectory().getAbsolutePath();
            if (sub.isEmpty()) return base;
            return base + "/" + sub;
        }
        catch (Exception e) {
            return "";
        }
    }
}
