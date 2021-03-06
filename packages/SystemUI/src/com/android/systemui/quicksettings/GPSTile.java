package com.android.systemui.quicksettings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsController;
import com.android.systemui.statusbar.phone.QuickSettingsContainerView;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.LocationController.LocationSettingsChangeCallback;


public class GPSTile extends QuickSettingsTile implements LocationSettingsChangeCallback {

    ContentResolver mContentResolver;
    private LocationController mLocationController;
    private int mCurrentMode;
    private int mLocatorsMode;
    private int mLocatorsIndex;

    private static final int[] LOCATORS = new int[]{
            Settings.Secure.LOCATION_MODE_OFF,
            Settings.Secure.LOCATION_MODE_BATTERY_SAVING,
            Settings.Secure.LOCATION_MODE_SENSORS_ONLY,
            Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
    };

    public GPSTile(Context context, QuickSettingsController qsc, LocationController lc) {
        super(context, qsc);

        mContentResolver = mContext.getContentResolver();
        mLocationController = lc;
        mLocationController.addSettingsChangedCallback(this);

        mOnClick = new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleState();
                updateResources();
            }
        };

        mOnLongClick = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startSettingsActivity(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                return true;
            }
        };
        qsc.registerObservedContent(
                Settings.System.getUriFor(Settings.System.EXPANDED_LOCATION_MODE), this);
    }

    @Override
    void onPostCreate() {
        updateSettings();
        onLocationSettingsChanged(false);
        updateTile();
        super.onPostCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLocationController.removeSettingsChangedCallback(this);
    }

    @Override
    public void updateResources() {
        updateTile();
        updateQuickSettings();
    }

    @Override
    public void onLocationSettingsChanged(boolean locationEnabled) {
        mCurrentMode = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF,
                UserHandle.USER_CURRENT);
        mLocatorsIndex = currentModeToLocatorIndex(mCurrentMode);
        updateResources();
    }

    private void toggleState() {
        if (mLocatorsMode == 0) {
            int newMode;

            switch (mCurrentMode) {
                case Settings.Secure.LOCATION_MODE_BATTERY_SAVING:
                    newMode = Settings.Secure.LOCATION_MODE_HIGH_ACCURACY;
                    break;
                case Settings.Secure.LOCATION_MODE_HIGH_ACCURACY:
                    newMode = Settings.Secure.LOCATION_MODE_BATTERY_SAVING;
                    break;
                case Settings.Secure.LOCATION_MODE_OFF:
                    newMode = Settings.Secure.LOCATION_MODE_SENSORS_ONLY;
                    break;
                case Settings.Secure.LOCATION_MODE_SENSORS_ONLY:
                    newMode = Settings.Secure.LOCATION_MODE_OFF;
                    break;
                default:
                    newMode = Settings.Secure.LOCATION_MODE_OFF;
            }

            Settings.Secure.putIntForUser(mContext.getContentResolver(),
                    Settings.Secure.LOCATION_MODE, newMode, UserHandle.USER_CURRENT);
        } else {
            int locatorIndex = mLocatorsIndex;
            int mask;
            do {
                locatorIndex++;
                if (locatorIndex >= LOCATORS.length) {
                    locatorIndex = 0;
                }
                mask = (int) Math.pow(2, locatorIndex);
            }
            while ((mLocatorsMode & mask) != mask);

            // Set the desired state
            Settings.Secure.putIntForUser(mContext.getContentResolver(),
                    Settings.Secure.LOCATION_MODE, LOCATORS[locatorIndex], UserHandle.USER_CURRENT);
        }
    }


    private int currentModeToLocatorIndex(int mode) {
        int count = LOCATORS.length;
        for (int i = 0; i < count; i++) {
            if (LOCATORS[i] == mode) {
                return i;
            }
        }
        return 0;
    }

    private void updateSettings() {
        mLocatorsMode = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.EXPANDED_LOCATION_MODE, 0, UserHandle.USER_CURRENT);
    }

    @Override
    public void onChangeUri(ContentResolver resolver, Uri uri) {
        updateSettings();
        updateResources();
    }

    private synchronized void updateTile() {
        int textResId;
        switch(mCurrentMode) {
        case Settings.Secure.LOCATION_MODE_SENSORS_ONLY:
            textResId = R.string.location_mode_sensors_only_title;
            mDrawable = R.drawable.ic_qs_location_on;
            break;
        case Settings.Secure.LOCATION_MODE_BATTERY_SAVING:
            textResId = R.string.location_mode_battery_saving_title;
            mDrawable = R.drawable.ic_qs_location_lowpower;
            break;
        case Settings.Secure.LOCATION_MODE_HIGH_ACCURACY:
            textResId = R.string.location_mode_high_accuracy_title;
            mDrawable = R.drawable.ic_qs_location_on;
            break;
        default:
            textResId = R.string.quick_settings_location_off_label;
            mDrawable = R.drawable.ic_qs_location_off;
            break;
        }
        mLabel = mContext.getText(textResId).toString();
    }

}
