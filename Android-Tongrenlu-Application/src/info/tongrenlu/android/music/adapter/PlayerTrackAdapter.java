package info.tongrenlu.android.music.adapter;

import info.tongrenlu.android.image.LoadImageTask;
import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.TongrenluApplication;
import info.tongrenlu.android.provider.HttpHelper;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.TrackBean;
import info.tongrenlu.support.ApplicationSupport;

import java.util.LinkedList;
import java.util.List;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PlayerTrackAdapter extends BaseAdapter {

    private List<TrackBean> mPlaylist = new LinkedList<TrackBean>();
    private int mActivePosition = 0;

    @Override
    public int getCount() {
        return this.mPlaylist.size();
    }

    @Override
    public TrackBean getItem(int position) {
        return this.mPlaylist.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Context context = parent.getContext();
        TrackBean trackBean = this.getItem(position);
        View view = convertView;
        if (view == null) {
            view = this.newView(context, trackBean, parent);
        }
        this.bindView(view, context, trackBean);
        return view;
    }

    public View newView(final Context context, TrackBean trackBean, final ViewGroup parent) {
        final View view = View.inflate(context,
                                       R.layout.list_item_player_track,
                                       null);
        final ViewHolder holder = new ViewHolder();
        holder.coverView = (ImageView) view.findViewById(R.id.article_cover);
        holder.titleView = (TextView) view.findViewById(R.id.track_title);
        holder.artistView = (TextView) view.findViewById(R.id.track_artist);
        view.setTag(holder);
        return view;
    }

    public void bindView(final View view, final Context context, final TrackBean trackBean) {
        final ViewHolder holder = (ViewHolder) view.getTag();
        holder.update(context, trackBean);
    }

    public void setPlaylist(List<TrackBean> playlist) {
        this.mPlaylist = playlist;
    }

    public int getActivePosition() {
        return this.mActivePosition;
    }

    public void setActivePosition(int activePosition) {
        this.mActivePosition = activePosition;
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
