package info.tongrenlu.android.music;

import info.tongrenlu.android.music.fragment.AlbumFragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;

public class SearchActivity extends ActionBarActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_search);

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
        final FragmentManager fragmentManager = this.getSupportFragmentManager();
        final FragmentTransaction transaction = fragmentManager.beginTransaction();

        final Bundle args = new Bundle();
        args.putString("query", query);

        final Fragment fragment = new AlbumFragment();
        fragment.setArguments(args);

        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        this.getMenuInflater().inflate(R.menu.activity_search, menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            final SearchManager searchManager = (SearchManager) this.getSystemService(Context.SEARCH_SERVICE);
            final MenuItem searchItem = menu.findItem(R.id.action_search);
            final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
            searchView.setSearchableInfo(searchManager.getSearchableInfo(this.getComponentName()));
            searchView.setIconifiedByDefault(false);

            final String query = this.getIntent()
                                     .getStringExtra(SearchManager.QUERY);
            searchView.setQuery(query, false);
        }
        return true;
    }
}
