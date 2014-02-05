package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.fragment.TitleFragment;
import info.tongrenlu.android.music.MusicPlayerActivity;
import info.tongrenlu.android.music.PlaylistTrackListActivity;
import info.tongrenlu.android.music.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class PlaylistFragment extends TitleFragment implements OnItemClickListener {

    private ListView mListView = null;
    private SimpleAdapter mAdapter = null;

    public PlaylistFragment() {
        this.setTitle("播放列表");
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        List<Map<String, String>> items = new ArrayList<Map<String, String>>();
        items.add(Collections.singletonMap("name", "playing"));
        items.add(Collections.singletonMap("name", "all tracks"));
        this.mAdapter = new SimpleAdapter(this.getActivity(),
                                          items,
                                          android.R.layout.simple_list_item_1,
                                          new String[] { "name" },
                                          new int[] { android.R.id.text1 });
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_list_view,
                                           null,
                                           false);
        this.mListView = (ListView) view.findViewById(android.R.id.list);
        //
        this.mListView.setAdapter(this.mAdapter);
        this.mListView.setOnItemClickListener(this);
        this.mListView.setVisibility(View.VISIBLE);

        view.findViewById(android.R.id.progress).setVisibility(View.INVISIBLE);
        view.findViewById(android.R.id.empty).setVisibility(View.INVISIBLE);
        return view;
    }

    @Override
    public void onItemClick(final AdapterView<?> listView, final View itemView, final int position, final long itemId) {
        if (itemId == 0) {
            final Intent activityIntent = new Intent(this.getActivity(),
                                                     MusicPlayerActivity.class);
            this.startActivity(activityIntent);
        } else if (itemId == 1) {
            final Intent activityIntent = new Intent(this.getActivity(),
                                                     PlaylistTrackListActivity.class);
            this.startActivity(activityIntent);
        }
    }

}
