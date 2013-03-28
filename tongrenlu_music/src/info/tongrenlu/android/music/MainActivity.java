package info.tongrenlu.android.music;

import info.tongrenlu.android.music.fragment.MusicListFragment;
import info.tongrenlu.android.music.fragment.PlaylistTrackListFragment;
import info.tongrenlu.app.CommonConstants;
import info.tongrenlu.domain.ArticleBean;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.widget.Toast;

import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitlePageIndicator.IndicatorStyle;
import com.viewpagerindicator.sample.TitleFragmentAdapter;

public class MainActivity extends BaseActivity implements
        MusicListFragment.OnArticleSelectedListener {

    private long mExitTime = 0;
    protected FragmentPagerAdapter mAdapter;
    protected ViewPager mPager;
    protected TitlePageIndicator mIndicator;

    private Toast mToast = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);

        final FragmentManager fm = this.getSupportFragmentManager();
        final TitleFragmentAdapter adapter = new TitleFragmentAdapter(fm);
        adapter.addItem(new PlaylistTrackListFragment());
        adapter.addItem(new MusicListFragment());
        this.mAdapter = adapter;

        this.mPager = (ViewPager) this.findViewById(R.id.pager);
        this.mPager.setAdapter(this.mAdapter);

        this.mIndicator = (TitlePageIndicator) this.findViewById(R.id.indicator);
        this.mIndicator.setViewPager(this.mPager);
        this.mIndicator.setFooterIndicatorStyle(IndicatorStyle.Underline);
        // this.mIndicator.setOnCenterItemClickListener(this);
    }

    @Override
    public void onArticleSelected(final ArticleBean articleBean) {
        final String articleId = articleBean.getArticleId();
        final String title = articleBean.getTitle();

        final Intent intent = new Intent();
        intent.putExtra("articleId", articleId);
        intent.putExtra("title", title);
        intent.setClass(this, MusicInfoActivity.class);

        this.startActivity(intent);
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
