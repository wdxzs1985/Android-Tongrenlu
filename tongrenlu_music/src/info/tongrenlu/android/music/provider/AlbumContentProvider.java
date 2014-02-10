package info.tongrenlu.android.music.provider;

import info.tongrenlu.android.provider.BaseContentProvider;
import android.net.Uri;

public class AlbumContentProvider extends BaseContentProvider {

    private static final String AUTHORITY = "info.tongrenlu.android.music.album";
    private static final String TABLE = "tb_album";
    public static final Uri URI = Uri.parse("content://" + AUTHORITY);

    public AlbumContentProvider() {
        super(AUTHORITY, TABLE, new TongrenluDBV2());
    }

}
