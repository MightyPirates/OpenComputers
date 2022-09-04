# Amélioration de vol plané

![Flotter comme une plume.](oredict:opencomputers:hoverUpgrade1)

L'amélioration de vol plané permet aux [robots](../block/robot.md) de voler beaucoup plus au dessus du sol que ce qu'ils pourraient le faire normalement. Contrairement aux [drones](drone.md), ils sont limités par défaut à une hauteur de vol de 8 blocs. Ca n'est généralement pas un problème, parce qu'ils peuvent tout de même monter aux murs. Leurs règles de déplacement peuvent être résumées ainsi :
- Les robots peuvent seulement bouger si la position de départ ou d'arrivée est valide (par exemple pour un pont).
- La position en dessous d'un robot est toujours valide (ils peuvent toujours descendre).
- Les positions jusqu'à `flightHeight` au dessus d'un bloc solide sont valides (capacités de vol limitées).
- N'importe quelle position avec un bloc adjacent ayant une face solide tournée vers la nouvelle position est valide (les robots peuvent "grimper").

Ces règles, à part la règle 2 pour la clarté (ils peuvent toujours descendre), peuvent être visualisées comme ça :
![Visualisation des règles de déplacement des robots.](opencomputers:doc/img/robotMovement.png)

Si vous ne souhaitez pas vous préoccuper des limitations de hauteur de vol, ces améliorations sont ce que vous cherchez.
