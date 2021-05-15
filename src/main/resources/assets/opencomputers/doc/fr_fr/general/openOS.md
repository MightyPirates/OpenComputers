# OpenOS

OpenOS est un système d'exploitation basique disponible dans OpenComputers. Il est nécessaire pour faire démarrer un [ordinateur](computer.md) pour la première fois, et peut être fabriqué en plaçant une [disquette](../item/floppy.md) vide et un manuel d'OpenComputers dans une table de craft.

Une fois créée, la [disquette](../item/floppy.md) peut être placée dans un [lecteur de disquettes](../block/diskDrive.md) connectée à un système [informatique](computer.md) [correctement configuré](quickstart.md), ce qui permettra à l'[ordinateur](computer.md) de lancer OpenOS.
Une fois démarré, il est conseillé d'installer OpenOS sur un [disque dur](../item/hdd1.md) vierge, ce qui évite la nécessité d'une [disquette](../item/floppy.md) et donne accès à un système de fichiers en lecture/écriture (la [disquette](../item/floppy.md) d'OpenOS et les autres disquettes "trouvées" sont en lecture seule). Un [boîtier](../block/case3.md) de niveau 3 n'a pas besoin de [lecteur de disquettes](../block/diskDrive.md), car il a un emplacement intégré pour les [disquettes](../item/floppy.md).

OpenOS peut être installé en écrivant simplement `install`, et en suivant les informations affichées à l'écran pour finaliser l'installation. La [disquette](../item/floppy.md) peut être retirée une fois que le système a été redémarré. OpenOS peut être installé sur tous les appareils sauf les [drones](../item/drone.md) et les [micro-contrôleurs](../block/microcontroller.md) (ils nécessitent tous les deux une programmation manuelle d'une [EEPROM](../item/eeprom.md) pour fournir des fonctionnalités, parce qu'ils n'ont pas de système de fichiers intégré).

OpenOS a de nombreuses fonctions intégrées, la plus utile d'entre elles étant la commande `lua`, qui ouvre un interpréteur Lua. C'est un bon endroit de test pour essayer diverses commandes, et expérimenter l'API des composants, avant d'écrire les commandes dans un script .lua. Retenez l'information affichée lors du démarrage de l'interpréteur, elle vous expliquera comment afficher le résultat des commandes que vous entrez, et comment en sortir.

Pour plus d'information sur la programmation, référez vous à la page sur la [programmation en Lua](lua.md). Pour exécuter des scripts Lua, saisissez simplement le nom du fichier et appuyez sur Entrée (par exemple, `script.lua` peut être exécuté en tapant la commande `script` dans le terminal).
