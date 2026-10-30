package com.winlator.tru;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.winlator.R;
import com.winlator.XServerDisplayActivity;
import com.winlator.contentdialog.ContentDialog;
import com.winlator.widget.FrameRating;

public final class TruGameMenuDialog {
    public static void show(final XServerDisplayActivity activity) {
        Context context = activity;
        ContentDialog dialog = new ContentDialog(context);
        dialog.setTitle(context.getString(R.string.tru_subtitle));

        String[] items = {
            context.getString(R.string.tru_menu_back),
            context.getString(R.string.tru_menu_fullscreen),
            context.getString(R.string.tru_menu_controls),
            context.getString(R.string.tru_menu_hud),
            context.getString(R.string.tru_menu_exit_game)
        };

        ListView listView = dialog.findViewById(R.id.ListView);
        listView.setVisibility(View.VISIBLE);
        listView.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, items));
        dialog.findViewById(R.id.LLBottomBar).setVisibility(View.VISIBLE);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            switch (position) {
                case 0:
                    break;
                case 1:
                    activity.getXServerView().getRenderer().toggleFullscreen();
                    break;
                case 2:
                    activity.truOpenControlsDialog();
                    break;
                case 3:
                    showHudPicker(activity);
                    break;
                case 4:
                    activity.truExitGame();
                    break;
            }
        });

        dialog.findViewById(R.id.BTCancel).setVisibility(View.GONE);
        dialog.show();
    }

    private static void showHudPicker(final XServerDisplayActivity activity) {
        int[] values = {
            FrameRating.Mode.DISABLED.ordinal(),
            FrameRating.Mode.SIMPLE.ordinal(),
            FrameRating.Mode.FULL.ordinal()
        };
        String[] labels = {
            activity.getString(R.string.trt_hud_off),
            activity.getString(R.string.trt_hud_simple),
            activity.getString(R.string.trt_hud_full)
        };

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(activity);
        builder.setTitle(R.string.tru_hud_title)
            .setCancelable(true)
            .setSingleChoiceItems(labels, new TruPrefs(activity).getHudMode(), (d, which) -> {
                activity.truSetHudMode(values[which]);
                d.dismiss();
            })
            .show();
    }
}
