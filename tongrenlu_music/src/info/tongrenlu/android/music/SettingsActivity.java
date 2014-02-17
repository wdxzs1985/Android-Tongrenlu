package info.tongrenlu.android.music;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    public static final String PREF_KEY_SHUFFLE_PLAY = "pref_key_shuffle_play";
    public static final String PREF_KEY_LOOP_PLAY = "pref_key_loop_play";
    public static final String PREF_KEY_SERVER = "pref_key_server";
    public static final String PREF_KEY_SAVE_TO_SDCARD = "pref_key_save_to_sdcard";
    public static final String PREF_KEY_CLEAR_CACHE = "pref_key_clear_cache";
    public static final String PREF_KEY_CLEAR_DATA = "pref_key_clear_data";

    public static final String PREF_KEY_VERSION_NAME = "pref_key_version_name";
    public static final String PREF_KEY_NEXT_UPDATE_DATE = "pref_key_next_update_date";
    public static final String PREF_KEY_CHECK_UPDATE = "pref_key_check_update";

    public static final String PREF_DEFAULT_SHUFFLE_PLAY = "0";
    public static final String PREF_DEFAULT_LOOP_PLAY = "0";
    public static final String PREF_DEFAULT_SERVER = "http://www.tongrenlu.info";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(R.xml.settings);

        // final SharedPreferences sharedPreferences =
        // PreferenceManager.getDefaultSharedPreferences(this);
        // this.initShufflePlayPref(sharedPreferences);
        // this.initLoopPlayPref(sharedPreferences);
        // this.initServerPref(sharedPreferences);
        //
        // this.initVersionPref();
        // this.initCheckUpdate();
    }

    private void initShufflePlayPref(final SharedPreferences sharedPreferences) {
        final Preference preference = this.findPreference(SettingsActivity.PREF_KEY_SHUFFLE_PLAY);
        final String value = sharedPreferences.getString(SettingsActivity.PREF_KEY_SHUFFLE_PLAY,
                                                         SettingsActivity.PREF_DEFAULT_SHUFFLE_PLAY);

        final Resources res = this.getResources();
        final String[] entries = res.getStringArray(R.array.pref_entries_shuffle_play);
        final String[] entryValues = res.getStringArray(R.array.pref_entry_values_shuffle_play);

        final int index = ArrayUtils.indexOf(entryValues, value);

        preference.setSummary(entries[index]);
    }

    private void initLoopPlayPref(final SharedPreferences sharedPreferences) {
        final Preference preference = this.findPreference(SettingsActivity.PREF_KEY_LOOP_PLAY);
        final String value = sharedPreferences.getString(SettingsActivity.PREF_KEY_LOOP_PLAY,
                                                         SettingsActivity.PREF_DEFAULT_LOOP_PLAY);

        final Resources res = this.getResources();
        final String[] entries = res.getStringArray(R.array.pref_entries_loop_play);
        final String[] entryValues = res.getStringArray(R.array.pref_entry_values_loop_play);

        final int index = ArrayUtils.indexOf(entryValues, value);

        preference.setSummary(entries[index]);
    }

    private void initServerPref(final SharedPreferences sharedPreferences) {
        final Preference preference = this.findPreference(SettingsActivity.PREF_KEY_SERVER);
        final String value = sharedPreferences.getString(SettingsActivity.PREF_KEY_SERVER,
                                                         SettingsActivity.PREF_DEFAULT_SERVER);
        preference.setSummary(value);
    }

    private void initVersionPref() {
        final Preference preference = this.findPreference(SettingsActivity.PREF_KEY_VERSION_NAME);
        preference.setSummary(TongrenluApplication.VERSION_NAME);
    }

    private void initCheckUpdate() {
        // final Preference preference =
        // this.findPreference(SettingsActivity.PREF_KEY_CHECK_UPDATE);
        // preference.setOnPreferenceClickListener(new
        // OnPreferenceClickListener() {
        //
        // @Override
        // public boolean onPreferenceClick(final Preference preference) {
        // final Intent service = new Intent(SettingsActivity.this,
        // UpdateService.class);
        // service.setAction(UpdateService.ACTION_CHECK);
        // SettingsActivity.this.startService(service);
        // return false;
        // }
        // });
    }

    @Override
    protected void onResume() {
        super.onResume();
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        if (StringUtils.equals(key, SettingsActivity.PREF_KEY_SHUFFLE_PLAY)) {
            this.initShufflePlayPref(sharedPreferences);
        } else if (StringUtils.equals(key, SettingsActivity.PREF_KEY_LOOP_PLAY)) {
            this.initLoopPlayPref(sharedPreferences);
        } else if (StringUtils.equals(key, SettingsActivity.PREF_KEY_SERVER)) {
            this.initServerPref(sharedPreferences);
        }
    }
}
