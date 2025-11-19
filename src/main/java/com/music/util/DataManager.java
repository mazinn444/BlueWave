package com.music.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.music.model.AppConfig;
import com.music.model.AppData;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DataManager {
    private static final String DATA_FILE = "bluewave_data.json";
    private static final String CONFIG_FILE = "/com/music/config.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private static AppConfig cachedConfig;

    // --- User Data (Playlists, Volume, Configs do Usuário) ---

    public static void save(AppData data) {
        try (Writer writer = new FileWriter(DATA_FILE)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static AppData load() {
        if (!Files.exists(Paths.get(DATA_FILE))) return new AppData();
        try (Reader reader = new FileReader(DATA_FILE)) {
            return gson.fromJson(reader, AppData.class);
        } catch (IOException e) {
            return new AppData();
        }
    }

    // --- System Config (config.json - Somente Leitura) ---

    public static AppConfig loadAppConfig() {
        if (cachedConfig != null) return cachedConfig;

        try (InputStream is = DataManager.class.getResourceAsStream(CONFIG_FILE)) {
            if (is == null) {
                System.out.println("CRÍTICO: config.json não encontrado em " + CONFIG_FILE);
                cachedConfig = new AppConfig(); // Usa padrões
            } else {
                Reader reader = new InputStreamReader(is);
                cachedConfig = gson.fromJson(reader, AppConfig.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
            cachedConfig = new AppConfig();
        }
        return cachedConfig;
    }
    
    public static AppConfig getConfig() {
        if (cachedConfig == null) loadAppConfig();
        return cachedConfig;
    }
}