package info.tongrenlu.android.music.provider;

import info.tongrenlu.android.provider.BaseContentProvider;
import android.net.Uri;

public class TrackContentProvider extends BaseContentProvider {

    private static final String AUTHORITY = "info.tongrenlu.android.music.track";
    private static final String TABLE = "tb_track";
    public static final Uri URI = Uri.parse("content://" + AUTHORITY);

    public TrackContentProvider() {
        super(AUTHORITY, TABLE, new TongrenluDBV1());
    }

}
