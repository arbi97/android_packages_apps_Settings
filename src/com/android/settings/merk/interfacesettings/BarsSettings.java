/*
 *  Copyright (C) 2013 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.android.settings.merk.interfacesettings;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.android.internal.util.omni.DeviceUtils;
import com.android.settings.Utils;

public class BarsSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "BarsSettings";

    private static final String STATUS_BAR_BRIGHTNESS_CONTROL = "status_bar_brightness_control";
    private static final String STATUS_BAR_NOTIF_COUNT = "status_bar_notif_count";
    private static final String STATUS_BAR_QS_QUICK_PULLDOWN = "status_bar_qs_quick_pulldown";
    private static final String QUICKSETTINGS_DYNAMIC = "quicksettings_dynamic_row";
    private static final String CATEGORY_NAVBAR = "category_navigation_bar";
    private static final String NETWORK_TRAFFIC_STATE = "network_traffic_state";
    private static final String NETWORK_TRAFFIC_UNIT = "network_traffic_unit";
    private static final String NETWORK_TRAFFIC_PERIOD = "network_traffic_period";
    private static final String STATUS_BAR_NETWORK_ACTIVITY = "status_bar_network_activity";
    private static final String SOFT_BACK_KILL_APP = "soft_back_kill_app";
    private static final String EMULATE_MENU_KEY = "emulate_menu_key";

    private CheckBoxPreference mStatusBarBrightnessControl;
    private CheckBoxPreference mStatusBarNotifCount;
    private CheckBoxPreference mQuickSettingsDynamic;
    private CheckBoxPreference mStatusBarQsPulldown;
    private ListPreference mNetTrafficState;
    private ListPreference mNetTrafficUnit;
    private ListPreference mNetTrafficPeriod;
    private CheckBoxPreference mStatusBarNetworkActivity;
    private CheckBoxPreference mSoftBackKillApp;
    private CheckBoxPreference mEmulateMenuKey;

    private int mNetTrafficVal;
    private int MASK_UP;
    private int MASK_DOWN;
    private int MASK_UNIT;
    private int MASK_PERIOD;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.bars_settings);

        loadResources();

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        mStatusBarBrightnessControl =
                (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_BRIGHTNESS_CONTROL);
        mStatusBarBrightnessControl.setChecked((Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL, 0) == 1));
        mStatusBarBrightnessControl.setOnPreferenceChangeListener(this);

        try {
            if (Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
                    == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                mStatusBarBrightnessControl.setEnabled(false);
                mStatusBarBrightnessControl.setSummary(R.string.status_bar_toggle_info);
            }
        } catch (SettingNotFoundException e) {
        }

        mStatusBarNotifCount = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_NOTIF_COUNT);
        mStatusBarNotifCount.setChecked(Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NOTIF_COUNT, 0) == 1);
        mStatusBarNotifCount.setOnPreferenceChangeListener(this);

	mStatusBarQsPulldown = (CheckBoxPreference) prefset.findPreference(STATUS_BAR_QS_QUICK_PULLDOWN);
	mStatusBarQsPulldown.setChecked(Settings.System.getInt(resolver,
	Settings.System.STATUS_BAR_QS_QUICK_PULLDOWN, 0) == 1);
	mStatusBarQsPulldown.setOnPreferenceChangeListener(this);

        mQuickSettingsDynamic = (CheckBoxPreference) prefSet.findPreference(QUICKSETTINGS_DYNAMIC);
        mQuickSettingsDynamic.setChecked(Settings.System.getInt(resolver,
            Settings.System.QUICK_SETTINGS_TILES_ROW, 1) != 0);
        mQuickSettingsDynamic.setOnPreferenceChangeListener(this);

        mStatusBarNetworkActivity =
                (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_NETWORK_ACTIVITY);
        mStatusBarNetworkActivity.setChecked(Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NETWORK_ACTIVITY, 0) == 1);
        mStatusBarNetworkActivity.setOnPreferenceChangeListener(this);
        mStatusBarNetworkActivity.setOnPreferenceChangeListener(this);

        mNetTrafficState = (ListPreference) prefSet.findPreference(NETWORK_TRAFFIC_STATE);
        mNetTrafficUnit = (ListPreference) prefSet.findPreference(NETWORK_TRAFFIC_UNIT);
        mNetTrafficPeriod = (ListPreference) prefSet.findPreference(NETWORK_TRAFFIC_PERIOD);

        // TrafficStats will return UNSUPPORTED if the device does not support it.
        if (TrafficStats.getTotalTxBytes() != TrafficStats.UNSUPPORTED &&
                TrafficStats.getTotalRxBytes() != TrafficStats.UNSUPPORTED) {
            mNetTrafficVal = Settings.System.getInt(resolver, Settings.System.NETWORK_TRAFFIC_STATE, 0);
            int intIndex = mNetTrafficVal & (MASK_UP + MASK_DOWN);
            intIndex = mNetTrafficState.findIndexOfValue(String.valueOf(intIndex));
            if (intIndex <= 0) {
                mNetTrafficUnit.setEnabled(false);
                mNetTrafficPeriod.setEnabled(false);
            }
            mNetTrafficState.setValueIndex(intIndex >= 0 ? intIndex : 0);
            mNetTrafficState.setSummary(mNetTrafficState.getEntry());
            mNetTrafficState.setOnPreferenceChangeListener(this);

            mNetTrafficUnit.setValueIndex(getBit(mNetTrafficVal, MASK_UNIT) ? 1 : 0);
            mNetTrafficUnit.setSummary(mNetTrafficUnit.getEntry());
            mNetTrafficUnit.setOnPreferenceChangeListener(this);

            intIndex = (mNetTrafficVal & MASK_PERIOD) >>> 16;
            intIndex = mNetTrafficPeriod.findIndexOfValue(String.valueOf(intIndex));
            mNetTrafficPeriod.setValueIndex(intIndex >= 0 ? intIndex : 1);
            mNetTrafficPeriod.setSummary(mNetTrafficPeriod.getEntry());
            mNetTrafficPeriod.setOnPreferenceChangeListener(this);
        } else {
            prefSet.removePreference(findPreference(NETWORK_TRAFFIC_STATE));
            prefSet.removePreference(findPreference(NETWORK_TRAFFIC_UNIT));
            prefSet.removePreference(findPreference(NETWORK_TRAFFIC_PERIOD));
        }

        boolean hasNavBar = DeviceUtils.deviceSupportNavigationBar(getActivity());

        // Hide navigation bar category on devices without navigation bar
        if (!hasNavBar) {
            prefSet.removePreference(findPreference(CATEGORY_NAVBAR));
        } else {
            mSoftBackKillApp = (CheckBoxPreference) prefSet.findPreference(SOFT_BACK_KILL_APP);
            mSoftBackKillApp.setChecked(Settings.System.getInt(resolver,
                    Settings.System.SOFT_BACK_KILL_APP_ENABLE, 0) == 1);
            mSoftBackKillApp.setOnPreferenceChangeListener(this);

            mEmulateMenuKey = (CheckBoxPreference) prefSet.findPreference(EMULATE_MENU_KEY);
            mEmulateMenuKey.setChecked(Settings.System.getInt(resolver,
                    Settings.System.EMULATE_HW_MENU_KEY, 0) == 1);
            mEmulateMenuKey.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        // If we didn't handle it, let preferences handle it.
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mStatusBarBrightnessControl) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver, Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL,
                    value ? 1 : 0);
        } else if (preference == mStatusBarNotifCount) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver, Settings.System.STATUS_BAR_NOTIF_COUNT, value ? 1 : 0);
        } else if (preference == mQuickSettingsDynamic) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver,
                Settings.System.QUICK_SETTINGS_TILES_ROW, value ? 1 : 0);
        } else if (preference == mStatusBarQsPulldown) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver,
		Settings.System.STATUS_BAR_QS_QUICK_PULLDOWN, value ? 1: 0);
        } else if (preference == mStatusBarNetworkActivity) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver, Settings.System.STATUS_BAR_NETWORK_ACTIVITY,
                    value ? 1 : 0);
        } else if (preference == mNetTrafficState) {
            int intState = Integer.valueOf((String)objValue);
            mNetTrafficVal = setBit(mNetTrafficVal, MASK_UP, getBit(intState, MASK_UP));
            mNetTrafficVal = setBit(mNetTrafficVal, MASK_DOWN, getBit(intState, MASK_DOWN));
            Settings.System.putInt(resolver, Settings.System.NETWORK_TRAFFIC_STATE, mNetTrafficVal);
            int index = mNetTrafficState.findIndexOfValue((String) objValue);
            mNetTrafficState.setSummary(mNetTrafficState.getEntries()[index]);
            if (intState == 0) {
                mNetTrafficUnit.setEnabled(false);
                mNetTrafficPeriod.setEnabled(false);
            } else {
                mNetTrafficUnit.setEnabled(true);
                mNetTrafficPeriod.setEnabled(true);
            }
        } else if (preference == mNetTrafficUnit) {
            // 1 = Display as Byte/s; default is bit/s
            mNetTrafficVal = setBit(mNetTrafficVal, MASK_UNIT, ((String)objValue).equals("1"));
            Settings.System.putInt(resolver, Settings.System.NETWORK_TRAFFIC_STATE, mNetTrafficVal);
            int index = mNetTrafficUnit.findIndexOfValue((String) objValue);
            mNetTrafficUnit.setSummary(mNetTrafficUnit.getEntries()[index]);
        } else if (preference == mNetTrafficPeriod) {
            int intState = Integer.valueOf((String)objValue);
            mNetTrafficVal = setBit(mNetTrafficVal, MASK_PERIOD, false) + (intState << 16);
            Settings.System.putInt(resolver, Settings.System.NETWORK_TRAFFIC_STATE, mNetTrafficVal);
            int index = mNetTrafficPeriod.findIndexOfValue((String) objValue);
            mNetTrafficPeriod.setSummary(mNetTrafficPeriod.getEntries()[index]);
        } else if (preference == mSoftBackKillApp) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver,
                Settings.System.SOFT_BACK_KILL_APP_ENABLE, value ? 1 : 0);
        } else if (preference == mEmulateMenuKey) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver,
                Settings.System.EMULATE_HW_MENU_KEY, value ? 1 : 0);
        } else {
            return false;
        }
        return true;
    }

    private void loadResources() {
        Resources resources = getActivity().getResources();
        MASK_UP = resources.getInteger(R.integer.maskUp);
        MASK_DOWN = resources.getInteger(R.integer.maskDown);
        MASK_UNIT = resources.getInteger(R.integer.maskUnit);
        MASK_PERIOD = resources.getInteger(R.integer.maskPeriod);
    }

    // intMask should only have the desired bit(s) set
    private int setBit(int intNumber, int intMask, boolean blnState) {
        if (blnState) {
            return (intNumber | intMask);
        }
        return (intNumber & ~intMask);
    }

    private boolean getBit(int intNumber, int intMask) {
        return (intNumber & intMask) == intMask;
    }
}
