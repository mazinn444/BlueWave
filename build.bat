echo building...
mvn clean package

echo testing...
java -jar target/BlueWave-1.0.0.jar

echo compiling to exe
jpackage --type app-image --input target --name "BlueWave" --main-jar BlueWave-1.0.0.jar --main-class com.music.Launcher --icon icon.ico --dest dist --win-dir-chooser --win-menu --win-shortcut