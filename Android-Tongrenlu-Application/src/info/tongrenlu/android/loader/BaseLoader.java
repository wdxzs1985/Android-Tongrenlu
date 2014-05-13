package info.tongrenlu.android.loader;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public abstract class BaseLoader<T> extends AsyncTaskLoader<T> {

    private T mData = null;

    public BaseLoader(Context ctx) {
        // Loaders may be used across multiple Activitys (assuming they aren't
        // bound to the LoaderManager), so NEVER hold a reference to the context
        // directly. Doing so will cause you to leak an entire Activity's
        // context.
        // The superclass constructor will store a reference to the Application
        // Context instead, and can be retrieved with a call to getContext().
        super(ctx);
    }

    @Override
    public void deliverResult(T data) {
        if (this.isReset()) {
            // The Loader has been reset;
            // ignore the result and invalidate the data.
            this.releaseResources(data);
            return;
        }

        // Hold a reference to the old data so it doesn't get garbage collected.
        // We must protect it until the new data has been delivered.
        T oldData = this.mData;
        this.mData = data;

        if (this.isStarted()) {
            // If the Loader is in a started state, deliver the results to the
            // client. The superclass method does this for us.
            super.deliverResult(data);
        }

        // Invalidate the old data as we don't need it any more.
        if (oldData != null && oldData != data) {
            this.releaseResources(oldData);
        }
    }

    @Override
    protected void onStartLoading() {
        if (this.mData != null) {
            // Deliver any previously loaded data immediately.
            this.deliverResult(this.mData);
        }

        if (this.takeContentChanged() || this.mData == null) {
            // When the observer detects a change, it should call
            // onContentChanged()
            // on the Loader, which will cause the next call to
            // takeContentChanged()
            // to return true. If this is ever the case (or if the current data
            // is
            // null), we force a new load.
            this.forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        // The Loader is in a stopped state, so we should attempt to cancel the
        // current load (if there is one).
        this.cancelLoad();

        // Note that we leave the observer as is. Loaders in a stopped state
        // should still monitor the data source for changes so that the Loader
        // will know to force a new load if it is ever started again.
    }

    @Override
    protected void onReset() {
        // Ensure the loader has been stopped.
        this.onStopLoading();

        // At this point we can release the resources associated with 'mData'.
        if (this.mData != null) {
            this.releaseResources(this.mData);
            this.mData = null;
        }

        // The Loader is being reset, so we should stop monitoring for changes.
        // if (mObserver != null) {
        // TODO: unregister the observer
        // mObserver = null;
        // }
    }

    @Override
    public void onCanceled(T data) {
        super.onCanceled(data);
        this.releaseResources(data);
    }

    private void releaseResources(T data) {
        // For a simple List, there is nothing to do. For something like
        // a Cursor, we
        // would close it in this method. All resources associated with
        // the Loader
        // should be released here.
    }
}
