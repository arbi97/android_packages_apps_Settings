/*
 *  Copyright (C) 2014 The OmniROM Project
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

package com.android.settings.merk.brightness;

import com.android.settings.SettingsPreferenceFragment;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.IPowerManager;
import android.os.ServiceManager;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.ListPreference;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceCategory;
import android.provider.Settings;
import android.view.View;
import android.util.Log;
import android.app.AlertDialog;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.settings.merk.R;
import com.android.settings.merk.chameleonos.SeekBarPreference;

public class ButtonBrightnessSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "ButtonBrightnessSettings";

    private static final String KEY_BUTTON_NO_BRIGHTNESS = "button_no_brightness";
    private static final String KEY_BUTTON_LINK_BRIGHTNESS = "button_link_brightness";
    private static final String KEY_BUTTON_AUTO_BRIGHTNESS = "button_auto_brightness";
    private static final String KEY_BUTTON_MANUAL_BRIGHTNESS = "button_manual_brightness";
    private static final String KEY_BUTTON_TIMEOUT = "button_timeout";

    private CheckBoxPreference mNoButtonBrightness;
    private CheckBoxPreference mLinkButtonBrightness;
    private Preference mAutomaticButtonBrightness;
    private Preference mManualButtonBrightness;
    private AutoBrightnessDialog mButtonBrightnessDialog;
    private ManualButtonBrightnessDialog mManualBrightnessDialog;
    private IPowerManager mPowerService;
    private SeekBarPreference mButtonTimoutBar;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.button_brightness_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        mNoButtonBrightness = (CheckBoxPreference) findPreference(KEY_BUTTON_NO_BRIGHTNESS);
        mNoButtonBrightness.setChecked(Settings.System.getInt(resolver,
                Settings.System.CUSTOM_BUTTON_DISABLE_BRIGHTNESS, 0) != 0);

        mLinkButtonBrightness = (CheckBoxPreference) findPreference(KEY_BUTTON_LINK_BRIGHTNESS);
        mLinkButtonBrightness.setChecked(Settings.System.getInt(resolver,
                Settings.System.CUSTOM_BUTTON_USE_SCREEN_BRIGHTNESS, 0) != 0);

        mAutomaticButtonBrightness = (Preference) findPreference(KEY_BUTTON_AUTO_BRIGHTNESS);
        mManualButtonBrightness = (Preference) findPreference(KEY_BUTTON_MANUAL_BRIGHTNESS);

        mButtonTimoutBar = (SeekBarPreference) findPreference(KEY_BUTTON_TIMEOUT);
        int currentTimeout = Settings.System.getInt(resolver,
                        Settings.System.BUTTON_BACKLIGHT_TIMEOUT, 0);
        mButtonTimoutBar.setValue(currentTimeout);
        mButtonTimoutBar.setOnPreferenceChangeListener(this);

        mPowerService = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));

        updateEnablement();
    }

    private void updateEnablement() {
        if (mNoButtonBrightness.isChecked()){
            mLinkButtonBrightness.setEnabled(false);
            mButtonTimoutBar.setEnabled(false);
            mAutomaticButtonBrightness.setEnabled(false);
            mManualButtonBrightness.setEnabled(false);
        } else if (mLinkButtonBrightness.isChecked()){
            mNoButtonBrightness.setEnabled(false);
            mAutomaticButtonBrightness.setEnabled(false);
            mManualButtonBrightness.setEnabled(false);
        } else {
            mNoButtonBrightness.setEnabled(true);
            mLinkButtonBrightness.setEnabled(true);
            mButtonTimoutBar.setEnabled(true);
            mAutomaticButtonBrightness.setEnabled(true);
            mManualButtonBrightness.setEnabled(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mButtonBrightnessDialog != null) {
            mButtonBrightnessDialog.dismiss();
        }
        if (mManualBrightnessDialog != null) {
            mManualBrightnessDialog.dismiss();
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mAutomaticButtonBrightness) {
            showButtonAutoBrightnessDialog();
            return true;
        } else if (preference == mManualButtonBrightness) {
            showButtonManualBrightnessDialog();
            return true;
        } else if (preference == mNoButtonBrightness) {
            boolean checked = ((CheckBoxPreference)preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.CUSTOM_BUTTON_DISABLE_BRIGHTNESS, checked ? 1:0);
            updateEnablement();
            return true;
        } else if (preference == mLinkButtonBrightness) {
            boolean checked = ((CheckBoxPreference)preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.CUSTOM_BUTTON_USE_SCREEN_BRIGHTNESS, checked ? 1:0);
            updateEnablement();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();

        if (preference == mButtonTimoutBar) {
            int buttonTimeout = (Integer) objValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.BUTTON_BACKLIGHT_TIMEOUT, buttonTimeout);
        } else {
            return false;
        }
        return true;
    }

    private void showButtonAutoBrightnessDialog() {
        if (mButtonBrightnessDialog != null && mButtonBrightnessDialog.isShowing()) {
            return;
        }

        mButtonBrightnessDialog = new AutoBrightnessDialog(getActivity(), false);
        mButtonBrightnessDialog.show();
    }

    private void showButtonManualBrightnessDialog() {
        if (mManualBrightnessDialog != null && mManualBrightnessDialog.isShowing()) {
            return;
        }

        mManualBrightnessDialog = new ManualButtonBrightnessDialog(getActivity());
        mManualBrightnessDialog.show();
    }

    private class ManualButtonBrightnessDialog extends AlertDialog implements DialogInterface.OnClickListener {

        private SeekBar mBacklightBar;
        private EditText mBacklightInput;
        private int mCurrentBrightness;
        private boolean mIsDragging = false;

        public ManualButtonBrightnessDialog(Context context) {
            super(context);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            final View v = getLayoutInflater().inflate(R.layout.dialog_manual_brightness, null);
            final Context context = getContext();

            mBacklightBar = (SeekBar) v.findViewById(R.id.backlight);
            mBacklightInput = (EditText) v.findViewById(R.id.backlight_input);

            setTitle(R.string.dialog_manual_brightness_title);
            setCancelable(true);
            setView(v);

            try {
                mCurrentBrightness = mPowerService.getCurrentButtonBrightnessValue();
            } catch(Exception e){
            }

            mBacklightBar.setMax(brightnessToProgress(PowerManager.BRIGHTNESS_ON));
            initListeners();
            init();

            setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.ok), this);
            setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.cancel), this);

            super.onCreate(savedInstanceState);
        }

        private int brightnessToProgress(int brightness) {
            return brightness * 100;
        }

        private int progressToBrightness(int progress) {
            int brightness = progress / 100;
            return brightness;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                try {
                    int newBacklight = Integer.valueOf(mBacklightInput.getText().toString());
                    Settings.System.putInt(getContext().getContentResolver(),
                            Settings.System.CUSTOM_BUTTON_BRIGHTNESS, newBacklight);
                } catch (NumberFormatException e) {
                    Log.d(TAG, "NumberFormatException " + e);
                }
            }
        }

        private void init() {
            int currentValue = Settings.System.getInt(getContext().getContentResolver(),
                            Settings.System.CUSTOM_BUTTON_BRIGHTNESS, 100);

            mBacklightBar.setProgress(brightnessToProgress(currentValue));
            mBacklightInput.setText(String.valueOf(currentValue));
        }

        private void initListeners() {
            mBacklightBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (mIsDragging) {
                        int brightness = progressToBrightness(seekBar.getProgress());
                        mBacklightInput.setText(String.valueOf(brightness));
                        try {
                            mPowerService.setButtonBrightness(brightness);
                        } catch(Exception e){
                        }
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    int brightness = progressToBrightness(seekBar.getProgress());
                    try {
                        mPowerService.setButtonBrightness(brightness);
                    } catch(Exception e){
                    }
                    mIsDragging = true;
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    try {
                        mPowerService.setButtonBrightness(mCurrentBrightness);
                    } catch(Exception e){
                    }
                    mIsDragging = false;
                }
            });

            mBacklightInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
                @Override
                public void afterTextChanged(Editable s) {
                    boolean ok = false;
                    try {
                        int minValue = 0;
                        int maxValue = PowerManager.BRIGHTNESS_ON;
                        int newBrightness = Integer.valueOf(s.toString());

                        if (newBrightness >= minValue && newBrightness <= maxValue) {
                            ok = true;
                            mBacklightBar.setProgress(brightnessToProgress(newBrightness));
                        }
                    } catch (NumberFormatException e) {
                        //ignored, ok is false ayway
                    }

                    Button okButton = mManualBrightnessDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    if (okButton != null) {
                        okButton.setEnabled(ok);
                    }
                }
            });
        }
    }
}

