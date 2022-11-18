# Amélioration de base de données

![Je suis dans la matrice!](oredict:opencomputers:databaseUpgrade1)

L'amélioration de base de données peut être configurée pour stocker une liste de représentations de groupes d'objets, qui peuvent ensuite être utilisés par d'autres composants. C'est particulièrement utile pour les objets qui sont uniquement distinguables par leurs données NBT, ce qui ne fait pas partie des données affichées de base pour un groupe d'objets.

Pour configurer une base de données, ouvrez la en faisant un clic droit avec la base de données dans la main. Placez les objets avec lesquels vous souhaitez la configurer dans l'inventaire du haut. Cela stockera un "objet fantôme", ce qui veut dire qu'aucun objet "réel" n'est stocké dans la base de données.

Autrement, la base de données peut être configurée automatiquement en utilisant l'API du composant fournie par les [améliorations du contrôleur d'inventaire](inventoryControllerUpgrade.md) et les [géolyseurs](../block/geolyzer.md).
