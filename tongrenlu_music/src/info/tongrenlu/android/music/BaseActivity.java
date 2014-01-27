package info.tongrenlu.android.music;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class BaseActivity extends FragmentActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // InputStream ins = null;
        // try {
        // ins = this.getAssets().open("background.jpg");
        // final BitmapDrawable drawable = new
        // BitmapDrawable(this.getResources(),
        // ins);
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        // this.findViewById(android.R.id.content).setBackground(drawable);
        // } else {
        // this.findViewById(android.R.id.content)
        // .setBackgroundDrawable(drawable);
        // }
        // } catch (final Exception e) {
        // e.printStackTrace();
        // } finally {
        // IOUtils.closeQuietly(ins);
        // }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle item selection
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
