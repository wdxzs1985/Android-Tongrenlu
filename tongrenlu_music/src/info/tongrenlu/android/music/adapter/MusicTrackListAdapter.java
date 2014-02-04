package info.tongrenlu.android.music.adapter;

import info.tongrenlu.android.music.R;
import info.tongrenlu.domain.TrackBean;

import java.util.ArrayList;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MusicTrackListAdapter extends BaseAdapter {

    private Integer page = 0;
    private Integer size = 0;
    private Integer itemCount = 0;
    private ArrayList<TrackBean> items = new ArrayList<TrackBean>();
    private int pagenum = 0;

    private boolean mScrolling = false;

    private OnDownloadClickListener mDownloadListener;

    @Override
    public int getCount() {
        return this.items.size();
    }

    @Override
    public TrackBean getItem(final int position) {
        return this.items.get(position);
    }

    @Override
    public long getItemId(final int position) {
        return Long.parseLong(this.getItem(position).getFileId());
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        View view = convertView;
        final Context context = parent.getContext();
        if (view == null) {
            view = View.inflate(context,
                                R.layout.expandable_list_item_track,
                                null);
        }
        final TrackBean trackBean = this.getItem(position);
        view.setTag(trackBean);

        final TextView titleView = (TextView) view.findViewById(R.id.track_title);
        titleView.setText(trackBean.getTitle());
        final TextView artistView = (TextView) view.findViewById(R.id.track_artist);
        artistView.setText(trackBean.getArtist());

        return view;
    }

    public void addData(final TrackBean trackBean) {
        this.items.add(trackBean);
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

    public ArrayList<TrackBean> getItems() {
        return this.items;
    }

    public void setItems(final ArrayList<TrackBean> items) {
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

    public OnDownloadClickListener getDownloadListener() {
        return this.mDownloadListener;
    }

    public void setDownloadListener(final OnDownloadClickListener mDownloadListener) {
        this.mDownloadListener = mDownloadListener;
    }

    public interface OnDownloadClickListener {
        public void onItemDownloadClick(TrackBean trackBean);
    }
}
