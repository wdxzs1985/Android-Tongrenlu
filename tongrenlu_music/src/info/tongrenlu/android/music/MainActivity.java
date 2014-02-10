package info.tongrenlu.android.music;

import info.tongrenlu.android.fragment.TitleFragmentAdapter;
import info.tongrenlu.android.music.fragment.MusicGridFragment;
import info.tongrenlu.android.music.fragment.PlaylistFragment;
import info.tongrenlu.app.CommonConstants;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.widget.Toast;

import com.viewpagerindicator.PageIndicator;

public class MainActivity extends BaseActivity {

    private long mExitTime = 0;
    protected FragmentPagerAdapter mAdapter;
    protected ViewPager mPager;
    protected PageIndicator mIndicator;

    private Toast mToast = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);

        final FragmentManager fm = this.getSupportFragmentManager();
        final TitleFragmentAdapter adapter = new TitleFragmentAdapter(fm);
        adapter.addItem(new PlaylistFragment());
        adapter.addItem(new MusicGridFragment());
        this.mAdapter = adapter;

        this.mPager = (ViewPager) this.findViewById(R.id.pager);
        this.mPager.setAdapter(this.mAdapter);

        this.mIndicator = (PageIndicator) this.findViewById(R.id.indicator);
        this.mIndicator.setViewPager(this.mPager);
        // this.mIndicator.setFooterIndicatorStyle(IndicatorStyle.Underline);
        // this.mIndicator.setOnCenterItemClickListener(this);
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

    @Override
    protected void onStart() {
        super.onStart();
        // this.registerReceiver(UpdateService.RECEIVER, UpdateService.FILTER);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // this.unregisterReceiver(UpdateService.RECEIVER);
    }

}
