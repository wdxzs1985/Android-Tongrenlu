package info.tongrenlu.android.music.adapter;

import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.TongrenluApplication;
import info.tongrenlu.android.music.async.LoadImageCacheTask;
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

public class MusicGridAdapter extends CursorAdapter {

    public MusicGridAdapter(Context context, Cursor c) {
        super(context, c, true);
    }

    @Override
    public View newView(Context context, Cursor c, ViewGroup parent) {
        View view = View.inflate(context, R.layout.list_item_article, null);
        ViewHolder holder = new ViewHolder();
        holder.coverView = (ImageView) view.findViewById(R.id.article_cover);
        holder.titleView = (TextView) view.findViewById(R.id.article_title);
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor c) {
        ViewHolder holder = (ViewHolder) view.getTag();
        String articleId = c.getString(c.getColumnIndex("article_id"));
        String title = c.getString(c.getColumnIndex("title"));
        ArticleBean articleBean = new ArticleBean();
        articleBean.setArticleId(articleId);
        articleBean.setTitle(title);
        holder.update(context, articleBean);
    }

    public class ViewHolder {
        public ImageView coverView;
        public TextView titleView;
        public ArticleBean articleBean;
        public LoadImageCacheTask task;

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public void update(final Context context, final ArticleBean articleBean) {
            if (articleBean.equals(this.articleBean)) {
                return;
            } else if (this.task != null) {
                this.task.cancel(true);
            }
            this.articleBean = articleBean;
            this.task = new LoadImageCacheTask() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    ViewHolder.this.titleView.setText(articleBean.getTitle());
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
