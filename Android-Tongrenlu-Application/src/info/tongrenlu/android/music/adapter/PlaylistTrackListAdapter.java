package info.tongrenlu.android.music.adapter;

import info.tongrenlu.android.image.LoadImageTask;
import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.TongrenluApplication;
import info.tongrenlu.android.provider.HttpHelper;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.TrackBean;
import uk.co.senab.bitmapcache.BitmapLruCache;
import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.widget.CursorAdapter;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class PlaylistTrackListAdapter extends CursorAdapter {

    public PlaylistTrackListAdapter(final Context context) {
        super(context, null, false);
    }

    @Override
    public View newView(final Context context, final Cursor c, final ViewGroup viewGroup) {
        final View view = View.inflate(context,
                                       R.layout.list_item_playlist_track,
                                       null);
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
        final String articleId = c.getString(c.getColumnIndex("articleId"));
        final String fileId = c.getString(c.getColumnIndex("fileId"));
        final String songTitle = c.getString(c.getColumnIndex("songTitle"));
        final String leadArtist = c.getString(c.getColumnIndex("leadArtist"));
        final int trackNumber = c.getInt(c.getColumnIndex("trackNumber"));
        final TrackBean trackBean = new TrackBean();
        trackBean.setArticleId(articleId);
        trackBean.setFileId(fileId);
        trackBean.setSongTitle(songTitle);
        trackBean.setLeadArtist(leadArtist);
        trackBean.setTrackNumber(trackNumber);
        holder.update(context, trackBean);
    }

    public class ViewHolder {
        public ImageView coverView;
        public TextView titleView;
        public TextView artistView;
        public TrackBean trackBean;
        public LoadImageTask task;

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public void update(final Context context, final TrackBean trackBean) {
            if (trackBean.equals(this.trackBean)) {
                return;
            } else if (this.task != null) {
                this.task.cancel(true);
            }
            this.trackBean = trackBean;

            this.titleView.setText(trackBean.getSongTitle());
            this.artistView.setText(trackBean.getLeadArtist());

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
                        final Drawable emptyDrawable = new ShapeDrawable();
                        final TransitionDrawable fadeInDrawable = new TransitionDrawable(new Drawable[] { emptyDrawable,
                                result });
                        ViewHolder.this.coverView.setImageDrawable(fadeInDrawable);
                        fadeInDrawable.startTransition(LoadImageTask.TIME_SHORT);
                    }
                }
            };
            final TongrenluApplication application = (TongrenluApplication) context.getApplicationContext();
            final BitmapLruCache bitmapCache = application.getBitmapCache();
            HttpHelper http = application.getHttpHelper();

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
                                                HttpConstants.XXS_COVER);
                break;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
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
