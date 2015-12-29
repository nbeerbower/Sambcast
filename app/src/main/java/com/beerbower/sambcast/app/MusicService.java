package com.beerbower.sambcast.app;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.app.Service;
import android.os.PowerManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Nicholas on 12/29/2015.
 */
public class MusicService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private final IBinder musicBind = new MusicBinder();
    private MediaPlayer player;
    private ArrayList<Song> songList;
    private int songPos;

    public void onCreate() {
        super.onCreate();
        songPos = 0;
        player = new MediaPlayer();
        initMusicPlayer();
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
    }

    public void initMusicPlayer(){
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }
    public void setList(ArrayList<Song> songs){
        songList = songs;
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void playSong(){
        player.reset();
        Song playSong = songList.get(songPos);
        long curSong = playSong.getID();
        Uri trackUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, curSong);
        try {
            player.setDataSource(getApplicationContext(), trackUri);
        } catch(Exception e){
            Log.e("MusicService", "Error setting data source", e);
        }
        player.prepareAsync();
    }

    public void setSong(int songIndex){
        songPos = songIndex;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent){
        player.stop();
        player.release();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if(player.getCurrentPosition() > 0){
            mp.reset();
            playNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
        String songTitle = songList.get(songPos).getTitle();
        String songArtist = songList.get(songPos).getArtist();
        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0, notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.ic_launcher)
                .setTicker(songTitle + " - " + songArtist)
                .setOngoing(true)
                .setContentTitle("Sambcast")
                .setContentText(songTitle + " - " + songArtist);
        Notification not = builder.build();

        startForeground(1, not);
    }

    public int getPos() {
        return player.getCurrentPosition();
    }

    public int getDur() {
        return player.getDuration();
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    public void pausePlayer() {
        player.pause();
    }

    public void seek(int pos) {
        player.seekTo(pos);
    }

    public void go() {
        player.start();
    }

    public void playPrev() {
        songPos--;
        if(songPos < 0) songPos = songList.size()-1;
        playSong();
    }

    public void playNext() {
        songPos = (songPos+1 > songList.size()-1)? 0 : songPos+1;
        playSong();
    }
}
