package info.tongrenlu.android.player;

import info.tongrenlu.android.music.MusicService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AudioBecomingNoisyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(MusicService.ACTION_PAUSE));
    }

}
