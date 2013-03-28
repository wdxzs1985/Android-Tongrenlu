package info.tongrenlu.android.music;

import info.tongrenlu.android.task.FileDownloadTask;
import info.tongrenlu.android.task.JSONLoadTask;
import info.tongrenlu.app.CommonConstants;
import info.tongrenlu.app.HttpConstants;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.IntentService;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class UpdateService extends IntentService {

    public static final String ACTION_CHECK = "info.tongrenlu.android.UpdateService.ACTION_CHECK";
    public static final String NEW_VERSION_AVAILABLE = "info.tongrenlu.android.UpdateService.NEW_VERSION_AVAILABLE";
    public static final IntentFilter FILTER = new IntentFilter(NEW_VERSION_AVAILABLE);
    public static final UpdateReceiver RECEIVER = new UpdateReceiver();

    public UpdateService() {
        super("UpdateService");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        final String action = intent.getAction();
        if (ACTION_CHECK.equals(action)) {
            this.doCheck();
        } else {
            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            final long nextUpdateDate = sharedPreferences.getLong(SettingsActivity.PREF_KEY_NEXT_UPDATE_DATE,
                                                                  System.currentTimeMillis());
            if (nextUpdateDate < System.currentTimeMillis()) {
                this.doCheck();
            }
        }
    }

    protected void doCheck() {
        final Uri uri = HttpConstants.getVersionInfoUri(this);
        new ApplicationVersionLoadTask().execute(uri);
    }

    private class ApplicationVersionLoadTask extends JSONLoadTask {

        @Override
        protected void processResponseJSON(final JSONObject responseJSON)
                throws JSONException {
            final UpdateService context = UpdateService.this;
            final int versionCode = responseJSON.getInt("versionCode");
            final String versionName = responseJSON.getString("versionName");
            final String whatsnew = responseJSON.getString("whatsnew");
            System.out.println(String.format("versionName: %s, versionCode: %d",
                                             versionName,
                                             versionCode));
            System.out.println(whatsnew);

            if (TongrenluApplication.getApplicationVersionCode() < versionCode) {
                context.showUpdateAlertDialog(versionName, whatsnew);
            } else {
                Toast.makeText(context, R.string.it_is_new, Toast.LENGTH_LONG)
                     .show();
                UpdateService.this.remindMeLater(CommonConstants.WEEK);
            }
        }

        @Override
        protected void onNetworkError(final int code) {
            final UpdateService context = UpdateService.this;
            Toast.makeText(context,
                           context.getText(R.string.err_network),
                           Toast.LENGTH_SHORT).show();
            context.remindMeLater(CommonConstants.HOUR);
        }

        @Override
        protected void onJSONException(final JSONException e) {
            super.onJSONException(e);
            final UpdateService context = UpdateService.this;
            context.remindMeLater(CommonConstants.HOUR);
        }

    }

    protected void showUpdateAlertDialog(final String versionName,
                                         final String whatsnew) {
        final Intent intent = new Intent(NEW_VERSION_AVAILABLE);
        intent.putExtra("versionName", versionName);
        intent.putExtra("whatsnew", whatsnew);
        this.sendBroadcast(intent);
    }

    protected void remindMeLater(final long time) {
        final long nextUpdateDate = System.currentTimeMillis() + time;
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit()
                         .putLong(SettingsActivity.PREF_KEY_NEXT_UPDATE_DATE,
                                  nextUpdateDate)
                         .commit();
    }

    public static class UpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String whatsnew = intent.getStringExtra("whatsnew");
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.dial_info_new_version);
            builder.setMessage(whatsnew);
            builder.setPositiveButton(R.string.action_update_now,
                                      new OnClickListener() {

                                          @Override
                                          public void onClick(final DialogInterface dialog,
                                                              final int which) {
                                              UpdateReceiver.this.doUpdate(context);
                                          }
                                      });
            builder.setNegativeButton(R.string.action_update_later, null);
            builder.create().show();
        }

        protected void doUpdate(final Context context) {
            final String url = HttpConstants.getApkUrl(context);
            final File file = HttpConstants.getApkFile(context);
            new APKDownloadTask(context).execute(url, file);
        }

        private class APKDownloadTask extends FileDownloadTask implements
                OnCancelListener {

            private final Context mContext;
            private final ProgressDialog mProgress;

            public APKDownloadTask(final Context context) {
                this.mContext = context;
                this.mProgress = new ProgressDialog(context);
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                this.mProgress.setTitle(R.string.dial_update);
                this.mProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                this.mProgress.setCancelable(false);
                this.mProgress.setOnCancelListener(this);

                this.mProgress.setProgress(0);
                this.mProgress.setMax(100);

                this.mProgress.show();
            }

            @Override
            protected void onProgressUpdate(final Long... values) {
                super.onProgressUpdate(values);
                final long loaded = values[CommonConstants.ZERO];
                final long total = values[CommonConstants.ONE];
                this.mProgress.setProgress((int) loaded);
                this.mProgress.setMax((int) total);
            }

            @Override
            protected void onPostExecute(final File result) {
                super.onPostExecute(result);
                this.mProgress.dismiss();
                // 通过Intent安装APK文件
                final Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + result.toString()),
                                      "application/vnd.android.package-archive");
                this.mContext.startActivity(intent);
            }

            @Override
            public void onCancel(final DialogInterface dialog) {
                this.cancel(true);
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
            }

            @Override
            protected void onCancelled(final File result) {
                super.onCancelled(result);
            }

        }
    }
}
