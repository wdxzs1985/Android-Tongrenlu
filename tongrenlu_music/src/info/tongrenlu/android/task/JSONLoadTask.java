package info.tongrenlu.android.task;

import info.tongrenlu.android.task.RESTClient.RESTResponse;
import info.tongrenlu.app.CommonConstants;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

public abstract class JSONLoadTask extends
        AsyncTask<Object, Integer, RESTClient.RESTResponse> {

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        this.onStart();
    }

    protected void onStart() {

    }

    @Override
    protected RESTResponse doInBackground(final Object... params) {
        final Uri uri = (Uri) params[CommonConstants.ZERO];
        Bundle parameters = null;
        if (params.length == 2) {
            parameters = (Bundle) params[CommonConstants.ONE];
        }
        return new RESTClient(RESTClient.HTTPVerb.GET, uri, parameters).load();
    }

    @Override
    protected void onPostExecute(final RESTClient.RESTResponse data) {
        super.onPostExecute(data);
        final int code = data.getCode();
        final String json = data.getData();
        if (code == 200 && StringUtils.isNotBlank(json)) {
            try {
                this.processResponseJSON(new JSONObject(json));
            } catch (final JSONException e) {
                this.onJSONException(e);
            }
        } else {
            this.onNetworkError(code);
        }

        this.onFinish();
    }

    protected void processResponseJSON(final JSONObject responseJSON)
            throws JSONException {

    }

    protected void onJSONException(final JSONException e) {
        e.printStackTrace();
    }

    protected void onNetworkError(final int code) {

    }

    protected void onFinish() {
        // TODO Auto-generated method stub

    }

}
