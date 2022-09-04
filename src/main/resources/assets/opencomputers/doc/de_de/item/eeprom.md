# EEPROM

![Let's get this party started.](oredict:opencomputers:eeprom)

Der EEPROM enthält den Code der verwendet wird um den Computer zu starten. Diese Daten sind als einfaches Bytearray gespeichert und können bei unterschiedlichen Architekturen andere Operationen auslösen. Das LUA-BIOS ist ein kleines Script das auf dem Dateisystem nach eine Datei namens `init.lua`. Auf anderen Architekturen kann es echter Maschinencode sein.

EEPROMs können für spezialisierte Aufgaben programmiert werden, wie es bei [Drohnen](drone.md) oder [Microcontrollern](../block/microcontroller.md) der Fall ist.
