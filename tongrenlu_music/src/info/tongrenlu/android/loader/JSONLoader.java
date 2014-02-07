package info.tongrenlu.android.loader;

import info.tongrenlu.support.RESTClient;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

public abstract class JSONLoader<T> extends BaseLoader<T> {

    private final Uri mUri;

    private final Bundle mParameters;

    public JSONLoader(Context ctx, Uri uri, Bundle parameters) {
        super(ctx);
        this.mUri = uri;
        this.mParameters = parameters;
    }

    protected abstract T parseJSON(final JSONObject jsonData) throws JSONException;

    @Override
    public T loadInBackground() {
        T data = null;
        RESTClient.RESTResponse response = new RESTClient(RESTClient.HTTPVerb.GET,
                                                          this.mUri,
                                                          this.mParameters).load();
        final int code = response.getCode();
        final String json = response.getData();
        if (code == 200 && StringUtils.isNotBlank(json)) {
            try {
                JSONObject jsonData = new JSONObject(json);
                data = this.parseJSON(jsonData);
            } catch (final JSONException e) {
                this.onJSONException(e);
            }
        } else {
            this.onNetworkError(code);
        }
        return data;
    }

    protected void onJSONException(final JSONException e) {
        e.printStackTrace();
    }

    protected void onNetworkError(final int code) {
        System.err.println("Network status code :" + code);
    }
}
