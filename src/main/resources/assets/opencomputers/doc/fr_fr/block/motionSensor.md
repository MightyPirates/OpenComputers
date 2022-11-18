# Détecteur de mouvement

![Ne. Clignez. Pas. Des. Yeux.](oredict:opencomputers:motionSensor)

Le détecteur de mouvement permet aux [ordinateurs](../general/computer.md) de détecter le mouvement des entités vivantes. Si une entité se déplace plus vite qu'un seuil défini, un signal sera injecté dans les [ordinateurs](../general/computer.md) connectés au détecteur de mouvement. Le seuil peut être configuré en utilisant l'API des composants que le détecteur de mouvement expose aux ordinateurs connectés.

Le mouvement est seulement détecté s'il survient dans un rayon de huit blocs autour du détecteur de mouvement, et s'il y a une ligne de mire directe entre le bloc et l'entité qui a bougé.
