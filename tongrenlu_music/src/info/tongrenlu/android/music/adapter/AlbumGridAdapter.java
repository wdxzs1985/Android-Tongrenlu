package info.tongrenlu.android.music.adapter;

import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.TongrenluApplication;
import info.tongrenlu.android.music.async.LoadImageTask;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.ArticleBean;
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

public class AlbumGridAdapter extends CursorAdapter {

    public AlbumGridAdapter(final Context context) {
        super(context, null, true);
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
            this.task = new LoadImageTask() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    ViewHolder.this.titleView.setText(articleBean.getTitle());
                    ViewHolder.this.coverView.setImageDrawable(null);
                }

                @Override
                protected Drawable doInBackground(Object... params) {
                    Drawable result = super.doInBackground(params);
                    if (result == null) {
                        result = context.getResources()
                                        .getDrawable(R.drawable.default_180);
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
                        fadeInDrawable.startTransition(500);
                    }
                }
            };
            final TongrenluApplication app = (TongrenluApplication) context.getApplicationContext();
            final BitmapLruCache bitmapCache = app.getBitmapCache();
            final String url = HttpConstants.getCoverUrl(app,
                                                         articleBean.getArticleId(),
                                                         HttpConstants.M_COVER);

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
