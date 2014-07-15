package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.music.R;
import info.tongrenlu.support.ApplicationSupport;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    public static final String PREF_KEY_SHUFFLE_PLAY = "pref_key_shuffle_play";
    public static final String PREF_KEY_LOOP_PLAY = "pref_key_loop_play";
    public static final String PREF_KEY_HOST_SERVER = "pref_key_host_server";
    public static final String PREF_KEY_FILE_SERVER = "pref_key_file_server";
    public static final String PREF_KEY_BACKGROUND_RENDER = "pref_key_background_render";

    public static final String PREF_DEFAULT_SHUFFLE_PLAY = "0";
    public static final String PREF_DEFAULT_LOOP_PLAY = "0";
    public static final String PREF_DEFAULT_HOST_SERVER = "http://www.tongrenlu.info";
    public static final String PREF_DEFAULT_FILE_SERVER = "http://files.tongrenlu.info";

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
                                          final String key) {
        if (StringUtils.equals(key, PREF_KEY_SHUFFLE_PLAY)) {
            this.initShufflePlayPref(sharedPreferences);
        } else if (StringUtils.equals(key, PREF_KEY_LOOP_PLAY)) {
            this.initLoopPlayPref(sharedPreferences);
        }
    }

    private SharedPreferences sharedPreferences = null;

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        this.initShufflePlayPref(this.sharedPreferences);
        this.initLoopPlayPref(this.sharedPreferences);
        this.initHostServerPref(this.sharedPreferences);
        this.initFileServerPref(this.sharedPreferences);
        this.initBackgroundRenderPref(this.sharedPreferences);
    }

    private void initShufflePlayPref(final SharedPreferences sharedPreferences) {
        final Preference preference = this.findPreference(PREF_KEY_SHUFFLE_PLAY);
        final String value = sharedPreferences.getString(PREF_KEY_SHUFFLE_PLAY,
                                                         PREF_DEFAULT_SHUFFLE_PLAY);

        final Resources res = this.getResources();
        final String[] entries = res.getStringArray(R.array.pref_entries_shuffle_play);
        final String[] entryValues = res.getStringArray(R.array.pref_entry_values_shuffle_play);

        final int index = ArrayUtils.indexOf(entryValues, value);

        preference.setSummary(entries[index]);
    }

    private void initLoopPlayPref(final SharedPreferences sharedPreferences) {
        final Preference preference = this.findPreference(PREF_KEY_LOOP_PLAY);
        final String value = sharedPreferences.getString(PREF_KEY_LOOP_PLAY,
                                                         PREF_DEFAULT_LOOP_PLAY);

        final Resources res = this.getResources();
        final String[] entries = res.getStringArray(R.array.pref_entries_loop_play);
        final String[] entryValues = res.getStringArray(R.array.pref_entry_values_loop_play);

        final int index = ArrayUtils.indexOf(entryValues, value);

        preference.setSummary(entries[index]);
    }

    private void initHostServerPref(final SharedPreferences sharedPreferences) {
        final Preference preference = this.findPreference(PREF_KEY_HOST_SERVER);
        final String value = sharedPreferences.getString(PREF_KEY_HOST_SERVER,
                                                         PREF_DEFAULT_HOST_SERVER);
        preference.setSummary(value);
    }

    private void initFileServerPref(final SharedPreferences sharedPreferences) {
        final Preference preference = this.findPreference(PREF_KEY_FILE_SERVER);
        final String value = sharedPreferences.getString(PREF_KEY_FILE_SERVER,
                                                         PREF_DEFAULT_FILE_SERVER);
        preference.setSummary(value);
    }

    private void initBackgroundRenderPref(final SharedPreferences sharedPreferences) {
        final CheckBoxPreference preference = (CheckBoxPreference) this.findPreference(PREF_KEY_BACKGROUND_RENDER);
        final boolean checked = sharedPreferences.getBoolean(PREF_KEY_BACKGROUND_RENDER,
                                                             ApplicationSupport.canUseRenderScript());
        preference.setChecked(checked);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }
}
