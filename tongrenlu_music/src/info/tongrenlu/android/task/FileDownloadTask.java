package info.tongrenlu.android.task;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import android.os.AsyncTask;
import android.util.Log;

public class FileDownloadTask extends AsyncTask<Object, Long, File> {

    private static final String TAG = "FileDownloadTask";

    @Override
    protected File doInBackground(final Object... params) {
        final String spec = (String) params[0];
        final File file = (File) params[1];

        InputStream input = null;
        OutputStream output = null;
        try {
            final URL url = new URL(spec);
            final URLConnection connection = url.openConnection();
            connection.setConnectTimeout(60 * 1000);
            connection.setReadTimeout(60 * 1000);
            // connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();
            long loaded = 0;
            final long contentLength = connection.getContentLength();
            // download the file
            input = new BufferedInputStream(url.openStream());
            output = FileUtils.openOutputStream(file);

            final byte data[] = new byte[1024];
            int count;
            while ((count = input.read(data)) != -1) {
                loaded += count;
                // publishing the progress....
                this.onProgressUpdate(loaded, contentLength);
                output.write(data, 0, count);
                output.flush();
                if (this.isCancelled()) {
                    break;
                }
            }
        } catch (final MalformedURLException e) {
            Log.d(FileDownloadTask.TAG, e.getMessage(), e);
            return null;
        } catch (final IOException e) {
            Log.d(FileDownloadTask.TAG, e.getMessage(), e);
            return null;
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }
        return file;
    }

}
