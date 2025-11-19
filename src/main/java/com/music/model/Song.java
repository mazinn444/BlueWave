package com.music.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.File;
import java.io.Serializable;

public class Song implements Serializable {
    // Properties são transient para o GSON ignorar (ele salva apenas as Strings)
    private transient StringProperty titleProp;
    private transient StringProperty artistProp;
    private transient StringProperty durationProp;

    // Dados persistentes
    private String title;
    private String artist;
    private String path;
    private String durationStr;

    public Song(File file) {
        this.path = file.toURI().toString();
        // Padrão inicial: Nome do arquivo e Artista Desconhecido
        this.title = file.getName().replace(".mp3", "").replace(".wav", "");
        this.artist = "Desconhecido";
        this.durationStr = "--:--";
        initProps();
    }

    // Inicializa as propriedades JavaFX e vincula aos dados
    public void initProps() {
        if (titleProp != null) return; // Já inicializado

        this.titleProp = new SimpleStringProperty(title);
        this.artistProp = new SimpleStringProperty(artist);
        this.durationProp = new SimpleStringProperty(durationStr);
        
        // Listeners: Se a Property mudar (via tabela/código), atualiza a String para salvar depois
        this.titleProp.addListener((o, old, val) -> this.title = val);
        this.artistProp.addListener((o, old, val) -> this.artist = val);
        this.durationProp.addListener((o, old, val) -> this.durationStr = val);
    }

    // Getters de Property (para o TableView)
    public StringProperty titleProperty() { if(titleProp==null) initProps(); return titleProp; }
    public StringProperty artistProperty() { if(artistProp==null) initProps(); return artistProp; }
    public StringProperty durationProperty() { if(durationProp==null) initProps(); return durationProp; }

    // Getters e Setters normais
    public String getTitle() { return titleProperty().get(); }
    public void setTitle(String title) { titleProperty().set(title); }

    public String getArtist() { return artistProperty().get(); }
    public void setArtist(String artist) { artistProperty().set(artist); }

    public String getDurationStr() { return durationProperty().get(); }
    public void setDurationStr(String s) { durationProperty().set(s); }

    public String getPath() { return path; }

    @Override
    public String toString() { return getTitle(); }
}