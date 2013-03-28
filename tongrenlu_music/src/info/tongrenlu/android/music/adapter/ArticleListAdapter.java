package info.tongrenlu.android.music.adapter;

import info.tongrenlu.android.music.R;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.ArticleBean;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ArticleListAdapter extends BaseAdapter {

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
    public View getView(final int position,
                        final View convertView,
                        final ViewGroup parent) {
        View view = convertView;
        final Context context = parent.getContext();
        if (view == null) {
            view = View.inflate(context, R.layout.list_item_article, null);
        }

        final ArticleBean articleBean = this.getItem(position);
        final String articleId = articleBean.getArticleId();
        final String tag = (String) view.getTag();
        if (StringUtils.equals(articleId, tag)) {
            // no change
            // return view;
        }
        view.setTag(articleId);
        final TextView titleView = (TextView) view.findViewById(R.id.article_title);
        titleView.setText(articleBean.getTitle());

        final ImageView coverView = (ImageView) view.findViewById(R.id.article_cover);
        if (this.mScrolling) {
            coverView.setImageResource(R.drawable.default_cover);
        } else {
            HttpConstants.displayLargeCover(coverView, articleId);
        }
        //
        return view;
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
