package com.video.videoplayer.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.video.videoplayer.utilities.Constants;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String actionName = intent.getAction();
        Intent serviceIntent = new Intent(context, MusicService.class);
        if (actionName != null) {
            switch (actionName) {
                case Constants.ACTION_PLAY:
                    serviceIntent.putExtra("ActionName", "PlayPause");
                    context.startService(serviceIntent);
                    break;
                case Constants.ACTION_NEXT:
                    serviceIntent.putExtra("ActionName", "next");
                    context.startService(serviceIntent);
                    break;
                case Constants.ACTION_PREVIOUS:
                    serviceIntent.putExtra("ActionName", "previous");
                    context.startService(serviceIntent);
                    break;
            }
        }
    }
}
