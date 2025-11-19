package com.music;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import com.music.model.*;
import com.music.util.DataManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.util.*;

public class AppController {

    private enum RepeatState { NO_REPEAT, REPEAT_ALL, REPEAT_ONE }

    @FXML private VBox viewLibrary, viewSettings, queueDrawer;
    @FXML private ListView<Playlist> playlistList;
    @FXML private ListView<Song> queueListView;
    @FXML private TableView<Song> songTable;
    @FXML private TableColumn<Song, String> colTitle, colArtist, colDuration;
    @FXML private Label lblCurrentPlaylistName, lblStats, lblPlayerTitle, lblPlayerArtist;
    @FXML private Label lblCurrentTime, lblTotalTime;
    @FXML private Slider seekSlider, speedSlider, volumeSlider;
    @FXML private Button btnPlay, btnRepeat, btnQueue;
    @FXML private ToggleButton btnShuffle;
    @FXML private ImageView coverImage;
    @FXML private HBox visualizerContainer;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> comboSearchType;

    // Configurações UI
    @FXML private ToggleSwitch toggleTheme;
    @FXML private ToggleSwitch togglePotato;
    @FXML private Label lblVersion; // Label da versão injetada

    private AppData appData;
    private MediaPlayer mediaPlayer;
    
    private ObservableList<Song> masterSongList = FXCollections.observableArrayList();
    private FilteredList<Song> filteredSongs;
    
    private List<Song> currentContextList;
    private List<Integer> shuffleOrder = new ArrayList<>();
    private int queueIndex = -1;
    private int shufflePos = -1;

    private boolean isPlaying = false;
    private boolean isMuted = false;
    private double lastVolume = 0.5;
    private RepeatState repeatState = RepeatState.NO_REPEAT;
    
    private Rectangle[] spectrumBars;
    private AudioSpectrumListener spectrumListener;
    private static final int BANDS = 16;

    @FXML
    public void initialize() {
        // 1. Carregar Dados do Usuário
        appData = DataManager.load();
        
        // 2. Carregar Config do App (JSON) e setar Versão
        AppConfig config = DataManager.getConfig();
        if (lblVersion != null) lblVersion.setText("v" + config.version);
        
        // 3. Restaurar configs de UI
        toggleTheme.setSelected(appData.isDarkMode);
        togglePotato.setSelected(appData.isPotatoMode);
        applyTheme();

        toggleTheme.selectedProperty().addListener((o, old, val) -> {
            appData.isDarkMode = val; applyTheme(); saveData();
        });
        togglePotato.selectedProperty().addListener((o, old, val) -> {
            appData.isPotatoMode = val; applyPotatoMode(); saveData();
        });

        // 4. Inicializar Músicas
        for (Playlist p : appData.playlists) {
            for (Song s : p.getSongs()) {
                s.initProps();
                if (s.getDurationStr().equals("--:--")) preloadMetadata(s);
            }
        }
        if (appData.playlists.isEmpty()) appData.playlists.add(new Playlist("Biblioteca Principal"));
        
        // 5. Setup UI Components
        setupPlaylistList();
        setupSongTable();
        setupQueueDrawer();
        setupControls();
        setupVisualizer();
        applyPotatoMode();
        updateRepeatButtonUI();
        
        comboSearchType.setItems(FXCollections.observableArrayList("Tudo", "Título", "Artista", "Duração"));
        comboSearchType.getSelectionModel().selectFirst();

        filteredSongs = new FilteredList<>(masterSongList, p -> true);
        songTable.setItems(filteredSongs);
        
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> updateFilter());
        comboSearchType.valueProperty().addListener((obs, oldVal, newVal) -> updateFilter());
    }

    // --- Config Methods ---

    private void applyTheme() {
        Application.setUserAgentStylesheet(appData.isDarkMode ? 
            new PrimerDark().getUserAgentStylesheet() : new PrimerLight().getUserAgentStylesheet());
    }

    private void applyPotatoMode() {
        if (appData.isPotatoMode) {
            visualizerContainer.setVisible(false);
            if (mediaPlayer != null) mediaPlayer.setAudioSpectrumListener(null);
        } else {
            visualizerContainer.setVisible(true);
            if (mediaPlayer != null && isPlaying) mediaPlayer.setAudioSpectrumListener(spectrumListener);
        }
    }
    
    public void setLowPowerMode(boolean active) {
        if (appData.isPotatoMode) return;
        if (mediaPlayer != null) mediaPlayer.setAudioSpectrumListener(active ? null : spectrumListener);
    }

    // --- Setup Components ---

    private void setupPlaylistList() {
        playlistList.getItems().addAll(appData.playlists);
        playlistList.setCellFactory(lv -> {
            ListCell<Playlist> cell = new ListCell<>() { @Override protected void updateItem(Playlist i, boolean e) { super.updateItem(i, e); setText((e||i==null)?null:i.getName()); } };
            ContextMenu m = new ContextMenu(); MenuItem e = new MenuItem("✏ Editar"); MenuItem d = new MenuItem("🗑 Excluir");
            e.setOnAction(ev -> editPlaylistName(cell.getItem())); d.setOnAction(ev -> deletePlaylist(cell.getItem()));
            m.getItems().addAll(e, d); cell.emptyProperty().addListener((o,w,i) -> cell.setContextMenu(i?null:m)); return cell;
        });
        playlistList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> { if (newVal != null) loadPlaylistToTable(newVal); });
        playlistList.getSelectionModel().selectFirst();
    }

    private void setupSongTable() {
        colTitle.setCellValueFactory(data -> data.getValue().titleProperty());
        colArtist.setCellValueFactory(data -> data.getValue().artistProperty());
        colDuration.setCellValueFactory(data -> data.getValue().durationProperty());
        
        songTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentContextList = new ArrayList<>(masterSongList); 
                int index = currentContextList.indexOf(newVal);
                if (btnShuffle.isSelected()) generateShuffleOrder(index); else queueIndex = index;
                lblPlayerTitle.setText(newVal.getTitle());
                lblPlayerArtist.setText(newVal.getArtist());
                lblPlayerTitle.setTextFill(Color.web("#0078d7"));
            }
        });

        songTable.setRowFactory(tv -> {
            TableRow<Song> row = new TableRow<>();
            row.setOnMouseClicked(event -> { if (event.getClickCount() == 2 && (!row.isEmpty())) playInternal(row.getItem()); });
            
            row.setOnDragDetected(event -> {
                if (!row.isEmpty() && txtSearch.getText().isEmpty()) {
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    db.setDragView(row.snapshot(null, null));
                    ClipboardContent cc = new ClipboardContent(); cc.putString(Integer.toString(row.getIndex())); db.setContent(cc);
                    event.consume();
                }
            });
            row.setOnDragOver(event -> { if (event.getGestureSource() != row && event.getDragboard().hasString() && txtSearch.getText().isEmpty()) event.acceptTransferModes(TransferMode.MOVE); event.consume(); });
            row.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasString() && txtSearch.getText().isEmpty()) {
                    int draggedIndex = Integer.parseInt(db.getString());
                    int dropIndex = row.isEmpty() ? masterSongList.size() - 1 : row.getIndex();
                    if (draggedIndex != dropIndex) {
                        Song draggedSong = masterSongList.remove(draggedIndex);
                        masterSongList.add(dropIndex, draggedSong);
                        Playlist p = playlistList.getSelectionModel().getSelectedItem();
                        if(p != null) { p.getSongs().clear(); p.getSongs().addAll(masterSongList); }
                        if (queueIndex == draggedIndex) queueIndex = dropIndex;
                        else if (queueIndex > draggedIndex && queueIndex <= dropIndex) queueIndex--;
                        else if (queueIndex < draggedIndex && queueIndex >= dropIndex) queueIndex++;
                        if (btnShuffle.isSelected()) generateShuffleOrder(queueIndex);
                        saveData();
                    }
                    event.setDropCompleted(true);
                } else event.setDropCompleted(false);
                event.consume();
            });
            ContextMenu menu = new ContextMenu(); MenuItem rem = new MenuItem("❌ Remover"); rem.setOnAction(e -> removeSong(row.getItem())); menu.getItems().add(rem);
            row.emptyProperty().addListener((obs, w, isEmpty) -> row.setContextMenu(isEmpty ? null : menu));
            return row;
        });
    }
    
    private void setupQueueDrawer() {
        queueListView.setCellFactory(lv -> {
            ListCell<Song> cell = new ListCell<>() {
                @Override protected void updateItem(Song item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); setStyle(""); } 
                    else {
                        setText(item.getTitle());
                        if (mediaPlayer != null && lblPlayerTitle.getText().equals(item.getTitle())) {
                            setText("▶ " + getText()); setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-accent;");
                        } else { setStyle(""); }
                    }
                }
            };
            cell.setOnDragDetected(event -> {
                if (!cell.isEmpty()) {
                    Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent cc = new ClipboardContent(); cc.putString(Integer.toString(cell.getIndex())); db.setContent(cc);
                    db.setDragView(cell.snapshot(null, null)); event.consume();
                }
            });
            cell.setOnDragOver(event -> { if (event.getGestureSource() != cell && event.getDragboard().hasString()) event.acceptTransferModes(TransferMode.MOVE); event.consume(); });
            cell.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasString() && currentContextList != null) {
                    int draggedIdx = Integer.parseInt(db.getString());
                    int dropIdx = cell.isEmpty() ? queueListView.getItems().size() - 1 : cell.getIndex();
                    if (draggedIdx != dropIdx && draggedIdx >= 0 && dropIdx >= 0 && dropIdx < currentContextList.size()) {
                        Song s = currentContextList.remove(draggedIdx); currentContextList.add(dropIdx, s);
                        if (queueIndex == draggedIdx) queueIndex = dropIdx;
                        else if (queueIndex > draggedIdx && queueIndex <= dropIdx) queueIndex--;
                        else if (queueIndex < draggedIdx && queueIndex >= dropIdx) queueIndex++;
                        if(btnShuffle.isSelected()) generateShuffleOrder(queueIndex);
                        updateQueueDrawer();
                    }
                    event.setDropCompleted(true);
                } else event.setDropCompleted(false);
                event.consume();
            });
            cell.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !cell.isEmpty()) {
                    Song s = cell.getItem(); int idx = currentContextList.indexOf(s);
                    if (idx != -1) { queueIndex = idx; if (btnShuffle.isSelected()) generateShuffleOrder(idx); playInternal(s); updateQueueDrawer(); }
                }
            });
            return cell;
        });
    }

    private void setupControls() {
        volumeSlider.setValue(appData.savedVolume);
        volumeSlider.valueProperty().addListener((o, old, val) -> { if (mediaPlayer != null) mediaPlayer.setVolume(val.doubleValue()); appData.savedVolume = val.doubleValue(); });
        speedSlider.valueProperty().addListener((o, old, val) -> { if (mediaPlayer != null) mediaPlayer.setRate(val.doubleValue()); });
        seekSlider.setOnMouseReleased(e -> { if (mediaPlayer != null) mediaPlayer.seek(Duration.seconds(seekSlider.getValue())); });
        seekSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateSliderFill());

        btnRepeat.setOnAction(e -> {
            if (repeatState == RepeatState.NO_REPEAT) repeatState = RepeatState.REPEAT_ALL;
            else if (repeatState == RepeatState.REPEAT_ALL) repeatState = RepeatState.REPEAT_ONE;
            else repeatState = RepeatState.NO_REPEAT;
            updateRepeatButtonUI();
        });
        btnShuffle.setOnAction(e -> {
            if (btnShuffle.isSelected()) generateShuffleOrder(queueIndex);
            else { shuffleOrder.clear(); shufflePos = -1; }
            updateQueueDrawer();
        });
    }

    private void setupVisualizer() {
        spectrumBars = new Rectangle[BANDS];
        visualizerContainer.getChildren().clear();
        for (int i = 0; i < BANDS; i++) {
            Rectangle bar = new Rectangle(5, 5);
            bar.setArcWidth(2); bar.setArcHeight(2);
            bar.setFill(Color.web("#0078d7"));
            bar.getStyleClass().add("visualizer-bar");
            spectrumBars[i] = bar;
            visualizerContainer.getChildren().add(bar);
        }
        spectrumListener = (timestamp, duration, magnitudes, phases) -> {
            if (spectrumBars == null) return;
            for (int i = 0; i < BANDS && i < magnitudes.length && i < spectrumBars.length; i++) {
                if (spectrumBars[i] != null) {
                    double h = (60 + magnitudes[i]) * 1.5;
                    if (h < 2) h = 2;
                    spectrumBars[i].setHeight(h);
                    spectrumBars[i].setOpacity(0.5 + (h / 60.0));
                }
            }
        };
    }

    // Metadata & Helpers
    private void preloadMetadata(Song song) {
        try {
            Media tempMedia = new Media(song.getPath());
            tempMedia.durationProperty().addListener((obs, oldVal, newVal) -> { if (newVal != null) Platform.runLater(() -> song.setDurationStr(formatTime(newVal))); });
            tempMedia.getMetadata().addListener((MapChangeListener<String, Object>) change -> {
                if (change.wasAdded()) {
                    String key = change.getKey(); Object val = change.getValueAdded();
                    Platform.runLater(() -> { if (key.equals("artist")) song.setArtist(val.toString()); else if (key.equals("title")) song.setTitle(val.toString()); });
                }
            });
        } catch (Exception e) {}
    }
    private void updateFilter() {
        String filterText = txtSearch.getText(); String type = comboSearchType.getValue();
        filteredSongs.setPredicate(song -> {
            if (filterText == null || filterText.isEmpty()) return true;
            String lower = filterText.toLowerCase();
            String t = song.getTitle().toLowerCase(); String a = song.getArtist().toLowerCase(); String d = song.getDurationStr().toLowerCase();
            switch (type) {
                case "Título": return t.contains(lower); case "Artista": return a.contains(lower); case "Duração": return d.contains(lower);
                default: return t.contains(lower) || a.contains(lower) || d.contains(lower);
            }
        });
    }
    private void updateSliderFill() {
        double percentage = (seekSlider.getValue() / seekSlider.getMax()) * 100;
        Node track = seekSlider.lookup(".track");
        if (track != null) track.setStyle("-fx-background-color: linear-gradient(to right, #0078d7 " + percentage + "%, #404040 " + percentage + "%);");
    }
    private void loadPlaylistToTable(Playlist p) {
        lblCurrentPlaylistName.setText(p.getName());
        lblStats.setText(p.getSongs().size() + " músicas");
        masterSongList.setAll(p.getSongs());
        currentContextList = new ArrayList<>(masterSongList);
        for(Song s : masterSongList) if(s.getDurationStr().equals("--:--")) preloadMetadata(s);
        if (!isPlaying) updateQueueDrawer();
    }
    @FXML private void importFiles() {
        FileChooser fc = new FileChooser(); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav"));
        List<File> files = fc.showOpenMultipleDialog(null);
        if (files != null && !files.isEmpty()) {
            Playlist p = playlistList.getSelectionModel().getSelectedItem();
            for (File f : files) { Song s = new Song(f); p.addSong(s); masterSongList.add(s); preloadMetadata(s); }
            currentContextList = new ArrayList<>(masterSongList);
            lblStats.setText(masterSongList.size() + " músicas");
            saveData(); updateQueueDrawer();
        }
    }
    private void editPlaylistName(Playlist p) { if(p==null)return; TextInputDialog d = new TextInputDialog(p.getName()); d.showAndWait().ifPresent(n -> { p.setName(n); playlistList.refresh(); lblCurrentPlaylistName.setText(n); saveData(); }); }
    private void deletePlaylist(Playlist p) { if(p==null)return; Alert a=new Alert(Alert.AlertType.CONFIRMATION); a.setHeaderText("Apagar?"); a.showAndWait().ifPresent(b->{ if(b==ButtonType.OK){appData.playlists.remove(p); playlistList.getItems().remove(p); saveData();}}); }
    private void removeSong(Song s) { masterSongList.remove(s); Playlist p = playlistList.getSelectionModel().getSelectedItem(); if (p != null) p.getSongs().remove(s); saveData(); }
    private void updateQueueDrawer() {
        if (currentContextList == null) return;
        ObservableList<Song> queueItems = FXCollections.observableArrayList();
        if (btnShuffle.isSelected() && !shuffleOrder.isEmpty()) {
            for (int i : shuffleOrder) if (i < currentContextList.size()) queueItems.add(currentContextList.get(i));
        } else { queueItems.addAll(currentContextList); }
        queueListView.setItems(queueItems); queueListView.refresh();
    }
    private void generateShuffleOrder(int startingIndex) {
        if (currentContextList == null || currentContextList.isEmpty()) return;
        shuffleOrder.clear(); for (int i = 0; i < currentContextList.size(); i++) shuffleOrder.add(i);
        Collections.shuffle(shuffleOrder);
        if (startingIndex >= 0) { shuffleOrder.remove(Integer.valueOf(startingIndex)); shuffleOrder.add(0, startingIndex); shufflePos = 0; } 
        else { shufflePos = -1; }
    }
    private void updateRepeatButtonUI() {
        btnRepeat.getStyleClass().removeAll("repeat-active", "repeat-one"); btnRepeat.setOpacity(1.0);
        switch (repeatState) { case NO_REPEAT -> { btnRepeat.setText("🔁"); btnRepeat.setOpacity(0.5); } case REPEAT_ALL -> { btnRepeat.setText("🔁"); btnRepeat.getStyleClass().add("repeat-active"); } case REPEAT_ONE -> { btnRepeat.setText("🔂"); btnRepeat.getStyleClass().add("repeat-one"); } }
    }

    // Player Logic
    private void playInternal(Song song) {
        if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.dispose(); }
        Media media = new Media(song.getPath());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setVolume(volumeSlider.getValue());
        mediaPlayer.setRate(speedSlider.getValue());
        
        if (!appData.isPotatoMode) mediaPlayer.setAudioSpectrumListener(spectrumListener);

        mediaPlayer.setOnReady(() -> {
            lblPlayerTitle.setText(song.getTitle()); lblPlayerArtist.setText(song.getArtist());
            lblTotalTime.setText(formatTime(media.getDuration())); seekSlider.setMax(media.getDuration().toSeconds());
            Object img = media.getMetadata().get("image"); coverImage.setImage(img instanceof Image ? (Image)img : null);
            mediaPlayer.play(); isPlaying = true; btnPlay.setText("⏸");
            songTable.getSelectionModel().select(song); songTable.scrollTo(song);
            if(song.getDurationStr().equals("--:--")) song.setDurationStr(formatTime(media.getDuration()));
        });

        mediaPlayer.currentTimeProperty().addListener((obs, o, n) -> { 
            if (!seekSlider.isPressed()) seekSlider.setValue(n.toSeconds()); 
            lblCurrentTime.setText(formatTime(n)); updateSliderFill();
        });
        mediaPlayer.setOnEndOfMedia(() -> { if (repeatState == RepeatState.REPEAT_ONE) { mediaPlayer.seek(Duration.ZERO); mediaPlayer.play(); } else playNext(); });
    }

    public void togglePlay() { if(mediaPlayer!=null){if(isPlaying){mediaPlayer.pause();btnPlay.setText("▶");}else{mediaPlayer.play();btnPlay.setText("⏸");}isPlaying=!isPlaying;} else if (queueIndex != -1 && currentContextList != null) { playInternal(currentContextList.get(queueIndex)); }}
    @FXML private void playNext() {
        if (currentContextList == null || currentContextList.isEmpty()) return;
        if (btnShuffle.isSelected()) {
            if (shuffleOrder.isEmpty()) generateShuffleOrder(queueIndex);
            shufflePos++;
            if (shufflePos >= shuffleOrder.size()) { if (repeatState == RepeatState.REPEAT_ALL) { generateShuffleOrder(-1); shufflePos = 0; } else { if(mediaPlayer!=null)mediaPlayer.stop(); isPlaying = false; btnPlay.setText("▶"); return; } }
            queueIndex = shuffleOrder.get(shufflePos);
        } else {
            int next = queueIndex + 1; if (next >= currentContextList.size()) { if (repeatState == RepeatState.REPEAT_ALL) next = 0; else { if(mediaPlayer!=null)mediaPlayer.stop(); isPlaying = false; btnPlay.setText("▶"); return; } }
            queueIndex = next;
        }
        playInternal(currentContextList.get(queueIndex)); updateQueueDrawer();
    }
    @FXML private void playPrev() {
        if (currentContextList == null || currentContextList.isEmpty()) return;
        if (mediaPlayer != null && mediaPlayer.getCurrentTime().toSeconds() > 3) { mediaPlayer.seek(Duration.ZERO); return; }
        if (btnShuffle.isSelected()) { if (shufflePos > 0) { shufflePos--; queueIndex = shuffleOrder.get(shufflePos); } else queueIndex = shuffleOrder.get(0); } 
        else { queueIndex = (queueIndex > 0) ? queueIndex - 1 : currentContextList.size() - 1; }
        playInternal(currentContextList.get(queueIndex)); updateQueueDrawer();
    }
    public void seekForward() { if(mediaPlayer!=null) mediaPlayer.seek(mediaPlayer.getCurrentTime().add(Duration.seconds(10))); }
    public void seekBackward() { if(mediaPlayer!=null) mediaPlayer.seek(mediaPlayer.getCurrentTime().subtract(Duration.seconds(10))); }
    public void adjustVolume(double d) { double v=volumeSlider.getValue()+d; if(v>1)v=1;if(v<0)v=0; volumeSlider.setValue(v); }
    public void toggleMute() { if(isMuted){volumeSlider.setValue(lastVolume);isMuted=false;}else{lastVolume=volumeSlider.getValue();volumeSlider.setValue(0);isMuted=true;}}
    @FXML private void toggleQueueView() { boolean visible = queueDrawer.isVisible(); queueDrawer.setVisible(!visible); queueDrawer.setManaged(!visible); if (!visible) updateQueueDrawer(); }
    @FXML private void createPlaylist() { TextInputDialog d=new TextInputDialog("Nova Playlist"); d.showAndWait().ifPresent(n->{ Playlist p=new Playlist(n); appData.playlists.add(p); playlistList.getItems().add(p); saveData(); }); }
    @FXML private void showLibrary() { viewLibrary.setVisible(true); viewSettings.setVisible(false); }
    @FXML private void showSettings() { viewLibrary.setVisible(false); viewSettings.setVisible(true); }
    @FXML public void saveData() { DataManager.save(appData); }
    private String formatTime(Duration d) { return String.format("%02d:%02d", (int)d.toMinutes(), (int)d.toSeconds()%60); }
}