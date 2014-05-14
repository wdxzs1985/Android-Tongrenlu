package info.tongrenlu.android.music.adapter;

import info.tongrenlu.android.music.R;
import info.tongrenlu.domain.TrackBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class AlbumTrackListAdapter extends CursorAdapter {

    public AlbumTrackListAdapter(final Context context) {
        super(context, null, false);
    }

    @Override
    public View newView(final Context context, final Cursor c, final ViewGroup viewGroup) {
        final View view = View.inflate(context,
                                       R.layout.list_item_album_track,
                                       null);
        final ViewHolder holder = new ViewHolder();
        holder.titleView = (TextView) view.findViewById(R.id.track_title);
        holder.artistView = (TextView) view.findViewById(R.id.track_artist);
        holder.originalList = (ListView) view.findViewById(R.id.originalList);
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
        final String original = c.getString(c.getColumnIndex("original"));
        final int trackNumber = c.getInt(c.getColumnIndex("trackNumber"));
        final int downloadFlg = c.getInt(c.getColumnIndex("downloadFlg"));
        final TrackBean trackBean = new TrackBean();
        trackBean.setArticleId(articleId);
        trackBean.setFileId(fileId);
        trackBean.setSongTitle(songTitle);
        trackBean.setLeadArtist(leadArtist);
        trackBean.setOriginal(original);
        trackBean.setTrackNumber(trackNumber);
        trackBean.setDownloadFlg(downloadFlg);
        holder.update(context, trackBean);
    }

    public class ViewHolder {
        public TextView titleView;
        public TextView artistView;
        public ListView originalList;
        public TrackBean trackBean;

        public void update(final Context context, final TrackBean trackBean) {
            if (trackBean.equals(this.trackBean)) {
                return;
            }
            this.trackBean = trackBean;
            this.titleView.setText(String.format("%02d  %s",
                                                 trackBean.getTrackNumber(),
                                                 trackBean.getSongTitle()));

            this.artistView.setText(trackBean.getLeadArtist());

            String[] originals = StringUtils.split(trackBean.getOriginal(),
                                                   System.getProperty("line.separator"));
            List<Map<String, String>> data = new ArrayList<Map<String, String>>();
            for (String original : originals) {
                data.add(Collections.singletonMap("original",
                                                  StringUtils.strip(original)));
            }

            SimpleAdapter adapter = new SimpleAdapter(context,
                                                      data,
                                                      R.layout.list_item_original,
                                                      new String[] { "original" },
                                                      new int[] { android.R.id.text1 });
            this.originalList.setAdapter(adapter);
            this.setListViewHeightBasedOnChildren(this.originalList);
        }

        public void setListViewHeightBasedOnChildren(ListView listView) {
            ListAdapter listAdapter = listView.getAdapter();
            if (listAdapter == null) {
                // pre-condition
                return;
            }

            int totalHeight = 0;
            for (int i = 0; i < listAdapter.getCount(); i++) {
                View listItem = listAdapter.getView(i, null, listView);
                listItem.measure(0, 0);
                totalHeight += listItem.getMeasuredHeight();
            }

            ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
            listView.setLayoutParams(params);
            listView.requestLayout();
        }
    }
}
