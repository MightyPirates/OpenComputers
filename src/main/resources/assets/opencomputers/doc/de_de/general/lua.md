# Lua 

Die Lua-[Dokumentation (englisch)](http://www.lua.org/manual/5.2/manual.html) und die [Programming in Lua](http://www.lua.org/pil/)-Bücher (englisch) (die erste Ausgabe ist online kostenlos verfügbar) sind eine gute Anlaufstelle für die Grundlagen von Lua und zum Verständnis der Syntax und Standardbibliotheken der Sprache. [OpenOS](openOS.md) versucht die Standardbibliotheken so gut wie möglich mit ein paar Unterschieden (wie zum Beispiel die großteils fehlende Debugging-Bibliothek) zu emulieren. Diese Unterschiede sind [im Wiki dokumentiert (englisch)](https://ocdoc.cil.li/api:non-standard-lua-libs).

Nicht standardisierte Bibliotheken müssen `require`-d werden, um sie in einem Script zu verwenden. Zum Beispiel:

`local component = require("component")`
`local rs = component.redstone`

Dies ermöglicht es, alle Funktionen der [Redstone-Karte](../item/redstoneCard1.md) zu verwenden. Zum Beispiel:

`rs.setOutput(require("sides").front, 15)`

**Wichtig**: Wenn in einem Lua-Interpreter gearbeitet wird, ist *unter keinen Umständen* `local` zu verwenden. Dies sorgt dafür, dass die Variable nur in einer Zeile Input verfügbar ist. Wenn die obigen Zeilen eine nach der anderen in den Interpreter eingefügt werden würden, würde die dritte Zeile fehlschlagen, da `rs` den Wert `nil` hätte. Wieso? Aus Testgründen versucht der Interpreter unbekannte Variablen als Bibliotheken zu laden. Obwohl die Zuweisung zu `component` der ersten Zeile nichts tun würde, würde die Verwendung von `component` in der zweiten Zeile die Bibliothek laden und diese verwenden. Bibliotheken werden in Lua-Scripts jedoch nicht automatisch verwendet um den Speicherverbrauch niedrig zu halten, da es eine sehr begrenzte Ressource ist.

OpenOS stellt viele eigene Bibliotheken zur Verfügung, welche für viele Programme (vom Kontrollieren und Manipulieren von angeschlossenen Komponenten bis zu Referenz-APIS für Farben in gebündelter Redstonekontrolle und [Tastatur](../block/keyboard.md)-Keycodes.). Benutzerdefinierte Libraries können innerhalb eines Lua-Scripts mittels der `require()`-Funktion verwendet werden. Einige Bibliotheken benötigen bestimmte Komponenten, wie die `internet`-Library eine [Internetkarte](../item/internetCard.md) benötigt. In diesem speziellen Fall wird die Bibliothek sogar von der Karte bereitgestellt, was bedeutet, dass die Bibliothek auftaucht, sobald die Internetkarte installiert wird. Die API ist praktisch auf einem kleinen, nur lesbaren Dateisystem auf der Internetkarte enthalten.
