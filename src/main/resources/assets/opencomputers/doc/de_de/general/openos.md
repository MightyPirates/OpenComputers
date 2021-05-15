# OpenOS

OpenOS ist ein einfaches Betriebssystem von OpenComputers. Es wird benötigt um einen [Computer](computer.md) zum ersten Mal hochzufahren, und kann angefertigt werden, indem man eine leere [Diskette](../item/floppy.md) und das OpenComputers-[Handbuch](../item/manual.md) in eine Werkbank legt.

Sobald sie gefertigt wurde, kann die [Diskette](../item/floppy.md) in einem [Diskettenlaufwerk](../block/diskDrive.md) verwendet werden, welches mit einem [korrekt konfigurierten](quickstart.md) [Computer](computer.md) verbunden ist. Dieser kann dadurch mit OpenOS hochfahren.
Sobald der Computer hochgefahren wurde, ist eine Installation auf eine leere [Festplatte](../item/hdd1.md) zu empfehlen. Dies ermöglicht es, die [Diskette](../item/floppy.md) zu entfernen und einen Zugriff auf ein schreibbares Dateisystem zu erhalten. (Die OpenOS-[Disketten](../item/floppy.md) und andere Disketten können nur gelesen werden). Ein Stufe-Drei-[Gehäuse](../block/case3.md) benötigt kein [Diskettenlaufwerk](../block/diskDrive.md), da es ein Slot für eine [Diskette](../item/floppy.md) eingebaut hat.

OpenOS kann einfach installiert werden, indem `install` ausgeführt wird. Die [Diskette](../item/floppy.md) kann entfernt werden, sobald das System hochgefahren wurde. OpenOS kann auf allen Geräten außer [Drohnen](../item/drone.md) und [Mikrocontrollern](../block/microcontroller.md) installiert werden. (Diese benötigen eine manuelle Programmierung eines [EEPROM](../item/eeprom.md) um Funktionen bereitzustellen, da sie kein eingebautes Dateisystem besitzen.)

OpenOS hat viele eingebaute Funktionen, wovon die nützlichste der `lua`-Befehl ist. Hier wird ein Lua-Interpreter geöffnet. Dies ist ein guter Ort, um verschiedene Befehle auszuprobieren und mit der Komponenten-API zu experimentieren, bevor die Befehle in ein Lua-Script geschrieben werden. Beachte die Informationen welche der Interpreter zeigt, diese zeigen die Ergebnisse der eingegebenen Befehle und wie sie zu beenden sind.

Für mehr Informationen über das Programmieren siehe die [Seite über Lua-Programmierung](lua.md). Um Lua-Scripte auszuführen muss nur der Name der Datei eingegeben und die Enter-Taste betätigt werden (zum Beispiel kann die Datei `script.lua` mit dem Befehl `script` im Terminal ausgeführt werden).
