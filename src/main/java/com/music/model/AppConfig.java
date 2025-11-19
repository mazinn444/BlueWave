package com.music.model;

public class AppConfig {
    public String appName;
    public String iconPath;
    public String version;
    public boolean debug;

    // Construtor padrão para evitar crash se o JSON falhar
    public AppConfig() {
        this.appName = "Music Player";
        this.iconPath = "icon.png";
        this.version = "0.0.1";
        this.debug = true;
    }
}