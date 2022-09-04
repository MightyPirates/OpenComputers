# EEPROM

![Let's get this party started.](oredict:opencomputers:eeprom)

L'EEPROM est ce qui contient le code utilisé pour initialiser un ordinateur quand il démarre. Les données sont stockées dans un tableau d'octets, et peuvent avoir différentes significations pour différentes architectures de [processeur](cpu1.md). Par exemple, pour Lua, c'est généralement un petit script qui recherche les systèmes de fichier avec un script d'initialisation, pour d'autres architectures ça pourrait être du code machine.

Les EEPROMs peuvent être programmées pour un usage spécifique, comme les [drones](drone.md) et les [microcontrôleurs](../block/microcontroller.md).
