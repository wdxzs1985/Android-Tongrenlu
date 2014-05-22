package info.tongrenlu.android.player;

import info.tongrenlu.android.music.MusicService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class IncomingPhoneReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final TelephonyManager telephonymanager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        switch (telephonymanager.getCallState()) {
        case TelephonyManager.CALL_STATE_RINGING:
            context.startService(new Intent(MusicService.ACTION_PAUSE));
            break;
        case TelephonyManager.CALL_STATE_OFFHOOK:
            context.startService(new Intent(MusicService.ACTION_PLAY));
            break;
        default:
            break;
        }
    }

}
