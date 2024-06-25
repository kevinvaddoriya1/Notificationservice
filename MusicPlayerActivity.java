package com.video.videoplayer.ui.activities;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.util.Util;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.video.videoplayer.R;
import com.video.videoplayer.services.MusicActionPlaying;
import com.video.videoplayer.services.MusicService;
import com.video.videoplayer.services.NotificationReceiver;
import com.video.videoplayer.ui.models.MusicModel;
import com.video.videoplayer.utilities.Constants;

import java.util.ArrayList;
import java.util.Random;

public class MusicPlayerActivity extends AppCompatActivity implements MusicActionPlaying, ServiceConnection {

    private String TAG = "MusicPlayerActivity";

    TextView txt_song_name, txt_song_artist, txt_music_duration_played, txt_music_duration_total;
    ImageView img_album_cover, img_music_shuffle, img_music_previous, img_music_next, img_music_repeat, music_back;
    FloatingActionButton fab_music_play_pause;
    SeekBar seekBar;

    int position = -1;
    public static ArrayList<MusicModel> playlist = new ArrayList<>();
    Uri uri;
    Handler handler = new Handler();
    private Thread playThread, previousThread, nextThread;
    private boolean isShuffled = false;
    private boolean isRepeat = false;
    MusicService musicService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.blackTransparent));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        initViews();
        getIntentMethod();

        txt_song_name.setText(playlist.get(position).getName());
        txt_song_artist.setText(playlist.get(position).getArtist());

        setListeners();

        MusicPlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (musicService != null) {
                    int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                    seekBar.setProgress(mCurrentPosition);
                    txt_music_duration_played.setText(formattedTime(mCurrentPosition));
                }
                handler.postDelayed(this, 1000);
            }
        });

        backPressed();

    }

    private void getIntentMethod() {
        Bundle bundle = getIntent().getBundleExtra("music_bundle");
        position = bundle.getInt("position");
        playlist.clear();
        playlist = bundle.getParcelableArrayList("musicArrayList");
        if (playlist != null) {
            fab_music_play_pause.setImageResource(R.drawable.ic_pause);
            uri = Uri.parse(playlist.get(position).getPath());
        }

        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("servicePosition", position);
        startService(intent);


        metaData(uri);
    }

    private void setListeners() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (musicService != null && b) {
                    musicService.seekTo(i * 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        music_back.setOnClickListener(view -> {
            getOnBackPressedDispatcher().onBackPressed();
        });

        img_music_shuffle.setOnClickListener(view -> {
            if (isShuffled) {
                isShuffled = false;
                img_music_shuffle.setImageResource(R.drawable.ic_shuffle_off);
            } else {
                isShuffled = true;
                img_music_shuffle.setImageResource(R.drawable.ic_shuffle_on);
            }
        });

        img_music_repeat.setOnClickListener(view -> {
            if (isRepeat) {
                isRepeat = false;
                img_music_repeat.setImageResource(R.drawable.ic_repeat_off);
            } else {
                isRepeat = true;
                img_music_repeat.setImageResource(R.drawable.ic_repeat_on);
            }
        });
    }

    private void initViews() {
        txt_song_name = findViewById(R.id.txt_song_name);
        txt_song_artist = findViewById(R.id.txt_song_artist);
        txt_music_duration_played = findViewById(R.id.txt_music_duration_played);
        txt_music_duration_total = findViewById(R.id.txt_music_duration_total);
        img_album_cover = findViewById(R.id.img_album_cover);
        img_music_shuffle = findViewById(R.id.img_music_shuffle);
        img_music_previous = findViewById(R.id.img_music_previous);
        img_music_next = findViewById(R.id.img_music_next);
        img_music_repeat = findViewById(R.id.img_music_repeat);
        fab_music_play_pause = findViewById(R.id.fab_music_play_pause);
        music_back = findViewById(R.id.music_back);
        seekBar = findViewById(R.id.seekBar);
    }

    private String formattedTime(int mCurrentPosition) {
        String totalout = "";
        String totalNew = "";
        String seconds = String.valueOf(mCurrentPosition % 60);
        String minutes = String.valueOf(mCurrentPosition / 60);
        totalout = minutes + ":" + seconds;
        totalNew = minutes + ":" + "0" + seconds;
        if (seconds.length() == 1) {
            return totalNew;
        } else {
            return totalout;
        }
    }

    private void metaData(Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri.toString());
        int total_duration = Integer.parseInt(playlist.get(position).getDuration()) / 1000;
        txt_music_duration_total.setText(formattedTime(total_duration));
        byte[] art = retriever.getEmbeddedPicture();
        Bitmap bitmap;
        if (art != null) {
            bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
            imageAnimation(MusicPlayerActivity.this, img_album_cover, bitmap);
        } else {
            Glide.with(getApplicationContext()).asBitmap().load(R.drawable.img_music).into(img_album_cover);
        }
    }

    private void playThreadBtn() {
        playThread = new Thread() {
            @Override
            public void run() {
                super.run();
                fab_music_play_pause.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playPauseBtnClicked();
                    }
                });
            }
        };
        playThread.start();
    }

    public void playPauseBtnClicked() {
        if (musicService.isPlaying()) {
            fab_music_play_pause.setImageResource(R.drawable.ic_play);
            musicService.showNotification(R.drawable.ic_play);
            musicService.pause();
            seekBar.setMax(musicService.getDuration() / 1000);
            MusicPlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
        } else {
            fab_music_play_pause.setImageResource(R.drawable.ic_pause);
            musicService.showNotification(R.drawable.ic_pause);
            musicService.start();
            seekBar.setMax(musicService.getDuration() / 1000);
            MusicPlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
        }
    }

    private void nextThreadBtn() {
        nextThread = new Thread() {
            @Override
            public void run() {
                super.run();
                img_music_next.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        nextBtnClicked();
                    }
                });
            }
        };
        nextThread.start();
    }

    public void nextBtnClicked() {
        if (musicService.isPlaying()) {
            musicService.stop();
            musicService.release();

            if (isShuffled && !isRepeat) {
                position = getRandom(playlist.size() - 1);
            } else if (!isShuffled && !isRepeat) {
                position = (position + 1) % playlist.size();
            }

            Uri uri = Uri.parse(playlist.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);

            txt_song_name.setText(playlist.get(position).getName());
            txt_song_artist.setText(playlist.get(position).getArtist());

            seekBar.setMax(musicService.getDuration() / 1000);
            MusicPlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });

            musicService.OnCompleted();
            musicService.showNotification(R.drawable.ic_pause);
            fab_music_play_pause.setBackgroundResource(R.drawable.ic_pause);
            musicService.start();
        } else {
            musicService.stop();
            musicService.release();

            if (isShuffled && !isRepeat) {
                position = getRandom(playlist.size() - 1);
            } else if (!isShuffled && !isRepeat) {
                position = (position + 1) % playlist.size();
            }

            Uri uri = Uri.parse(playlist.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);

            txt_song_name.setText(playlist.get(position).getName());
            txt_song_artist.setText(playlist.get(position).getArtist());

            seekBar.setMax(musicService.getDuration() / 1000);
            MusicPlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.OnCompleted();
            musicService.showNotification(R.drawable.ic_play);
            fab_music_play_pause.setBackgroundResource(R.drawable.ic_play);
        }
    }

    private int getRandom(int i) {
        Random random = new Random();
        return random.nextInt(i + 1);
    }

    private void previousThreadBtn() {
        previousThread = new Thread() {
            @Override
            public void run() {
                super.run();
                img_music_previous.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        prevBtnClicked();
                    }
                });
            }
        };
        previousThread.start();
    }

    public void prevBtnClicked() {
        if (musicService.isPlaying()) {
            musicService.stop();
            musicService.release();

            if (isShuffled && !isRepeat) {
                position = getRandom(playlist.size() - 1);
            } else if (!isShuffled && !isRepeat) {
                position = (position - 1) < 0 ? (playlist.size() - 1) : (position - 1);
            }

            Uri uri = Uri.parse(playlist.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);

            txt_song_name.setText(playlist.get(position).getName());
            txt_song_artist.setText(playlist.get(position).getArtist());

            seekBar.setMax(musicService.getDuration() / 1000);
            MusicPlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.OnCompleted();
            musicService.showNotification(R.drawable.ic_pause);
            fab_music_play_pause.setBackgroundResource(R.drawable.ic_pause);
            musicService.start();
        } else {
            musicService.stop();
            musicService.release();

            if (isShuffled && !isRepeat) {
                position = getRandom(playlist.size() - 1);
            } else if (!isShuffled && !isRepeat) {
                position = (position - 1) < 0 ? (playlist.size() - 1) : (position - 1);
            }

            Uri uri = Uri.parse(playlist.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);

            txt_song_name.setText(playlist.get(position).getName());
            txt_song_artist.setText(playlist.get(position).getArtist());

            seekBar.setMax(musicService.getDuration() / 1000);
            MusicPlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.OnCompleted();
            musicService.showNotification(R.drawable.ic_play);
            fab_music_play_pause.setBackgroundResource(R.drawable.ic_play);
        }
    }

    public void imageAnimation(Context context, ImageView imageView, Bitmap bitmap) {
        Animation animOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
        Animation animIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);

        animOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Glide.with(context).load(bitmap).into(imageView);
                animIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                imageView.startAnimation(animIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        imageView.startAnimation(animOut);
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (mediaPlayer != null) {
//            mediaPlayer.release();
//            mediaPlayer = null;
//        }
//    }

    @Override
    protected void onResume() {
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, this, BIND_AUTO_CREATE);
        playThreadBtn();
        nextThreadBtn();
        previousThreadBtn();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this);
    }

    private void backPressed() {
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        MusicService.MyBinder myBinder = (MusicService.MyBinder) service;
        musicService = myBinder.getService();
        musicService.setCallBack(this);

        seekBar.setMax(musicService.getDuration() / 1000);
        musicService.OnCompleted();
        musicService.showNotification(R.drawable.ic_pause);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        musicService = null;
    }


}