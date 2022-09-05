# Tablette

![Touche moi si tu peux.](item:opencomputers:tablet)

Les tablettes sont faites en mettant un [boîtier de tablette](tabletCase1.md) dans un [assembleur](../block/assembler.md), en le configurant comme vous le voulez et en assemblant. Les tablettes se comportent comme des ordinateurs portables qui ne peuvent pas interagir directement avec le monde - par exemple, les [cartes de redstone](redstoneCard1.md) de base ne fonctionnent pas avec une tablette. Cependant, un certain nombre d'améliorations fonctionne, comme l'[amélioration de panneau d'E/S](signUpgrade.md) ou l'[amélioration de piston](pistonUpgrade.md).

La tablette de niveau 2 permet également d'installer une amélioration de conteneur. Il est possible d'accéder à l'emplacement fourni par le conteneur en ouvrant l'interface alternative de la tablette, en faisant un clic droit accroupi avec la tablette en main. Cela forcera également la tablette à s'éteindre.

A la différence des ordinateurs, les tablettes ne gèrent pas la persistence quand le joueur qui les tient quitte le jeu et y ré-accède. Elles ne gèrent pas non plus la persistence entre dimensions (un joueur qui va dans le Nether et qui en revient, pas exemple.

Les tablettes peuvent être mises dans un [chargeur](../block/charger.md) pour être rechargées, et pour accéder au premier [disque dur](hdd1.md) de la tablette depuis un [ordinateur](../general/computer.md) connecté au chargeur - dans cette situation, le chargeur se comportera comme un [lecteur de disquettes](../block/diskDrive.md), la tablette étant considérée comme la [disquette](floppy.md). Cela peut être très utile si vous oubliez d'installer un système d'exploitation sur le disque dur inclus dans la tablette, ou si vous avez bloqué le système d'exploitation d'une tablette.

Une autre fonctionnalité avancée de la tablette est sa capacité à générer des signaux avec des informations sur certains blocs du monde, en l'utilisant sur un bloc dans le monde pendant environ une seconde (c'est à dire qu'il faut laisser le bouton droit de la souris enfoncée pendant une seconde). A bip court vous préviendra alors que le signal a été généré. Cela fonctionne uniquement si des améliorations sont installées sur la tablette qui envoie le signal avec les informations. Par exemple, le [géoliseur](../block/geolyzer.md) ajoutera des informations sur le bloc en lui-même, comme sa dureté, l'[amélioration de navigation](navigationUpgrade.md) ajoutera les coordonnées du bloc relativement au joueur qui tient la tablette. Le bloc à analyser sera surligné en vert en tenant la tablette.
