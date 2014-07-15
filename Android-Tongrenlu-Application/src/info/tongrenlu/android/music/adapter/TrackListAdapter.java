package info.tongrenlu.android.music.adapter;

import info.tongrenlu.android.image.LoadImageTask;
import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.TongrenluApplication;
import info.tongrenlu.android.provider.HttpHelper;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.TrackBean;
import info.tongrenlu.support.ApplicationSupport;
import uk.co.senab.bitmapcache.BitmapLruCache;
import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.support.v4.widget.CursorAdapter;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class TrackListAdapter extends CursorAdapter {

    public TrackListAdapter(final Context context) {
        super(context, null, false);
    }

    @Override
    public View newView(final Context context,
                        final Cursor c,
                        final ViewGroup viewGroup) {
        final View view = View.inflate(context, R.layout.list_item_track, null);
        final ViewHolder holder = new ViewHolder();
        holder.coverView = (ImageView) view.findViewById(R.id.article_cover);
        holder.titleView = (TextView) view.findViewById(R.id.track_title);
        holder.artistView = (TextView) view.findViewById(R.id.track_artist);
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(final View view, final Context context, final Cursor c) {
        final ViewHolder holder = (ViewHolder) view.getTag();
        final TrackBean trackBean = new TrackBean();
        trackBean.setArticleId(c.getString(c.getColumnIndex("articleId")));
        trackBean.setFileId(c.getString(c.getColumnIndex("fileId")));
        trackBean.setName(c.getString(c.getColumnIndex("name")));
        trackBean.setArtist(c.getString(c.getColumnIndex("artist")));
        trackBean.setTrackNumber(c.getInt(c.getColumnIndex("trackNumber")));
        holder.update(context, trackBean);
    }

    public class ViewHolder {
        public ImageView coverView;
        public TextView titleView;
        public TextView artistView;
        public TrackBean trackBean;
        public LoadImageTask task;

        @SuppressLint("InlinedApi")
        public void update(final Context context, final TrackBean trackBean) {
            if (trackBean.equals(this.trackBean)) {
                return;
            } else if (this.task != null) {
                this.task.cancel(true);
            }
            this.trackBean = trackBean;

            this.titleView.setText(trackBean.getName());
            this.artistView.setText(trackBean.getArtist());

            this.task = new LoadImageTask() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    ViewHolder.this.coverView.setImageDrawable(null);
                }

                @Override
                protected void onPostExecute(final Drawable result) {
                    super.onPostExecute(result);
                    if (!this.isCancelled() && result != null) {
                        if (ApplicationSupport.canUseLargeHeap()) {
                            final Drawable emptyDrawable = new ShapeDrawable();
                            final TransitionDrawable fadeInDrawable = new TransitionDrawable(new Drawable[] { emptyDrawable,
                                    result });
                            ViewHolder.this.coverView.setImageDrawable(fadeInDrawable);
                            fadeInDrawable.startTransition(LoadImageTask.TIME_SHORT);
                        } else {
                            ViewHolder.this.coverView.setImageDrawable(result);
                        }
                    }
                }
            };
            final TongrenluApplication application = (TongrenluApplication) context.getApplicationContext();
            final BitmapLruCache bitmapCache = application.getBitmapCache();
            final HttpHelper http = application.getHttpHelper();

            String url;
            switch (application.getResources().getDisplayMetrics().densityDpi) {
            case DisplayMetrics.DENSITY_XXXHIGH:
            case DisplayMetrics.DENSITY_XXHIGH:
                url = HttpConstants.getCoverUrl(application,
                                                trackBean.getArticleId(),
                                                HttpConstants.S_COVER);
                break;
            case DisplayMetrics.DENSITY_XHIGH:
            case DisplayMetrics.DENSITY_HIGH:
            case DisplayMetrics.DENSITY_TV:
                url = HttpConstants.getCoverUrl(application,
                                                trackBean.getArticleId(),
                                                HttpConstants.XS_COVER);
                break;
            default:
                url = HttpConstants.getCoverUrl(application,
                                                trackBean.getArticleId(),
                                                HttpConstants.XS_COVER);
                break;
            }

            if (ApplicationSupport.canUseThreadPoolExecutor()) {
                this.task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                                            bitmapCache,
                                            url,
                                            http);
            } else {
                this.task.execute(bitmapCache, url, http);
            }
        }
    }
}
