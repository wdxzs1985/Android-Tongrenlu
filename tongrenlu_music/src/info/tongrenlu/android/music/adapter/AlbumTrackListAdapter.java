package info.tongrenlu.android.music.adapter;

import info.tongrenlu.android.music.R;
import info.tongrenlu.domain.TrackBean;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AlbumTrackListAdapter extends CursorAdapter {

    public AlbumTrackListAdapter(final Context context, final Cursor c) {
        super(context, c, true);
    }

    @Override
    public View newView(final Context context, final Cursor c, final ViewGroup viewGroup) {
        final View view = View.inflate(context,
                                       R.layout.list_item_album_track,
                                       null);
        final ViewHolder holder = new ViewHolder();
        holder.titleView = (TextView) view.findViewById(R.id.track_title);
        holder.artistView = (TextView) view.findViewById(R.id.track_artist);
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
        public TrackBean trackBean;

        public void update(final Context context, final TrackBean trackBean) {
            if (trackBean.equals(this.trackBean)) {
                return;
            }
            this.trackBean = trackBean;

            this.titleView.setText(String.format("%s %s",
                                                 trackBean.getSongTitle(),
                                                 trackBean.getLeadArtist()));
            this.artistView.setText(trackBean.getOriginal());
        }
    }
}
