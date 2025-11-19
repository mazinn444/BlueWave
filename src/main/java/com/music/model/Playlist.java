package com.music.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Playlist {
    private String id;
    private String name;
    private List<Song> songs;

    public Playlist(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.songs = new ArrayList<>();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public List<Song> getSongs() { return songs; }
    
    public void addSong(Song s) {
        if (songs == null) songs = new ArrayList<>();
        songs.add(s);
    }
    
    @Override
    public String toString() { return name; }
}