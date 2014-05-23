package info.tongrenlu.android.music.adapter;

import info.tongrenlu.android.image.LoadImageTask;
import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.TongrenluApplication;
import info.tongrenlu.android.provider.HttpHelper;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.ArticleBean;
import info.tongrenlu.support.ApplicationSupport;
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

public class AlbumGridAdapter extends CursorAdapter {

    public AlbumGridAdapter(final Context context) {
        super(context, null, false);
    }

    @Override
    public View newView(final Context context, final Cursor c, final ViewGroup parent) {
        final View view = View.inflate(context, R.layout.list_item_album, null);
        final ViewHolder holder = new ViewHolder();
        holder.coverView = (ImageView) view.findViewById(R.id.article_cover);
        holder.titleView = (TextView) view.findViewById(R.id.article_title);
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(final View view, final Context context, final Cursor c) {
        final ViewHolder holder = (ViewHolder) view.getTag();
        final String articleId = c.getString(c.getColumnIndex("articleId"));
        final String title = c.getString(c.getColumnIndex("title"));
        final ArticleBean articleBean = new ArticleBean();
        articleBean.setArticleId(articleId);
        articleBean.setTitle(title);
        holder.update(context, articleBean);
    }

    public class ViewHolder {
        public ImageView coverView;
        public TextView titleView;
        public ArticleBean articleBean;
        public LoadImageTask task;

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public void update(final Context context, final ArticleBean articleBean) {
            if (articleBean.equals(this.articleBean)) {
                return;
            } else if (this.task != null) {
                this.task.cancel(true);
            }
            this.articleBean = articleBean;
            this.titleView.setText(articleBean.getTitle());

            this.task = new LoadImageTask() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    ViewHolder.this.coverView.setImageDrawable(null);
                }

                @Override
                protected Drawable doInBackground(final Object... params) {
                    Drawable result = super.doInBackground(params);
                    if (result == null) {
                        result = context.getResources()
                                        .getDrawable(R.drawable.default_cover);
                    }
                    return result;
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
            final HttpHelper http = application.getHttpHelper();

            String url;
            switch (application.getResources().getDisplayMetrics().densityDpi) {
            case DisplayMetrics.DENSITY_XXXHIGH:
            case DisplayMetrics.DENSITY_XXHIGH:
            case DisplayMetrics.DENSITY_XHIGH:
            case DisplayMetrics.DENSITY_HIGH:
            case DisplayMetrics.DENSITY_TV:
                url = HttpConstants.getCoverUrl(application,
                                                articleBean.getArticleId(),
                                                HttpConstants.L_COVER);
                break;
            default:
                url = HttpConstants.getCoverUrl(application,
                                                articleBean.getArticleId(),
                                                HttpConstants.M_COVER);
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
