package com.winlator.tru;

import android.view.View;
import android.widget.FrameLayout;

import com.winlator.R;
import com.winlator.XServerDisplayActivity;
import com.winlator.inputcontrols.ControlsProfile;
import com.winlator.inputcontrols.InputControlsManager;
import com.winlator.widget.InputControlsView;

/**
 * Встраивает элементы TRU Launch в игровой экран движка Winlator:
 * кнопка меню + межэкранный HUD управляется уже встроенным FrameRating.
 */
public final class TruIngame {
    private TruIngame() {}

    public static void attachTruOverlay(final XServerDisplayActivity activity) {
        TruPrefs prefs = new TruPrefs(activity);
        if (!prefs.getMenuButtonEnabled()) return;

        FrameLayout rootView = activity.findViewById(R.id.FLXServerDisplay);
        View menuButton = View.inflate(activity, R.layout.tru_ingame_menu_button, null);
        menuButton.setOnClickListener(v -> TruGameMenuDialog.show(activity));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
        int margin = (int) (12 * activity.getResources().getDisplayMetrics().density);
        params.setMargins(0, margin, 0, 0);
        rootView.addView(menuButton, params);
    }

    /** Включить сохранённый профиль управления при старте игры. */
    public static void applyDefaultControls(XServerDisplayActivity activity, InputControlsManager manager) {
        TruPrefs prefs = new TruPrefs(activity);
        int profileId = prefs.getControlsProfileId();
        if (profileId <= 0) return;
        ControlsProfile profile = manager.getProfile(profileId);
        if (profile != null) activity.truShowProfile(profile);
    }
}
