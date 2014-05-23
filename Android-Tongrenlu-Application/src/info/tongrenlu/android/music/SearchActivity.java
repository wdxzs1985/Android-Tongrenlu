package info.tongrenlu.android.music;

import info.tongrenlu.android.music.fragment.AlbumFragment;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

public class SearchActivity extends ActionBarActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_fragment_container);

        final ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        // actionBar.setDisplayShowTitleEnabled(false);

        this.handleIntent(this.getIntent());
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        this.setIntent(intent);
        this.handleIntent(intent);
    }

    private void handleIntent(final Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            final String query = intent.getStringExtra(SearchManager.QUERY);
            this.doMySearch(query);
        }
    }

    private void doMySearch(final String query) {
        final Bundle args = new Bundle();
        args.putString(SearchManager.QUERY, query);

        final Fragment fragment = new AlbumFragment();
        fragment.setArguments(args);

        this.getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit();

    }
}
