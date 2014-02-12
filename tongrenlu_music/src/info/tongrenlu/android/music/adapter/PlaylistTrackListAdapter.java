package info.tongrenlu.android.music.adapter;

import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.TongrenluApplication;
import info.tongrenlu.android.music.async.LoadImageCacheTask;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class PlaylistTrackListAdapter extends CursorAdapter {

    public PlaylistTrackListAdapter(final Context context, final Cursor c) {
        super(context, c, true);
    }

    @Override
    public View newView(final Context context, final Cursor c, final ViewGroup viewGroup) {
        final View view = View.inflate(context,
                                       R.layout.playlist_track_list_item_track,
                                       null);
        ViewHolder holder = new ViewHolder();
        holder.coverView = (ImageView) view.findViewById(R.id.article_cover);
        holder.titleView = (TextView) view.findViewById(R.id.track_title);
        holder.artistView = (TextView) view.findViewById(R.id.track_artist);
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(final View view, final Context context, final Cursor c) {
        ViewHolder holder = (ViewHolder) view.getTag();
        String articleId = c.getString(c.getColumnIndex("article_id"));
        String fileId = c.getString(c.getColumnIndex("file_id"));
        String title = c.getString(c.getColumnIndex("title"));
        String artist = c.getString(c.getColumnIndex("artist"));
        TrackBean trackBean = new TrackBean();
        trackBean.setArticleId(articleId);
        trackBean.setFileId(fileId);
        trackBean.setTitle(title);
        trackBean.setArtist(artist);
        holder.update(context, trackBean);
    }

    @Override
    public Cursor swapCursor(final Cursor newCursor) {
        if (this.mCursor != null) {
            this.mCursor.close();
        }
        return super.swapCursor(newCursor);
    }

    public class ViewHolder {
        public ImageView coverView;
        public TextView titleView;
        public TextView artistView;
        public TrackBean trackBean;
        public LoadImageCacheTask task;

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public void update(final Context context, final TrackBean trackBean) {
            if (trackBean.equals(this.trackBean)) {
                return;
            } else if (this.task != null) {
                this.task.cancel(true);
            }
            this.trackBean = trackBean;
            this.task = new LoadImageCacheTask() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    ViewHolder.this.titleView.setText(trackBean.getTitle());
                    ViewHolder.this.artistView.setText(trackBean.getArtist());
                    ViewHolder.this.coverView.setImageDrawable(null);
                }

                @Override
                protected void onPostExecute(Drawable result) {
                    super.onPostExecute(result);
                    if (!this.isCancelled()) {
                        Drawable emptyDrawable = new ShapeDrawable();
                        TransitionDrawable fadeInDrawable = new TransitionDrawable(new Drawable[] { emptyDrawable,
                                result });
                        ViewHolder.this.coverView.setImageDrawable(fadeInDrawable);
                        fadeInDrawable.startTransition(500);
                    }
                }
            };
            TongrenluApplication app = (TongrenluApplication) context.getApplicationContext();
            final BitmapLruCache bitmapCache = app.getBitmapCache();
            final String url = HttpConstants.getCoverUrl(app,
                                                         trackBean.getArticleId(),
                                                         HttpConstants.S_COVER);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                this.task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                                            bitmapCache,
                                            url);
            } else {
                this.task.execute(bitmapCache, url);
            }

        }
    }
}
