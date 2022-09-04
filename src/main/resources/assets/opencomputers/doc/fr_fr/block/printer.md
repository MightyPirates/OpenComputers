# Imprimante 3D

![L'impression en 2D, c'est tellement dépassé.](oredict:opencomputers:printer)

Les imprimantes 3D vous permettent d'imprimer n'importe quel bloc de n'importe forme, avec n'importe quelle texture. Pour démarrer avec les imprimantes 3D, vous devrez placer un bloc d'imprimante 3D à côté d'un ordinateur. Cela vous donnera accès à l'API de composant `printer3d`, ce qui permet de paramétrer et d'imprimer des [modèles](print.md) en utilisant les fonction fournies.

Une manière plus simple d'installer une imprimante 3D est d'utiliser le gestionnaire de paquets Open Programs (OPPM). Une fois installé (`oppm install oppm`), assurez vous d'avoir une [carte internet](../item/internetCard.md) dans votre [ordinateur](../general/computer.md) and lancez la commande suivante :
`oppm install print3d-examples`

Vous pouvez trouver des exemples dans `/usr/share/models/` sous forme de fichiers .3dm. Jetez un oeil dans ces fichiers d'exemple pour voir les options disponibles, en particulier le fichier `example.3dm`. Par ailleurs, vous pouvez télécharger les programmes `print3d` et `print3d-examples` depuis [OpenPrograms](https://github.com/OpenPrograms) en utilisant `wget` et une [carte internet](../item/internetCard.md).

Afin d'imprimer les modèles, une imprimante 3D doit être configurée par un [ordinateur](../general/computer.md). S'il est mis en mode sans-arrêt, l'ordinateur ne sera plus requis par la suite. Vous devrez également lui fournir une [cartouche d'encre](../item/inkCartridge.md) et un peu de [chamélium](../item/chamelium.md) pour les matières premières. La quantité de chamélium utilisée dépend du volume de l'impression 3D, tandis que la quantité d'encre utilisée dépend de la surface de l'objet imprimé.

Pour imprimer un objet, utilisez la commande suivante :
`print3d /path/to/file.3dm`
en fournissant le chemin vers le fichier .3dm.

Vous pourrez trouver la documentation relative à la création de vos propres modèles dans `/usr/share/models/example.3dm`.
