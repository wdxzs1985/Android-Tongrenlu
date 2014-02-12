package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.fragment.TitleFragment;
import info.tongrenlu.android.music.MusicPlayerActivity;
import info.tongrenlu.android.music.PlaylistTrackActivity;
import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.TrackActivity;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class PlaylistFragment extends TitleFragment implements OnItemClickListener {

    public static final int PLAYLIST_LOADER = 0;

    private View mProgress = null;
    private ListView mList1View = null;
    private ListView mList2View = null;
    private CursorAdapter mAdapter2 = null;

    private ContentObserver contentObserver = null;

    public PlaylistFragment() {
        this.setTitle("播放列表");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final FragmentActivity activity = this.getActivity();

        this.contentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(final boolean selfChange) {
                super.onChange(selfChange);
                activity.getSupportLoaderManager()
                        .getLoader(PLAYLIST_LOADER)
                        .onContentChanged();
            }
        };
        activity.getContentResolver()
                .registerContentObserver(TongrenluContentProvider.PLAYLIST_URI,
                                         true,
                                         this.contentObserver);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        final FragmentActivity activity = this.getActivity();
        activity.getContentResolver()
                .unregisterContentObserver(this.contentObserver);
        activity.getSupportLoaderManager().destroyLoader(PLAYLIST_LOADER);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_playlist,
                                           null,
                                           false);
        View listContainer = view.findViewById(android.R.id.list);
        listContainer.setVisibility(View.VISIBLE);
        //
        this.mList1View = (ListView) listContainer.findViewById(R.id.list1);
        this.mList1View.setOnItemClickListener(this);
        this.mList1View.setVisibility(View.VISIBLE);

        this.mList2View = (ListView) listContainer.findViewById(R.id.list2);
        this.mList2View.setOnItemClickListener(this);
        this.mList2View.setVisibility(View.GONE);

        this.mProgress = view.findViewById(android.R.id.progress);
        this.mProgress.setVisibility(View.GONE);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        List<Map<String, String>> items = new ArrayList<Map<String, String>>();
        items.add(Collections.singletonMap("name", "playing"));
        items.add(Collections.singletonMap("name", "all tracks"));
        SimpleAdapter adapter = new SimpleAdapter(this.getActivity(),
                                                  items,
                                                  android.R.layout.simple_list_item_1,
                                                  new String[] { "name" },
                                                  new int[] { android.R.id.text1 });
        this.mList1View.setAdapter(adapter);

        FragmentActivity activity = this.getActivity();
        this.mAdapter2 = new CursorAdapter(activity, null, true) {

            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                View view = View.inflate(context,
                                         android.R.layout.simple_list_item_1,
                                         null);
                return view;
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                String title = cursor.getString(cursor.getColumnIndex("title"));
                TextView titleView = (TextView) view.findViewById(android.R.id.text1);
                titleView.setText(title);
            }
        };
        this.mList2View.setAdapter(this.mAdapter2);

        activity.getSupportLoaderManager()
                .initLoader(PLAYLIST_LOADER,
                            null,
                            new PlaylistCursorLoaderCallback());

    }

    @Override
    public void onItemClick(final AdapterView<?> listView, final View itemView, final int position, final long itemId) {
        FragmentActivity activity = this.getActivity();
        if (listView == this.mList1View) {
            if (itemId == 0) {
                final Intent intent = new Intent(activity,
                                                 MusicPlayerActivity.class);
                this.startActivity(intent);
            } else if (itemId == 1) {
                final Intent intent = new Intent(activity, TrackActivity.class);
                this.startActivity(intent);
            }
        } else {
            final Intent intent = new Intent(activity,
                                             PlaylistTrackActivity.class);
            intent.putExtra("playlistId", itemId);
            this.startActivity(intent);
        }
    }

    private class PlaylistCursorLoaderCallback implements LoaderCallbacks<Cursor> {

        @Override
        public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
            Context context = PlaylistFragment.this.getActivity();
            return new CursorLoader(context,
                                    TongrenluContentProvider.PLAYLIST_URI,
                                    null,
                                    null,
                                    null,
                                    null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
            PlaylistFragment.this.mAdapter2.swapCursor(c);
            if (c.getCount() == 0) {
                PlaylistFragment.this.mProgress.setVisibility(View.GONE);
                PlaylistFragment.this.mList2View.setVisibility(View.GONE);
            } else {
                PlaylistFragment.this.mProgress.setVisibility(View.GONE);
                PlaylistFragment.this.mList2View.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            PlaylistFragment.this.mAdapter2.swapCursor(null);
        }

    }

}
