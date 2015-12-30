package com.beerbower.sambcast.app;

/**
 * Created by Nicholas on 12/28/2015.
 */
public class Song {
    private long id;
    private String title;
    private String artist;
    private String album;
    private long size;

    public Song(long songID, String songTitle, String songArtist, String songAlbum, long songSize) {
        id = songID;
        title = songTitle;
        artist = songArtist;
        album = songAlbum;
        size = songSize;
    }

    public long getID() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
}
