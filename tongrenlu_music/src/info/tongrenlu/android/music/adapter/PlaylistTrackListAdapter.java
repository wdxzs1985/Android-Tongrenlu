package info.tongrenlu.android.music.adapter;

import info.tongrenlu.android.music.R;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class PlaylistTrackListAdapter extends CursorAdapter {

    public PlaylistTrackListAdapter(final Context context, final Cursor c) {
        super(context, c, true);
    }

    @Override
    public View newView(final Context context,
                        final Cursor c,
                        final ViewGroup viewGroup) {
        final View view = View.inflate(context, R.layout.list_item_track, null);
        this.bindView(view, context, c);
        return view;
    }

    @Override
    public void bindView(final View view, final Context context, final Cursor c) {
        //
        final String title = c.getString(c.getColumnIndex("title"));
        final TextView titleView = (TextView) view.findViewById(R.id.track_title);
        titleView.setText(title);
        //
        final String artist = c.getString(c.getColumnIndex("artist"));
        final TextView artistView = (TextView) view.findViewById(R.id.track_artist);
        artistView.setText(artist);
    }

    @Override
    public Cursor swapCursor(final Cursor newCursor) {
        if (this.mCursor != null) {
            this.mCursor.close();
        }
        return super.swapCursor(newCursor);
    }
}
