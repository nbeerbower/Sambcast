package com.beerbower.sambcast.app;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import jcifs.netbios.NbtAddress;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;


public class MainActivity extends ActionBarActivity implements MediaController.MediaPlayerControl {

    private ArrayList<Song> songList;
    private ListView songView;

    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;

    private boolean paused = false;
    private boolean playbackPaused = false;

    private MusicController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songView = (ListView)findViewById(R.id.song_list);
        songList = new ArrayList<Song>();
        getSongList();
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);

        setController();
        new SambaTask().execute();
    }

    class SambaTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            /* XXX: SAMBA Test stuff */
            SmbFile[] domains = null;
            try {
                domains = (new SmbFile("smb://")).listFiles();
                for (int i = 0; i < domains.length; i++) {
                    Log.v("SAMBA", domains[i].getName() + " - domain, Path = " + domains[i].getPath());
                    Log.v("SAMBA", "File Listing for " + domains[i].getName());
                    SmbFile[] servers = null;
                    try {
                        servers = domains[i].listFiles();
                    } catch (SmbException e) {
                        e.printStackTrace();
                        return null;
                    }

                    if(servers != null) {
                        if (servers.length >0) {
                            for (int j = 0; j < servers.length; j++) {
                                Log.v("SAMBA-SERVER", servers[j].getName() + " - server, Path = " + servers[i].getPath());
                                listShares(servers[j].getName());
                            }
                        } else {
                            listShares(domains[i].getName());
                        }
                    } else {
                        listShares(domains[i].getName());
                    }
                    Log.v("SAMBA", "Scan finished !");
                }
            } catch (SmbException e) {
                e.printStackTrace();
                return null;
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }
            return null;
        }
    }

    private void listShares(String name) {
        Log.v("SAMBA", "File Listing for " + name);
        String host;
        try {
            host = name;
            if(host.endsWith("/"))
                host = host.substring(0, host.length() - 1);
            NbtAddress addrs = NbtAddress.getByName(host);
            SmbFile test = new SmbFile("smb://" + addrs.getHostAddress());
            SmbFile[] files = test.listFiles();
            for(SmbFile s : files)
            {
                Log.v("SAMBA", s.getName());
            }
        } catch (SmbException e) {
            Log.e("SAMBA", "ERROR");
        } catch (MalformedURLException e) {
            Log.e("SAMBA", "ERROR");
        } catch (UnknownHostException e) {
            Log.e("SAMBA", "ERROR");
        }

    }

    private ServiceConnection musicConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent==null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
        // Allow smb:// URIs
        jcifs.Config.registerSmbURLHandler();
    }

    @Override
    protected void onDestroy() {
        controller.hide();
        stopService(playIntent);
        musicSrv = null;
        unbindService(musicConnection);
        super.onDestroy();
    }

    @Override
    protected void onPause(){
        super.onPause();
        paused=true;
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (paused) {
            setController();
            paused = false;
        }
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }

    public void getSongList() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        /*String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " +
            MediaStore.Audio.Media.DATA + " LIKE '/mnt/sdcard/Music/%'";*/
        Toast.makeText(this, musicUri.toString(), Toast.LENGTH_LONG).show();
        Cursor musicCursor = musicResolver.query(musicUri, null, selection, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            // Scrape data
            int titleColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int sizeColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.SIZE);
            // Add the songs to the list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String title = musicCursor.getString(titleColumn);
                String artist = musicCursor.getString(artistColumn);
                String album = musicCursor.getString(albumColumn);
                long size = musicCursor.getLong(sizeColumn);
                songList.add(new Song(thisId, title, artist, album, size));
            }
            while (musicCursor.moveToNext());

            musicCursor.close();
        }
    }

    private void setController() {
        controller = new MusicController(this);
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playPrev();
            }
        });
        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);
    }

    public void songPicked(View view){
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                //shuffle
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicSrv = null;
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void start() {
        musicSrv.go();
    }

    @Override
    public void pause() {
        playbackPaused=true;
        musicSrv.pausePlayer();
    }

    @Override
    public int getDuration() {
        if (musicSrv !=null && musicBound && musicSrv.isPlaying()){
            return musicSrv.getDur();
        } else {
            return 0;
        }
    }

    @Override
    public int getCurrentPosition() {
        if (musicSrv !=null && musicBound && musicSrv.isPlaying()){
            return musicSrv.getPos();
        } else {
            return 0;
        }
    }

    @Override
    public void seekTo(int i) {
        musicSrv.seek(i);
    }

    @Override
    public boolean isPlaying() {
        if (musicSrv !=null && musicBound){
            return musicSrv.isPlaying();
        } else {
            return false;
        }
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    private void playNext(){
        musicSrv.playNext();
        if(playbackPaused){
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    private void playPrev(){
        musicSrv.playPrev();
        if(playbackPaused){
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }
}
