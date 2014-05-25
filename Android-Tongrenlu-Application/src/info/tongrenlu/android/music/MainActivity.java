package info.tongrenlu.android.music;

import info.tongrenlu.android.fragment.TitleFragmentAdapter;
import info.tongrenlu.android.fragment.ZoomOutPageTransformer;
import info.tongrenlu.android.music.fragment.AlbumFragment;
import info.tongrenlu.android.music.fragment.AlbumUpdateFragment;
import info.tongrenlu.android.music.fragment.PlaylistFragment;
import info.tongrenlu.android.music.fragment.TrackFragment;
import info.tongrenlu.app.CommonConstants;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.viewpagerindicator.PageIndicator;

public class MainActivity extends ActionBarActivity {

    public static final int ALBUM_LOADER = 0;
    public static final int PLAYLIST_LOADER = 1;
    public static final int TRACK_LOADER = 2;

    private long mExitTime = 0;
    protected FragmentPagerAdapter mAdapter;
    protected ViewPager mPager;
    protected PageIndicator mIndicator;

    private Toast mToast = null;

    public final static int UPDATE_ALBUM = 1;

    protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case UPDATE_ALBUM:
                MainActivity.this.onUpdateAlbum();
                break;
            }
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);

        // ActionBar actionBar = this.getSupportActionBar();
        // actionBar.setDisplayShowTitleEnabled(false);

        final FragmentManager fm = this.getSupportFragmentManager();
        final TitleFragmentAdapter adapter = new TitleFragmentAdapter(fm);
        adapter.addItem(new AlbumFragment());
        adapter.addItem(new PlaylistFragment());
        adapter.addItem(new TrackFragment());
        this.mAdapter = adapter;

        this.mPager = (ViewPager) this.findViewById(R.id.pager);
        this.mPager.setAdapter(this.mAdapter);
        this.mPager.setPageTransformer(true, new ZoomOutPageTransformer());

        this.mIndicator = (PageIndicator) this.findViewById(R.id.indicator);
        this.mIndicator.setViewPager(this.mPager);
    }

    @Override
    public void onBackPressed() {
        final long now = System.currentTimeMillis();
        final int duration = (int) (CommonConstants.TWO * CommonConstants.SECOND);
        if (this.mToast == null) {
            this.mToast = Toast.makeText(this,
                                         R.string.press_back_hit_1,
                                         duration);
        }
        if (now - this.mExitTime > duration) {
            this.mExitTime = now;
            this.mToast.setText(R.string.press_back_hit_1);
            this.mToast.show();
        } else {
            this.mToast.setText(R.string.press_back_hit_2);
            this.mToast.show();
            this.finish();
        }
    }

    public void dispatchUpdateAlbum() {
        this.mHandler.sendEmptyMessage(MainActivity.UPDATE_ALBUM);
    }

    public void onUpdateAlbum() {
        final AlbumUpdateFragment fragment = new AlbumUpdateFragment();
        fragment.show(this.getSupportFragmentManager(), "update");
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        this.getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_settings:
            this.showSetting();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void showSetting() {
        final Intent intent = new Intent(this, SettingsActivity.class);
        this.startActivity(intent);
    }
}
