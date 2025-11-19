package com.music;

import atlantafx.base.theme.PrimerDark;
import com.music.model.AppConfig;
import com.music.util.DataManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.io.InputStream;

public class Main extends Application {
    
    private AppController controller;

    @Override
    public void start(Stage stage) throws Exception {
        // 1. Carregar Configuração
        AppConfig config = DataManager.loadAppConfig();
        if (config.debug) System.out.println("Iniciando " + config.appName + " v" + config.version + " (Debug Mode)");

        // 2. Tema e Fontes
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        loadCustomFont("/com/music/fonts/Inter-Regular.ttf", config.debug); 
        
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 800);
        controller = fxmlLoader.getController();

        // 3. Configurar Stage via JSON
        stage.setTitle(config.appName + " v" + config.version);
        
        try {
            String iconPath = config.iconPath.startsWith("/") ? config.iconPath : "/com/music/" + config.iconPath;
            InputStream iconStream = Main.class.getResourceAsStream(iconPath);
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
            } else if (config.debug) {
                System.out.println("Ícone não encontrado: " + iconPath);
            }
        } catch (Exception ignored) {}

        // 4. Atalhos Globais
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {
                case SPACE -> { controller.togglePlay(); event.consume(); }
                case RIGHT -> controller.seekForward();
                case LEFT -> controller.seekBackward();
                case UP -> controller.adjustVolume(0.1);
                case DOWN -> controller.adjustVolume(-0.1);
                case M -> controller.toggleMute();
            }
        });

        // 5. Otimização
        stage.iconifiedProperty().addListener((obs, wasMinimized, isMinimized) -> {
            if (controller != null) controller.setLowPowerMode(isMinimized);
        });

        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setMaximized(true); 
        stage.setScene(scene);
        stage.show();
    }

    private void loadCustomFont(String path, boolean debug) {
        try {
            InputStream fontStream = Main.class.getResourceAsStream(path);
            if (fontStream != null) {
                Font.loadFont(fontStream, 12);
                if (debug) System.out.println("Fonte carregada: " + path);
            }
        } catch (Exception e) {
            if (debug) System.out.println("Erro ao carregar fonte: " + path);
        }
    }

    @Override
    public void stop() {
        if (controller != null) controller.saveData();
    }

    public static void main(String[] args) {
        launch();
    }
}