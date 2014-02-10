package info.tongrenlu.android.music.provider;

import info.tongrenlu.android.provider.BaseContentProvider;
import android.net.Uri;

public class PlaylistContentProvider extends BaseContentProvider {

    private static final String AUTHORITY = "info.tongrenlu.android.music.playlist";
    private static final String TABLE = "tb_playlist";
    public static final Uri URI = Uri.parse("content://" + AUTHORITY);

    public PlaylistContentProvider() {
        super(AUTHORITY, TABLE, new TongrenluDBV2());
    }

}
