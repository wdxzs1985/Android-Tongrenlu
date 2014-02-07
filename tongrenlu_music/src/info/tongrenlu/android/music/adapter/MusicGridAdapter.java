package info.tongrenlu.android.music.adapter;

import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.TongrenluApplication;
import info.tongrenlu.android.music.async.LoadImageCacheTask;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.ArticleBean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MusicGridAdapter extends BaseAdapter {

    private List<ArticleBean> items = new ArrayList<ArticleBean>();

    private boolean mScrolling = false;

    @Override
    public int getCount() {
        return this.items.size();
    }

    @Override
    public ArticleBean getItem(final int position) {
        return this.items.get(position);
    }

    @Override
    public long getItemId(final int position) {
        return Long.parseLong(this.getItem(position).getArticleId());
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        View view = convertView;
        ViewHolder holder = null;
        final Context context = parent.getContext();
        if (view == null) {
            view = View.inflate(context, R.layout.list_item_article, null);
            holder = new ViewHolder();
            holder.coverView = (ImageView) view.findViewById(R.id.article_cover);
            holder.titleView = (TextView) view.findViewById(R.id.article_title);

            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        ArticleBean articleBean = this.getItem(position);
        holder.update(context, articleBean);
        return view;
    }

    public class ViewHolder {
        public ImageView coverView;
        public TextView titleView;
        public ArticleBean articleBean;
        public LoadImageCacheTask task;

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
                        ViewHolder.this.coverView.setImageDrawable(result);
                        fadeInDrawable.startTransition(500);
                    }
                }
            };
            TongrenluApplication app = (TongrenluApplication) context.getApplicationContext();
            final BitmapLruCache bitmapCache = app.getBitmapCache();
            final String url = HttpConstants.getCoverUrl(app,
                                                         articleBean.getArticleId(),
                                                         HttpConstants.S_COVER);
            this.task.executeOnExecutor(Executors.newFixedThreadPool(4),
                                        bitmapCache,
                                        url);
        }
    }

    public void addData(final ArticleBean musicBean) {
        this.items.add(musicBean);
    }

    public List<ArticleBean> getItems() {
        return this.items;
    }

    public void setItems(final List<ArticleBean> items) {
        this.items = items;
    }

    public boolean isScrolling() {
        return this.mScrolling;
    }

    public void setScrolling(final boolean scrolling) {
        this.mScrolling = scrolling;
    }
}
