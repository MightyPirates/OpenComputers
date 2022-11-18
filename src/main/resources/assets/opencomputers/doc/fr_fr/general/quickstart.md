# Bien démarrer

Aussi connu en tant que "comment construire votre premier ordinateur". Pour faire démarrer votre premier [ordinateur](computer.md), vous aurez d'abord besoin de l'installer correctement. Il y a plusieurs types différents d'ordinateurs dans OpenComputers, mais commençons avec la base : l'ordinateur standard.

**Avertissement**: ce sera un pas-à-pas, et il fournira des informations sur la manière de chercher des questions vous-même plus tard, donc ça sera plutôt long. Si vous n'avez jamais construit d'ordinateur dans la vie réelle, et/ou que vous êtes complètement nouveau sur ce mod, il est vivement recommandé que vous lisiez tout ceci.

Premièrement, vous aurez besoin d'un [boîtier](../block/case1.md). C'est le bloc qui contiendra tous les composants, en définissant le comportement de l'ordinateur que vous construisez.

![Un boîtier d'ordinateur de niveau 2.](oredict:opencomputers:case2)

Par exemple, vous aurez besoin de choisir quel niveau de [carte graphique](../item/graphicsCard1.md) vous voulez utiliser, si vous avez besoin d'une [carte réseau](../item/lanCard.md), une [carte de redstone](../item/redstoneCard1.md) ou, si vous faites des essais en mode créatif, peut être même une [carte de débogueur](../item/debugCard.md).

Quand vous ouvrez l'interface du [boîtier d'ordinateur](../block/case1.md), vous verrez quelques emplacements à droite. Le nombre d'emplacements, et le niveau des composants qui peuvent être placés dedans (indiqué par le petit chiffre romain dans l'emplacement) dépend du niveau du boîtier lui-même.
![Interface d'un boîtier de niveau 2.](opencomputers:doc/img/configuration_case1.png)
Quand ils sont vides, les [boîtiers](../block/case1.md) sont assez inutiles. Vous pouvez essayer d'allumer votre [ordinateur](computer.md), mais il affichera immédiatement un message d'erreur dans votre tchat, et fera entendre son mécontentement en vous bipant dessus. Heureusement que le message d'erreur vous dit quoi faire pour résoudre la situation : il demande de l'énergie. Connectez votre [ordinateur](computer.md) à un peu d'énergie, soit directement soit via un [convertisseur énergétique](../block/powerConverter.md).

Maintenant, si vous essayez de le démarrer, il vous dira qu'il a besoin d'un [processeur](../item/cpu1.md). Il en existe de différents niveaux - une tendance dont vous remarquerez la présence partout dans OpenComputers. Pour les [processeurs](../item/cpu1.md), un meilleur niveau signifie plus de composants à la fois, ainsi qu'une vitesse plus élevée. Donc choississez un niveau, et mettez le dans votre [boîtier](../block/case1.md).

Ensuite, il vous sera demandé d'insérer de la [mémoire (RAM)](../item/ram1.md). Remarquez que le bip est différent maintenant : long-court. De plus hauts niveaux de [mémoire (RAM)](../item/ram1.md) permettent plus de mémoire disponible pour les programmes s'exécutant sur votre [ordinateur](computer.md). Pour lancer [OpenOS](openOS.md), ce qui est le but de cette introduction, vous devrez utiliser au moins deux barettes de [mémoire (RAM)](../item/ram1.md) de niveau 1.

On progresse bien. Pour le moment, votre [boîtier](../block/case1.md) ressemblera à quelque chose comme ça :
![Un ordinateur partiellement configuré.](opencomputers:doc/img/configuration_case2.png)
Et voilà, mettre l'ordinateur sous tension n'affichera plus de message d'erreur ! Hélas, il ne fait quand même pas grand chose. Au moins, il émet 2 bips. Ce qui veut dire que le lancement de l'[ordinateur](computer.md) a échoué. En d'autres termes : techniquement, il fonctionne ! C'est là qu'intervient un outil très utile : l'[analyseur](../item/analyzer.md). Cet outil permet d'inspecter beaucoup de blocs d'OpenComputers, ainsi que quelques blocs d'autres mods. Pour l'utiliser sur l'[ordinateur](computer.md), utilisez l'[analyseur](../item/analyzer.md) sur le boîtier en vous accroupissant.

Vous devriez maintenant voir l'erreur qui a causé le crash de l'[ordinateur](computer.md) :
`no bios found; install configured EEPROM`
(aucun BIOS trouvé; installez une EEPROM configurée)

L'accent ici est mis sur *configured*. Crafter une [EEPROM](../item/eeprom.md) est plutôt simple. Pour la configurer, vous aurez généralement à utiliser un [ordinateur](computer.md) - mais c'est un peu compliqué pour le moment, donc on va utiliser une recette pour fabriquer une [EEPROM](../item/eeprom.md) configurée nommée "Lua BIOS". La recette classique est une [EEPROM](../item/eeprom.md) et un [manuel](../item/manual.md). Mettez l'[EEPROM](../item/eeprom.md) configurée dans l'[ordinateur](computer.md), eeeeet...

Non. Toujours rien. Mais nous savons quoi faire : le joueur utilise l'[analyseur](../item/analyzer.md), c'est super efficace ! Maintenant nous avons un message d'erreur différent :
`no bootable medium found; file not found`
(pas de média bootable; fichier non trouvé)

Bien. Cela veut dire que le BIOS fonctionne. C'est juste qu'il ne trouve pas de système de fichier à partir duquel il peut démarrer, comme une [disquette](../item/floppy.md) ou un [disque dur](../item/hdd1.md). Le BIOS Lua en particulier attend un tel système de fichier pour y trouver un fichier nommé `init.lua` à la racine. Avec l'[EEPROM](../item/eeprom.md), vous écrivez généralement dans un système de fichier avec un [ordinateur](computer.md). Vous l'avez probablement deviné : nous devons maintenant fabriquer notre disque de système d'exploitation. Prenez une [disquette](../item/floppy.md) vierge et un [manuel](../item/manual.md), assemblez les, et vous obtiendrez une disquette d'[OpenOS](openOS.md).

Maintenant, si vous avez utilisé un [boîtier](../block/case2.md) de niveau 2 comme dans les captures d'écran si dessus, vous n'aurez nulle part où mettre cette disquette. Si vous avez un [boîtier](../block/case2.md) créatif ou de niveau 3, vous pouvez placer la disquette directement dans le [boîtier](../block/case1.md). Sinon vous devrez mettre un [lecteur de disquettes](../block/diskDrive.md) à côté du boîtier (ou le connecter avec des [câbles](../block/cable.md)). Une fois votre disquette insérée, vous savez quoi faire. Appuyez sur le bouton d'alimentation.

Il vit ! Ou il devrait, en tout cas. Si ce n'est pas le cas quelque chose ne va pas, et vous devriez investiguer en utilisant l'[analyzer](../item/analyzer.md). Mais en supposant qu'il fonctionne maintenant, vous avez presque fini. La partie la plus compliquée est terminée. Tout ce qui reste à faire est de lui permettre d'accepter des entrées, et d'afficher des sorties.

Pour permettre à l'[ordinateur](computer.md) d'afficher des informations, vous devrez vous munir d'un [écran](../block/screen1.md) et d'une [carte graphique](../item/graphicsCard1.md).
![Non, ce n'est pas un écran plat.](oredict:opencomputers:screen2)

Placez l'[écran](../block/screen1.md) à côté de votre [boîtier](../block/case1.md) ou, à nouveau, connectez le en utilisant des [câbles](../block/cable.md). Puis placez une [carte graphique](../item/graphicsCard1.md) de votre choix dans le [boîtier](../block/case1.md). Vous devriez maintenant voir un curseur clignotant sur l'[écran](../block/screen1.md). Finalement, placez un [clavier](../block/keyboard.md) soit sur l'[écran](../block/screen1.md) lui-même, soit juste en face de l'[écran](../block/screen1.md), pour activer l'entrée de données au [clavier](../block/keyboard.md).

Avec ça, vous avez fini. L'[ordinateur](computer.md) fonctionne et prêt à l'action. Essayez le maintenant ! Tapez `lua` dans l'interface système et appuyez sur Entrée, et vous serez accueilli par quelques informations sur la manière d'utiliser l'interpréteur Lua. Vous pouvez y tester des commandes Lua de base. Pour plus d'informations à ce sujet, allez sur [la page Lua](lua.md)

![Il vit!](opencomputers:doc/img/configuration_done.png)

Amusez vous à construire des [ordinateurs](computer.md) plus complexes, jouer avec des [serveurs](../item/server1.md) et assembler des [robots](../block/robot.md), des [drones](../item/drone.md), des [micro-contrôleurs](../block/microcontroller.md) et des [tablettes](../item/tablet.md) dans l'[assembleur électronique](../block/assembler.md).

Bon code !
