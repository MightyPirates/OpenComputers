# Schnellstart-Guide

Auch bekannt als "Wie man seinen ersten Computer baut". Um deinen ersten [Computer](computer.md) zu starten, musst du ihn zuerst korrekt aufbauen. Es gibt verschiedene Typen von Computern in OpenComputers, aber starten wir zuerst mit dem einfachsten: Dem Standardcomputer.

**Disclaimer**: Der Guide ist Schritt für Schritt und hilft beim Finden von Fehlern, daher ist er recht lang. Wenn du noch nie einen Computer im echten Leben gebaut hast, oder diesen Mod zum ersten Mal spielst ist es empfohlen, den ganzen Text zu lesen.

Zuerst wirst du ein [Computergehäuse](../block/case1.md) benötigen. Dies sit der Block der alle Komponenten benötigt und das Verhalten des gebauten Computers bestimmen.

![Ein Stufe-2-Computergehäuse.](oredict:opencomputers:case2)

Zum Beispiel musst du bestimmen, welche Stufe die [Grafikkarte](../item/graphicsCard1.md) du benötigst, ob eine [Netzwerkkarte](../item/lanCard.md) oder eine [Redstonekarte](../item/redstoneCard1.md) benötigt wird oder, wenn du im Creative-Mode spielst, eine [Debugkarte](../item/debugCard.md) gebraucht wird.

Wenn du die Oberfläche des Computergehäuses öffnest, wirst du rechts einige Slots sehen. Die Anzahl der Slots und von welchen Stufen die Komponenten sein können (siehe die kleinen römischen Nummern im Slot) hängt vom Computergehäuse selbst ab.

![Grafische Oberfläche eines Stufe-2-Computergehäuses.](opencomputers:doc/img/configuration_case1.png)
Ohne Komponenten sind Computergehäuse unnütz. Du kannst versuchen den Computer zu starten, aber er wird sofort eine Fehlermeldung in den Chatlog schreiben und sich durch ein Piepen bemerkbar machen. Zum Glück steht in der Fehlermeldung was du tun kannst um das Problem zu lösen: Der Computer benötigt Energie. Der Computer kann entweder direkt oder über einen [Power Converter](../block/powerConverter.md) an eine Energiequelle angeschlossen werden.

Wenn du den Computer nun starten möchtest, wird er um eine [CPU](../item/cpu1.md) bitten. Diese werden in verschiedenen Stufen ausgeliefert. Dies ist ein Modell, das in OpenComputers häufig auftritt. Hochstufige CPUs ermöglichen mehr Komponenten und schnellere Ausführung von Programmen, also wähle eine Stufe und stecke die CPU in deinen Computer.

Danach wird der Computer nach [Random Access Memory (RAM)](../item/ram1.md) fragen. Der Piep-Code ist jetzt unterschiedlich: lang-kurz. Hochstufige RAM-Riegel bedeuten mehr für Programme verfügbarer Speicher. Für [OpenOS](openOS.md) werden mindestens zwei Stufe-1-RAM-Riegel benötigt. 

Wir machen einen guten Fortschritt. Dein Computer wird jetzt in etwa so aussehen:
![Teilweise konfigurierter Computer.](opencomputers:doc/img/configuration_case2.png)
Den Computer jetzt einzuschalten produziert keine Fehlermeldungen! Aber leider tut er auch nicht sehr viel. Zumindest piept er jetzt zwei mal. Dies bedeutet, dass der Computer selbst läuft, allerdings läuft auf dem Computer nichts. Hier kommt ein sehr nützliches Tool zum Einsatz: Das [Analysegerät](../item/analyzer.md). Es ermöglicht dir, viele OpenComputers-Blöcke und einige Blöcke anderer Mods zu analysieren. Um es auf den Computer anzuwenden verwende das Analysegerät beim Schleichen auf das Gehäuse.

Und jetzt solltest du sehen, dass der folgende Fehler den Computer zum Absturz gebracht hat:
`no bios found; install configured EEPROM`

Die Betonung liegt auf *configured*. Einen [EEPROM](../item/eeprom.md) anzufertigen ist recht einfach, um es zu konfigurieren wirst du einen Computer verwenden. Da das aber derzeit Probleme bereitet, werden wir mittels eines Rezeptes einen konfigurierten "Lua BIOS"-[EEPROM](../item/eeprom.md) anfertigen. Das Standardrezept ist ein [EEPROM](../item/eeprom.md) und ein [Handbuch](../item/manual.md). Lege das konfigurierte EEPROM in deinen Computer, uuuuuuund...

Nein. Immer noch nichts. Aber wir wissen was zu tun ist! Spieler setzt Messgerät ein. Das ist sehr effektiv! Jetzt haben wir eine andere Fehlermeldung:
`no bootable medium found; file not found`

Na gut, das bedeutet, dass das BIOS funktioniert. Es findet nur noch kein bootbares Dateisystem, so wie eine [Diskette](../item/floppy.md), oder eine [Festplatte](../item/hdd1.md). Das LUA BIOS erwartet ein solches Dateisystem um eine Datei namens `init.lua` in der Wurzel des Dateisystems zu finden. Der [EEPROM](../item/eeprom.md) schreibt in der Regel auf das Dateisystem des Computers. Du hast es wahrscheinlich schon erraten: Wir müssen unsere eigene Betriebssystemdiskette anfertigen. Dies erreichst du, indem du eine leere [Diskette](../item/floppy.md) und ein Handbuch zusammen in die Werkbank legst. Das Ergebnis ist eine [OpenOS-Diskette](openOS.md)

Wenn du, wie in den Screenshots oben, ein Stufe-2-Gehäuse verwendet hast gibt es keinen Ort um diese Diskette einzusetzen. Bei einem Stufe-3- oder Kreativ-Gehäuse kannst du die Diskette direkt in das Gehäuse einlegen, anderenfalls benötigst du ein [Diskettenlaufwerk](../block/diskDrive.md) neben deinem Gehäuse. (Auch möglich ist eine Verbindung über [Kabel](../block/cable.md). Sobald die Diskette eingesetzt ist, weißt du was zu tun ist - drücke den Startknopf!

Es lebt! Zumindest sollte es das. Wenn es das nicht tut lief etwas falsch, und das [Analysegerät](../item/analyzer.md) wird bei der Fehlersuche helfen. Wenn der Computer jetzt läuft bist zu weitgehend fertig und der schwerste Teil ist vorbei. Du musst es nur noch mit einem 

Um die Ausgabe des Computers zu lesen wird ein [Bildschirm](../block/screen1.md) und eine Grafikkarte benötigt.
![Nein. Es ist kein Flachbildschirm](oredict:opencomputers:screen2)

Platziere den Bildschirm entweder direkt neben den Computer oder verbinde sie mit einem Kabel. Setze eine Grafikkarte deiner Wahl in den Computer ein. Nun sollte ein blinkender Cursor auf dem Bildschirm sichtbar sein. Jetzt fehlt nur noch eine [Tastatur](../block/keyboard.md) entweder direkt auf dem Bildschirm oder neben dem Bildschirm (mit Ausrichtung auf diesen). 

Damit bist du fertig. Der Computer läuft - probiere ihn gleich aus! Schreibe `lua` in die Kommandozeile und dir werden einige Informationen über den Lua-Interpreter präsentiert. Hier kannst du grundlegende [Lua-Methoden](lua.md) ausführen.

![Es lebt~](opencomputers:doc/img/configuration_done.png)

Viel Spaß beim Bauen komplexerer Computer. Probiere unbedingt auch [Server](../item/server1.md), [Roboter](../block/robot.md), [Drohnen](../item/drone.md), [Mikrocontroller](../block/microcontroller.md) oder [Tablets](../item/tablet.md) aus. Du kannst sie mit der [Elektronik-Werkbank](../block/assembler.md) bauen!

Fröhliches Programmieren!
