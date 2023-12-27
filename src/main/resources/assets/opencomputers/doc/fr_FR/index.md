# Manuel d'OpenComputers

OpenComputers est un mod qui ajoute au jeu des [ordinateurs](general/computer.md), des [serveurs](item/server1.md), des [robots](block/robot.md), et des [drones](item/drone.md) persistants, modulaires et très configurables. Tous les appareils peuvent être programmés en utilisant Lua 5.2, permettant d'avoir des systèmes à complexité variable en fonction de leur usage.

Pour apprendre à utiliser ce manuel, allez sur [la page parlant du manuel](item/manual.md) (Ce texte en vert est un lien, vous pouvez cliquer dessus).

## Table des matières

### Appareils
- [Ordinateurs](general/computer.md)
- [Serveurs](item/server1.md)
- [Micro-contrôleurs](block/microcontroller.md)
- [Robots](block/robot.md)
- [Drones](item/drone.md)

### Logiciel et programmation
- [OpenOS](general/openOS.md)
- [Lua](general/lua.md)

### Blocs et objets
- [Objets](item/index.md)
- [Blocs](block/index.md)

### Guides
- [Bien démarrer](general/quickstart.md)

## Vue d'ensemble

Comme indiqué plus haut, les ordinateurs d'OpenComputers sont persistants, ce qui signifie qu'un [ordinateur](general/computer.md) en fonctionnement garde son état quand le chunk dans lequel il se trouve est déchargé. Cela signifie que si un joueur s'éloigne de cet [ordinateur](general/computer.md), ou se déconnecte, l'[ordinateur](general/computer.md) se souviendra de son dernier état connu et reprendra à ce point quand le joueur s'en rapprochera. La persistance fonctionne pour tous les appareils sauf les [tablettes](item/tablet.md)

Tous les appareils sont modulaires et peuvent être assemblés avec une grande variété de composants, comme les [ordinateurs](general/computer.md) de la vie réelle. Les joueurs qui aiment bricoler seront capables d'optimiser leurs appareils comme ils le souhaitent. Si on le désire, les appareils peuvent être [démontés](block/disassembler.md) et reconstruits si la configuration initiale n'était pas satisfaisante. Pour les [ordinateurs](general/computer.md) et les [serveurs](item/server1.md), les composants peuvent être échangés à la volée simplement en ouvrant l'interface correspondante.

Les appareils d'OpenComputers sont compatibles avec différents mods pour la manipulation de blocs et d'entités (à travers l'[adaptateur](block/adapter.md), ou des améliorations spécifiques d'un [robot](block/robot.md) ou d'un [drone](item/drone.md)). L'énergie peut être fournie grâce à une large gamme de mods, incluant, sans limitation, les Redstone Flux (RF), les EU d'IndustrialCraft2, les Joules de Mekanism, l'énergie d'Applied Energistics 2 autant que la charge de Factorization.

Les appareils d'OpenComputers ont des fonctionnalités supplémentaires ainsi que quelques limitations. Les [ordinateurs](general/computer.md) sont la base, et sont capables de contenir un bon nombre de composants, contrôlés par le niveau du processeur utilisé. Les [ordinateurs](general/computer.md) ont également accès aux composants par leurs six faces. Les [serveurs](item/server1.md) sont capables de se connecter à plus de composants (en interne ou en externe) qu'un [ordinateur](general/computer.md), en utilisant des [bus de composants](item/componentBus1.md); cependant, à cause du [support de serveur](block/serverRack.md), le [serveur](item/server1.md) est seulement capable d'accéder aux composants par une unique face du [support de serveur](block/serverRack.md), tel que configuré dans l'interface du [server rack](block/serverRack.md). Les [micro-contrôleurs](block/microcontroller.md) sont encore plus limités (comparés aux [ordinateurs](general/computer.md)) par leur manque d'emplacements de [disque dur](item/hdd1.md) et de [lecteur de disquettes](block/diskDrive.md), ce qui veut dire qu'[OpenOS](general/openOS.md) ne peut pas être installé sur un [micro-contrôleur](block/microcontroller.md). Les [micro-contrôleurs](block/microcontroller.md) ont un emplacement pour une [EEPROM](item/eeprom.md), et peuvent être programmés avec un système d'exploitation plus spécifique pour un ensemble limité de tâches.

Les [robots](block/robot.md) sont des [ordinateurs](general/computer.md) mobiles, et sont capables d'interagir avec le monde (mais ne peuvent pas interagir avec d'autres blocs d'OpenComputers). Contrairement aux [ordinateurs](general/computer.md), une fois qu'un robot est construit, les composants à l'intérieur du [robot](block/robot.md) ne peuvent pas être retirés. Pour contourner cette limitation, les [robots](block/robot.md) peuvent être construits avec des conteneurs d'[amélioration](item/upgradeContainer1.md) ou de [carte](item/cardContainer1.md), ce qui permettra d'échanger à la volée des cartes ou des améliorations, si nécessaire. [OpenOS](general/openOS.md) peut être installé sur les [robots](block/robot.md) en plaçant un [lecteur de disquettes](block/diskDrive.md) dans un emplacement de conteneur, ce qui permettra l'insertion de [disquettes](item/floppy.md), ou en plaçant un [disque dur](item/hdd1.md) avec [OpenOS](general/openOS.md) pré-installé dans l'un des emplacements de [disque dur](item/hdd1.md). Pour complètement reconfigurer un [robot](block/robot.md), il devra être [désassemblé](block/disassembler.md) avant. Les [drones](item/drone.md) sont une version limitée des [robots](block/robot.md). Ils se déplacent différemment, ont moins d'emplacements d'inventaire, et n'ont pas de système d'exploitation (à l'instar des [micro-contrôleurs](block/microcontroller.md), les [drones](item/drone.md) peuvent être configurés avec une [EEPROM](item/eeprom.md) programmée pour un ensemble limité de tâches). Pour la plupart, les [robots](block/robot.md) et les [drones](item/drone.md) partagent les mêmes améliorations et composants; cependant, les améliorations se comportent différemment avec les [drones](item/drone.md), comme les [améliorations d'inventaire](item/inventoryUpgrade.md) qui fournissent seulement 4 emplacements par amélioration, pour un total de 8 emplacements, tandis que les [robots](block/robot.md) sont capables d'accepter plus d'[améliorations d'inventaire](item/inventoryUpgrade.md) (pour un total de 4) ainsi que l'obtention de plus d'emplacements par amélioration (16 emplacements par amélioration).

Ce manuel contient des informations détaillées concernant tous les blocs et objets, comment mettre en place différents types de systèmes et d'appareils, ainsi qu'une introduction à la programmation.
