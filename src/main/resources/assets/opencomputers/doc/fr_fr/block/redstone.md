# Redstone E/S

![Salut Red.](oredict:opencomputers:redstone)

Le bloc d'E/S de redstone peut être utilisé pour lire et émettre des signaux de redstone à distance. Il se comporte comme un hybride des [cartes de redstone](../item/redstoneCard1.md) de niveau 1 et 2 : il peut aussi bien lire et émettre des signaux analogiques que des signaux empaquetés (bundle), mais il ne peut pas lire ou émettre de signaux redstone sans-fil.

En indiquant un côté aux méthodes du composant exposé par ce bloc, les directions sont les points cardinaux, c'est à dire qu'il est recommandé d'utiliser `sides.north`, `sides.east`, etc.

De même que les [cartes de redstone](../item/redstoneCard1.md), ce bloc injecte un signal dans les [ordinateurs](../general/computer.md) connectés quand l'état du signal de redstone change - autant pour les signaux analogiques qu'empaquetés (bundle). Ce bloc peut également être configuré pour démarrer des [ordinateurs](../general/computer.md) connectés quand une certaine puissance de signal est dépassée, ce qui permet de démarrer automatiquement des [ordinateurs](../general/computer.md).
