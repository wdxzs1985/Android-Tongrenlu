package info.tongrenlu.android.task;

import info.tongrenlu.android.music.TongrenluApplication;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

public class RESTClient {

    private static final String TAG = RESTClient.class.getName();

    private static final String ENCODE = "UTF-8";

    public enum HTTPVerb {
        GET, POST, PUT, DELETE
    }

    public static class RESTResponse {
        private String mData;
        private int mCode;

        public RESTResponse() {
        }

        public RESTResponse(final String data, final int code) {
            this.mData = data;
            this.mCode = code;
        }

        public String getData() {
            return this.mData;
        }

        public int getCode() {
            return this.mCode;
        }
    }

    private final HTTPVerb mVerb;
    private final Uri mAction;
    private Bundle mParams;

    public RESTClient(final HTTPVerb verb, final Uri action) {
        this.mVerb = verb;
        this.mAction = action;
    }

    public RESTClient(final HTTPVerb verb, final Uri action, final Bundle params) {
        this.mVerb = verb;
        this.mAction = action;
        this.mParams = params;
    }

    public RESTResponse load() {
        try {
            // At the very least we always need an action.
            if (this.mAction == null) {
                Log.e(RESTClient.TAG,
                      "You did not define an action. REST call canceled.");
                return new RESTResponse(); // We send an empty response back.
                                           // The LoaderCallbacks<RESTResponse>
                                           // implementation will always need to
                                           // check the RESTResponse
                                           // and handle error cases like this.
            }

            // Here we define our base request object which we will
            // send to our REST service via HttpClient.
            HttpRequestBase request = null;

            // Let's build our request based on the HTTP verb we were
            // given.
            switch (this.mVerb) {
            case GET: {
                request = new HttpGet();
                RESTClient.attachUriWithQuery(request,
                                              this.mAction,
                                              this.mParams);
            }
                break;

            case DELETE: {
                request = new HttpDelete();
                RESTClient.attachUriWithQuery(request,
                                              this.mAction,
                                              this.mParams);
            }
                break;

            case POST: {
                request = new HttpPost();
                request.setURI(new URI(this.mAction.toString()));

                // Attach form entity if necessary. Note: some REST APIs
                // require you to POST JSON. This is easy to do, simply use
                // postRequest.setHeader('Content-Type', 'application/json')
                // and StringEntity instead. Same thing for the PUT case
                // below.
                final HttpPost postRequest = (HttpPost) request;

                if (this.mParams != null) {
                    final UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(RESTClient.paramsToList(this.mParams));
                    postRequest.setEntity(formEntity);
                }
            }
                break;

            case PUT: {
                request = new HttpPut();
                request.setURI(new URI(this.mAction.toString()));

                // Attach form entity if necessary.
                final HttpPut putRequest = (HttpPut) request;

                if (this.mParams != null) {
                    final UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(RESTClient.paramsToList(this.mParams));
                    putRequest.setEntity(formEntity);
                }
            }
                break;
            }

            if (request != null) {
                this.initHttpHeader(request);
                // Let's send some useful debug information so we can monitor
                // things
                // in LogCat.
                Log.d(RESTClient.TAG,
                      "Executing request: " + RESTClient.verbToString(this.mVerb)
                              + ": "
                              + this.mAction.toString());

                // Finally, we send our request using HTTP. This is the
                // synchronous
                // long operation that we need to run on this Loader's thread.
                final HttpResponse response = new DefaultHttpClient().execute(request);

                final HttpEntity responseEntity = response.getEntity();
                final StatusLine responseStatus = response.getStatusLine();
                final int statusCode = responseStatus != null ? responseStatus.getStatusCode()
                        : 0;

                // Here we create our response and send it back to the
                // LoaderCallbacks<RESTResponse> implementation.
                final RESTResponse restResponse = new RESTResponse(responseEntity != null ? EntityUtils.toString(responseEntity,
                                                                                                                 RESTClient.ENCODE)
                                                                           : null,
                                                                   statusCode);
                return restResponse;
            }

            // Request was null if we get here, so let's just send our empty
            // RESTResponse like usual.
            return new RESTResponse();
        } catch (final URISyntaxException e) {
            Log.e(RESTClient.TAG,
                  "URI syntax was incorrect. " + RESTClient.verbToString(this.mVerb)
                          + ": "
                          + this.mAction.toString(),
                  e);
            return new RESTResponse();
        } catch (final UnsupportedEncodingException e) {
            Log.e(RESTClient.TAG,
                  "A UrlEncodedFormEntity was created with an unsupported encoding.",
                  e);
            return new RESTResponse();
        } catch (final ClientProtocolException e) {
            Log.e(RESTClient.TAG,
                  "There was a problem when sending the request.",
                  e);
            return new RESTResponse();
        } catch (final IOException e) {
            Log.e(RESTClient.TAG,
                  "There was a problem when sending the request.",
                  e);
            return new RESTResponse();
        }
    }

    private static void attachUriWithQuery(final HttpRequestBase request, Uri uri, final Bundle params) {
        try {
            if (params == null) {
                // No params were given or they have already been
                // attached to the Uri.
                request.setURI(new URI(uri.toString()));
            } else {
                final Uri.Builder uriBuilder = uri.buildUpon();

                // Loop through our params and append them to the Uri.
                for (final BasicNameValuePair param : RESTClient.paramsToList(params)) {
                    uriBuilder.appendQueryParameter(param.getName(),
                                                    param.getValue());
                }

                uri = uriBuilder.build();
                request.setURI(new URI(uri.toString()));
            }
        } catch (final URISyntaxException e) {
            Log.e(RESTClient.TAG, "URI syntax was incorrect: " + uri.toString());
        }
    }

    private static String verbToString(final HTTPVerb verb) {
        switch (verb) {
        case GET:
            return "GET";

        case POST:
            return "POST";

        case PUT:
            return "PUT";

        case DELETE:
            return "DELETE";
        }

        return "";
    }

    private static List<BasicNameValuePair> paramsToList(final Bundle params) {
        final ArrayList<BasicNameValuePair> formList = new ArrayList<BasicNameValuePair>(params.size());

        for (final String key : params.keySet()) {
            final Object value = params.get(key);

            // We can only put Strings in a form entity, so we call the
            // toString()
            // method to enforce. We also probably don't need to check for null
            // here
            // but we do anyway because Bundle.get() can return null.
            if (value != null) {
                formList.add(new BasicNameValuePair(key, value.toString()));
            }
        }

        return formList;
    }

    protected void initHttpHeader(final HttpMessage httpMessage) {
        httpMessage.addHeader("Accept-Charset", "UTF-8;");
        httpMessage.addHeader("Cache-Control", "no-cache");
        httpMessage.addHeader("Connection", "keep-alive");
        httpMessage.addHeader("Pragma", "no-cache");
        httpMessage.addHeader("User-Agent", this.buildUserAgent());
    }

    protected String buildUserAgent() {
        final StringBuilder userAgent = new StringBuilder();
        userAgent.append("Android/").append(Build.VERSION.RELEASE).append(" ");
        userAgent.append("(");
        userAgent.append(Build.MODEL).append(" ");
        userAgent.append("SDK/").append(Build.VERSION.SDK_INT);
        userAgent.append(")").append(" ");
        userAgent.append(TongrenluApplication.VERSION_NAME);
        return userAgent.toString();
    }

}