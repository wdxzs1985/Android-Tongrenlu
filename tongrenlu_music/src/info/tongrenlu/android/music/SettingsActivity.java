package info.tongrenlu.android.music;

import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import info.tongrenlu.app.HttpConstants;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.widget.Toast;

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

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.initShufflePlayPref(sharedPreferences);
        this.initLoopPlayPref(sharedPreferences);
        this.initServerPref(sharedPreferences);

        // this.initSaveToSDCard();
        this.initClearCachePref();
        this.initClearDataPref();
        this.initVersionPref();
        this.initNextUpdateDatePref(sharedPreferences);
        this.initCheckUpdate();
    }

    private void initShufflePlayPref(final SharedPreferences sharedPreferences) {
        final String value = sharedPreferences.getString(SettingsActivity.PREF_KEY_SHUFFLE_PLAY,
                                                         SettingsActivity.PREF_DEFAULT_SHUFFLE_PLAY);
        final Preference preference = this.findPreference(SettingsActivity.PREF_KEY_SHUFFLE_PLAY);

        final Resources res = this.getResources();
        final String[] entries = res.getStringArray(R.array.pref_entries_shuffle_play);
        final String[] entryValues = res.getStringArray(R.array.pref_entry_values_shuffle_play);

        final int index = ArrayUtils.indexOf(entryValues, value);

        preference.setSummary(entries[index]);
    }

    private void initLoopPlayPref(final SharedPreferences sharedPreferences) {
        final String value = sharedPreferences.getString(SettingsActivity.PREF_KEY_LOOP_PLAY,
                                                         SettingsActivity.PREF_DEFAULT_LOOP_PLAY);
        final Preference preference = this.findPreference(SettingsActivity.PREF_KEY_LOOP_PLAY);

        final Resources res = this.getResources();
        final String[] entries = res.getStringArray(R.array.pref_entries_loop_play);
        final String[] entryValues = res.getStringArray(R.array.pref_entry_values_loop_play);

        final int index = ArrayUtils.indexOf(entryValues, value);

        preference.setSummary(entries[index]);
    }

    private void initServerPref(final SharedPreferences sharedPreferences) {
        final String value = sharedPreferences.getString(SettingsActivity.PREF_KEY_SERVER,
                                                         SettingsActivity.PREF_DEFAULT_SERVER);
        final Preference preference = this.findPreference(SettingsActivity.PREF_KEY_SERVER);
        preference.setSummary(value);
    }

    protected void initSaveToSDCard() {
        final Preference preference = this.findPreference(SettingsActivity.PREF_KEY_SAVE_TO_SDCARD);
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder.setTitle(R.string.pref_title_clear_cache);
                builder.setMessage(R.string.pref_summary_clear_cache);
                builder.setPositiveButton(R.string.pref_title_clear_cache,
                                          new OnClickListener() {

                                              @Override
                                              public void onClick(final DialogInterface dialog, final int which) {
                                                  SettingsActivity.this.doSaveToSDCard();
                                              }
                                          });
                builder.setNegativeButton(R.string.action_cancel, null);
                builder.create().show();
                return false;
            }
        });

    }

    protected void doSaveToSDCard() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            final File sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            new Handler().post(new Runnable() {

                @Override
                public void run() {
                    final Context context = SettingsActivity.this;
                    final ProgressDialog dialog = new ProgressDialog(context);
                    dialog.setTitle(R.string.loading);
                    dialog.setCancelable(false);
                    dialog.show();

                    final ContentResolver cr = context.getContentResolver();
                    final Cursor c = cr.query(TongrenluContentProvider.TRACK_URI,
                                              null,
                                              null,
                                              null,
                                              null);
                    if (c.moveToFirst()) {
                        while (!c.isAfterLast()) {
                            final String articleId = c.getString(c.getColumnIndex("article_id"));
                            final String fileId = c.getString(c.getColumnIndex("file_id"));
                            final String title = c.getString(c.getColumnIndex("title"));
                            final String artist = c.getString(c.getColumnIndex("artist"));

                            final File srcFile = HttpConstants.getMp3(context,
                                                                      articleId,
                                                                      fileId);
                            final String destFileName = HttpConstants.getAvaliableFilename(artist + "-"
                                    + title
                                    + ".mp3");
                            final File destFile = new File(sdcard, destFileName);
                            dialog.setMessage("正在复制：" + destFileName);
                            try {
                                FileUtils.copyFile(srcFile, destFile);
                            } catch (final IOException e) {
                                e.printStackTrace();
                                Toast.makeText(context,
                                               destFileName + "复制失败。",
                                               Toast.LENGTH_SHORT).show();
                            }
                            c.moveToNext();
                        }
                    }
                    c.close();
                    Toast.makeText(context, "复制完成。", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
        } else {
            Toast.makeText(this, "没有找到SD卡", Toast.LENGTH_SHORT).show();
        }

    }

    private void initClearCachePref() {
        final Preference preference = this.findPreference(SettingsActivity.PREF_KEY_CLEAR_CACHE);
        // this.refreshSizeOfDirectory(dir, preference);
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder.setTitle(R.string.pref_title_clear_cache);
                builder.setMessage(R.string.pref_summary_clear_cache);
                builder.setPositiveButton(R.string.pref_title_clear_cache,
                                          new OnClickListener() {

                                              @Override
                                              public void onClick(final DialogInterface dialog, final int which) {
                                                  SettingsActivity.this.doClearCache();
                                                  // SettingsActivity.this.refreshSizeOfDirectory(dir,
                                                  // preference);
                                              }
                                          });
                builder.setNegativeButton(R.string.action_cancel, null);
                builder.create().show();
                return false;
            }
        });
    }

    private void doClearCache() {
        HttpConstants.clearCover(this);
        Toast.makeText(this, R.string.clear_complete, Toast.LENGTH_SHORT)
             .show();
    }

    private void refreshSizeOfDirectory(final File dir, final Preference preference) {
        final long size = dir.exists() ? FileUtils.sizeOfDirectory(dir) : 0;
        final String summary = this.getString(R.string.used_size,
                                              FileUtils.byteCountToDisplaySize(size));
        preference.setSummary(summary);
    }

    private void initClearDataPref() {
        final Preference preference = this.findPreference(SettingsActivity.PREF_KEY_CLEAR_DATA);
        // final File dir = HttpConstants.getMp3Dir(this);
        // final long size = dir.exists() ? FileUtils.sizeOfDirectory(dir) : 0;
        // final String summary = this.getString(R.string.used_size,
        // FileUtils.byteCountToDisplaySize(size));
        // preference.setSummary(summary);
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder.setTitle(R.string.pref_title_clear_data);
                builder.setMessage(R.string.pref_summary_clear_data);
                builder.setPositiveButton(R.string.pref_title_clear_data,
                                          new OnClickListener() {

                                              @Override
                                              public void onClick(final DialogInterface dialog, final int which) {
                                                  SettingsActivity.this.doClearData();
                                                  // SettingsActivity.this.refreshSizeOfDirectory(dir,
                                                  // preference);
                                              }
                                          });
                builder.setNegativeButton(R.string.action_cancel, null);
                builder.create().show();
                return false;
            }
        });
    }

    private void doClearData() {
        HttpConstants.clearMp3File(this);
        this.getContentResolver().delete(TongrenluContentProvider.TRACK_URI,
                                         null,
                                         null);
        this.getContentResolver().delete(TongrenluContentProvider.PLAYLIST_URI,
                                         null,
                                         null);
        Toast.makeText(this, R.string.clear_complete, Toast.LENGTH_SHORT)
             .show();
    }

    private void initVersionPref() {
        final Preference preference = this.findPreference(SettingsActivity.PREF_KEY_VERSION_NAME);
        preference.setSummary(TongrenluApplication.VERSION_NAME);
    }

    private void initNextUpdateDatePref(final SharedPreferences sharedPreferences) {
        final Preference preference = this.findPreference(SettingsActivity.PREF_KEY_NEXT_UPDATE_DATE);
        final long nextUpdateDate = sharedPreferences.getLong(SettingsActivity.PREF_KEY_NEXT_UPDATE_DATE,
                                                              System.currentTimeMillis());
        preference.setSummary(DateFormat.format("yyyy/MM/dd", nextUpdateDate));

    }

    private void initCheckUpdate() {
        final Preference preference = this.findPreference(SettingsActivity.PREF_KEY_CHECK_UPDATE);
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final Intent service = new Intent(SettingsActivity.this,
                                                  UpdateService.class);
                service.setAction(UpdateService.ACTION_CHECK);
                SettingsActivity.this.startService(service);
                return false;
            }
        });
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

    // @Override
    // protected void onStart() {
    // super.onStart();
    // this.registerReceiver(UpdateService.RECEIVER, UpdateService.FILTER);
    // }
    //
    // @Override
    // protected void onStop() {
    // super.onStop();
    // this.unregisterReceiver(UpdateService.RECEIVER);
    // }
}
