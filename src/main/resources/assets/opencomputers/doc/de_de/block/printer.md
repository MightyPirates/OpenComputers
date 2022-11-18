# 3D-Drucker

![2D printing is so yesteryear.](oredict:opencomputers:printer)

3D-Drucker erlauben es, Blöcke von jeder Form mit jeder Art von Textur zu drucken. Um mit 3D-Druckern anzufangen wird ein 3D-Drucker und ein Computer benötigt. Dadurch erhält man Zugriff auf die `printer3d`-Komponenten-API. Hiermit können [Modelle](print.md) erstellt und gedruckt werden.

Ein bequemerer Weg, 3D-Drucker aufzusetzen ist es, den Open Programs Package Manager zu verwenden. Sobald er installiert ist (`oppm install oppm`) wird eine [Internetkarte](../item/internetCard.md) im [Computer](../general/computer.md) benötigt. Mit folgendem Befehl 
`oppm install print3d-examples`
können Beispiele für 3D-Modelle installiert werden.

Diese Beispiele können in `/usr/share/models/` als .3dm-Dateien gefunden werden. Besonders gilt es hier, die `example.3dm`-Datei zu beachten. Alternativ können `print3d` und `print3d-examples` auch mittels `wget` von OpenPrograms heruntergeladen werden. Dies erfordert ebenfalls eine [Internetkarte](../item/internetCard.md).

Um diese Modelle drucken zu können muss ein 3D-Drucker mit einem [Computer](../general/computer.md) konfiguriert werden. Wenn der Drucker auf Non-Stop gesetzt wird, wird der Computer danach nicht mehr benötigt. Auch eine [Druckerpatrone](../item/inkCartridge.md) und ein bisschen [Chamelium](../item/chamelium.md) wird als Ausgangsmaterial benötigt. Die Menge an Chamelium hängt vom Volumen des Drucks ab, während die Menge der benötigten Tinte von der Oberfläche des gedruckten Items abhängt.

Um etwas zu drucken wird der Befehl `print3d /pfad/zur/Datei.3dm` verwendet, wobei der Pfad zur 3DM-Datei angegeben werden muss.

Die Dokumentation über die Erstellung benutzerdefinierter Modelle kann in `/usr/share/models/example.3dm` gefunden werden.
