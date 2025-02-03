package com.google.android.car.kitchensink;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.car.settings.CarSettings;
import android.content.ContentResolver;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.view.Display;

import java.util.ArrayList;

public final class DisplayLockHelper {
    private static final String TAG = "DisplayLockHelper";
    private static final boolean DEBUG = true;

    private DisplayManager mDisplayManager;

    // Array of display unique ids from the display input lock setting.
    private final ArraySet<String> mDisplayInputLockSetting = new ArraySet<>();

    private final ArrayList<DisplayInputLockItem> mDisplayInputLockItems = new ArrayList<>();

    static class DisplayInputLockItem {
        public final int mDisplayId;
        public boolean mIsLockEnabled;

        DisplayInputLockItem(int displayId, boolean isLockEnabled) {
            mDisplayId = displayId;
            mIsLockEnabled = isLockEnabled;
        }
    }

    public Boolean init(Context context, int displayId) {
        mDisplayManager = context.getSystemService(DisplayManager.class);
        initDisplayInputLockData(context, displayId);
        return mDisplayInputLockSetting.contains(getDisplayUniqueId(displayId));
    }

    public Boolean switchLock(int displayId, Context context) {
        requestDisplayLock(displayId,
                !mDisplayInputLockSetting.contains(getDisplayUniqueId(displayId)), context);
        return mDisplayInputLockSetting.contains(getDisplayUniqueId(displayId));
    }

    public void requestDisplayLock(int displayId, boolean lock,
            Context context) {
        String displayUniqueId = getDisplayUniqueId(displayId);
        Log.d(TAG, "requestUpdateDisplayInputLockSetting, displayUniqueId: " + displayUniqueId
                + "; mDisplayInputLockSetting" + mDisplayInputLockSetting);
        if (mDisplayInputLockSetting.contains(displayUniqueId) == lock) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "requestUpdateDisplayInputLockSetting: displayId=" + displayId
                    + ", lock=" + lock);
        }
        if (lock) {
            mDisplayInputLockSetting.add(displayUniqueId);
        } else {
            mDisplayInputLockSetting.remove(displayUniqueId);
        }
        mDisplayInputLockItems.stream().filter(item -> item.mDisplayId == displayId)
                .findAny().ifPresent(item -> item.mIsLockEnabled = lock);
        writeDisplayInputLockSetting(context.getContentResolver(),
                CarSettings.Global.DISPLAY_INPUT_LOCK,
                makeDisplayInputLockSetting(mDisplayInputLockSetting));
    }

    @Nullable
    private String getDisplayInputLockSetting(@NonNull ContentResolver resolver) {
        return Settings.Global.getString(resolver,
                CarSettings.Global.DISPLAY_INPUT_LOCK);
    }

    private void writeDisplayInputLockSetting(@NonNull ContentResolver resolver,
            @NonNull String settingKey, @NonNull String value) {
        Settings.Global.putString(resolver, settingKey, value);
    }

    private String makeDisplayInputLockSetting(@Nullable ArraySet<String> inputLockSetting) {
        if (inputLockSetting == null) {
            return "";
        }

        String settingValue = TextUtils.join(",", inputLockSetting);
        if (DEBUG) {
            Log.d(TAG, "makeDisplayInputLockSetting(): add new input lock setting: "
                    + settingValue);
        }
        return settingValue;
    }

    private String getDisplayUniqueId(int displayId) {
        Display display = mDisplayManager.getDisplay(displayId);
        if (display == null) {
            return "";
        }
        return display.getUniqueId();
    }

    private int findDisplayIdByUniqueId(@NonNull String displayUniqueId, Display[] displays) {
        for (int i = 0; i < displays.length; i++) {
            Display display = displays[i];
            if (displayUniqueId.equals(display.getUniqueId())) {
                return display.getDisplayId();
            }
        }
        return Display.INVALID_DISPLAY;
    }

    private void parseDisplayInputLockSettingValue(@NonNull String settingKey,
            @Nullable String value) {
        mDisplayInputLockSetting.clear();
        if (value == null || value.isEmpty()) {
            return;
        }
        Display[] displays = mDisplayManager.getDisplays();
        String[] entries = value.split(",");
        for (String uniqueId : entries) {
            if (findDisplayIdByUniqueId(uniqueId, displays) == Display.INVALID_DISPLAY) {
                Log.w(TAG, "Invalid display id: " + uniqueId);
                continue;
            }
            mDisplayInputLockSetting.add(uniqueId);
        }
    }

    private void initDisplayInputLockData(Context context, int displayId) {
        // Read a setting value from the global setting of rear seat input lock.
        String settingValue = getDisplayInputLockSetting(context.getContentResolver());
        parseDisplayInputLockSettingValue(CarSettings.Global.DISPLAY_INPUT_LOCK, settingValue);
        // Make input lock list items for passenger displays with the setting value.
        mDisplayInputLockItems.clear();
        mDisplayInputLockItems.add(new DisplayInputLockItem(displayId,
                mDisplayInputLockSetting.contains(getDisplayUniqueId(displayId))));
    }
}
