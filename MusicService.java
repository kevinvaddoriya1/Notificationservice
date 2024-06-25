package com.video.videoplayer.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.google.android.exoplayer2.util.Util;
import com.video.videoplayer.R;
import com.video.videoplayer.ui.activities.PlayerActivity;
import com.video.videoplayer.ui.models.MusicModel;
import com.video.videoplayer.utilities.Constants;

import java.util.ArrayList;

import static com.video.videoplayer.ui.activities.MusicPlayerActivity.playlist;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener {

    IBinder mBinder = new MyBinder();
    MediaPlayer mediaPlayer;
    ArrayList<MusicModel> musicList = new ArrayList<>();
    int position = -1;
    Uri uri;
    MusicActionPlaying musicActionPlaying;
    MediaSessionCompat mediaSessionCompat;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSessionCompat = new MediaSessionCompat(getBaseContext(), "My Audio");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.e("TAG===", "Method onBind...");
        return mBinder;
    }


    public class MyBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int mPosition = intent.getIntExtra("servicePosition", -1);
        String actionName = intent.getStringExtra("ActionName");
        if (mPosition != -1) {
            playMedia(mPosition);
        }
        if (actionName != null) {
            switch (actionName) {
                case "PlayPause":
                    if (musicActionPlaying != null) {
                        musicActionPlaying.playPauseBtnClicked();
                    }
                    break;
                case "next":
                    if (musicActionPlaying != null) {
                        musicActionPlaying.nextBtnClicked();
                    }
                    break;
                case "previous":
                    if (musicActionPlaying != null) {
                        musicActionPlaying.prevBtnClicked();
                    }
                    break;
            }
        }
        return START_STICKY;
    }

    private void playMedia(int StartPosition) {
        musicList = playlist;
        position = StartPosition;
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            if (musicList != null) {
                createMediaPlayer(position);
                mediaPlayer.start();
            }
        } else {
            createMediaPlayer(position);
            mediaPlayer.start();
        }
    }

    public void start() {
        mediaPlayer.start();
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public void stop() {
        mediaPlayer.stop();
    }

    public void pause() {
        mediaPlayer.pause();
    }

    public void release() {
        mediaPlayer.release();
    }

    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    public void seekTo(int position) {
        mediaPlayer.seekTo(position);
    }

    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public void createMediaPlayer(int positionI) {
        position = positionI;
        uri = Uri.parse(musicList.get(position).getPath());
        mediaPlayer = MediaPlayer.create(getBaseContext(), uri);
    }

    public void OnCompleted() {
        mediaPlayer.setOnCompletionListener(this);
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if (musicActionPlaying != null) {
            musicActionPlaying.nextBtnClicked();
            if (mediaPlayer != null) {
                createMediaPlayer(position);
                start();
                OnCompleted();
            }
        }
    }

    public void setCallBack(MusicActionPlaying actionPlaying) {
        this.musicActionPlaying = actionPlaying;
    }

    public void showNotification(int btnPlayPause) {

        int pendingFlags;
        if (Util.SDK_INT >= 23) {
            pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else {
            pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        }


        Intent intent = new Intent(this, PlayerActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Intent prevIntent = new Intent(this, NotificationReceiver.class).setAction(Constants.ACTION_PREVIOUS);
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this, 0, prevIntent, pendingFlags);

        Intent pauseIntent = new Intent(this, NotificationReceiver.class).setAction(Constants.ACTION_PLAY);
        PendingIntent pausePendingIntent = PendingIntent.getBroadcast(this, 0, pauseIntent, pendingFlags);

        Intent nextIntent = new Intent(this, NotificationReceiver.class).setAction(Constants.ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 0, nextIntent, pendingFlags);

        byte[] picture = null;
        picture = getAlbumArt(musicList.get(position).getPath());
        Bitmap thumbnail = null;
        if (picture != null) {
            thumbnail = BitmapFactory.decodeByteArray(picture, 0, picture.length);
        } else {
            thumbnail = BitmapFactory.decodeResource(getResources(), R.drawable.img_music);
        }

        Notification notification = new NotificationCompat.Builder(this, Constants.CHANNEL_ID_2)
                .setSmallIcon(btnPlayPause)
                .setLargeIcon(thumbnail)
                .setContentTitle(musicList.get(position).getName())
                .setContentText(musicList.get(position).getArtist())
                .addAction(R.drawable.ic_previous, "previous", prevPendingIntent)
                .addAction(btnPlayPause, "Pause", pausePendingIntent)
                .addAction(R.drawable.ic_next, "Next", nextPendingIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSessionCompat.getSessionToken()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();

        startForeground(2, notification);

    }

    private byte[] getAlbumArt(String uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri);
        byte[] art = retriever.getEmbeddedPicture();
        return art;
    }
}
