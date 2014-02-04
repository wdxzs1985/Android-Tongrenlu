package info.tongrenlu.android.music.adapter;

import info.tongrenlu.android.music.R;
import info.tongrenlu.domain.ArticleBean;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MusicListAdapter extends BaseAdapter {

    private Integer page = 0;
    private Integer size = 0;
    private Integer itemCount = 0;
    private List<ArticleBean> items = new ArrayList<ArticleBean>();
    private int pagenum = 0;

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
        this.updateItem(holder, articleBean);
        return view;
    }

    private void updateItem(ViewHolder holder, ArticleBean articleBean) {
        if (!articleBean.equals(holder.articleBean)) {
            holder.articleBean = articleBean;
            holder.titleView.setText(articleBean.getTitle());
            holder.coverView.setImageDrawable(null);
        }
    }

    public class ViewHolder {
        public ImageView coverView;
        public TextView titleView;
        public ArticleBean articleBean;
    }

    public void addData(final ArticleBean musicBean) {
        this.items.add(musicBean);
    }

    public Integer getPage() {
        return this.page;
    }

    public void setPage(final Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return this.size;
    }

    public void setSize(final Integer size) {
        this.size = size;
    }

    public Integer getItemCount() {
        return this.itemCount;
    }

    public void setItemCount(final Integer itemCount) {
        this.itemCount = itemCount;
    }

    public List<ArticleBean> getItems() {
        return this.items;
    }

    public void setItems(final List<ArticleBean> items) {
        this.items = items;
    }

    public int getPagenum() {
        return this.pagenum;
    }

    public void setPagenum(final int pagenum) {
        this.pagenum = pagenum;
    }

    public boolean isScrolling() {
        return this.mScrolling;
    }

    public void setScrolling(final boolean scrolling) {
        this.mScrolling = scrolling;
    }
}
