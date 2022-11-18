# Mémoire

![Do you remember, dancing in September~](oredict:opencomputers:ram1)

La mémoire est, au même titre que le [processeur](cpu1.md), un composant essentiel à chaque [ordinateur](../general/computer.md). En fonction de l'architecture du [processeur](cpu1.md), la mémoire a un effet important sur ce que l'[ordinateur](../general/computer.md) peut ou ne peut pas faire. Pour l'architecture Lua standard, par exemple, elle contrôle la quantité réelle de mémoire que les scripts Lua peuvent utiliser. Cela signifie que pour exécuter des programmes demandant plus de ressources, vous aurez besoin de plus de mémoire.

La mémoire est disponible en différents niveaux avec les capacités suivantes, par défaut :
- Niveau 1: 192Ko
- Niveau 1.5: 256Ko
- Niveau 2: 384Ko
- Niveau 2.5: 512Ko
- Niveau 3: 768Ko
- Niveau 3.5: 1024Ko

Remarquez que ces valeurs s'appliquent seulement à l'architecture Lua. D'autres architectures peuvent fournir d'autres quantités de mémoire pour les différents niveaux. Remarquez également que les niveaux 1 et 1.5 sont tous les deux considérés comme de la mémoire de niveau 1, et il en va de même pour les barettes de mémoire de niveau 2 et 3.

Ces valeurs peuvent être changées dans la configuration.
