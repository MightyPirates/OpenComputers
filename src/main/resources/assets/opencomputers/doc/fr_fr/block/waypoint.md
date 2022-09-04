# Point de passage

!["Par là !" - "Non, par là !"](oredict:opencomputers:waypoint)

Le point de passage n'a aucune utilité en soi, mais dans la façon dont il peut être utilisé. Les [améliorations de navigation](../item/navigationUpgrade.md) peuvent détecter les points de passage, ainsi les appareils équipés d'une amélioration de navigation peuvent utiliser ces points de passage pour parcourir le monde. C'est particulièrement utile pour écrire des programmes facilement ré-utilisables par des appreils comme les [robots](robot.md) et les [drones](../item/drone.md).

Remarquez que la position réelle renvoyée lors de la requête par l'amélioration de navigation est *le bloc en face du point de passage* (indiqué par les effets de particule). De cette manière vous pouvez le placer au dessus d'un coffre, et vous référer à la position du point de passage comme étant "au dessus du coffre", sans avoir besoin de prendre en compte dans votre programme la rotation du point de passage.

Un point de passage a deux propriétés qui peuvent être utilisées en l'interrogeant avec une amélioration de navigation : le puissance actuelle du signal de redstone qu'il reçoit, et un libellé éditable. Le libellé est une chaîne de caractères de 32 caractères qui peut être éditée soit par l'interface soit par le composant exposé par le bloc du point de passage. Ces deux propriétés peuvent ensuite être utilisée sur l'appareil pour déterminer que faire avec le point de passage. Par exemple, un programme de tri pourrait être configuré pour traiter tous les blocs avec un signal de redstone élevé en tant qu'entrée, et ceux avec un signal faible comme une sortie.
