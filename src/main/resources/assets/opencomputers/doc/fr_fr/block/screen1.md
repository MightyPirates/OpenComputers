# Ecrans

![Tu vois ça ?](oredict:opencomputers:screen1)

Un écran est utilisé en combinaison avec une [carte graphique](../item/graphicsCard1.md), pour permettre aux [ordinateurs](../general/computer.md) d'afficher du texte. Les différents niveaux d'écrans ont différentes capacités, comme le support de différentes résolutions et couleurs. La qualité des écrans va d'une faible résolution en affichage monochrome à une haute résolution en 256 couleurs.

La résolution disponible et la profondeur de couleurs dépend du composant de plus bas niveau. Si vous utilisez une [carte graphique (niveau 1)](../item/graphicsCard1.md) avec un [écran (niveau 3)](screen3.md), seules les couleurs et la résolutions de niveau 1 seront disponibles. Cependant, en utilisant une [carte graphique](../item/graphicsCard1.md) de niveau 3 avec un écran de niveau 1, même si la résolution et la profondeur de couleur seront limitées au niveau 1, les opérations de la [carte graphique](../item/graphicsCard1.md) seront plus rapides qu'en utilisant une [carte graphique](../item/graphicsCard1.md) de niveau 1.

Les écrans peuvent être placés les uns contre les autres pour former un écran multi-bloc, tant qu'ils sont dans la même direction. S'ils sont placés vers le haut ou le bas ils doivent également être tournés dans le même sens. Leur orientation est indiquée par une flèche dans la barre d'inventaire en tenant l'écran en main.

La taille d'un écran n'a pas d'impact sur la résolution disponible, seulement son niveau. Pour gérer la manière dont les écrans se connectent entr eux, il est possible de les colorer avec n'importe quel colorant. Faites simplement un clic-droit sur l'écran avec un colorant en main. Le colorant ne sera pas consommé, mais les écrans ne garderont pas leur couleur une fois détruits. Des écrans de différente couleur ne se connecteront pas. Des écrans de différents niveaux ne connecteront jamais, même s'ils sont de la même couleur.

Les écrans de niveau 2 et 3 supportent également les entrées de la souris. Il est possible de cliquer soit dans l'interface de l'écran (qui peut seulement être ouvert si un [clavier](keyboard.md) est connecté à l'écran), soit en s'accroupissant pour cliquer dessus (avec la main vide dans le doute). Le fait de s'accroupir est optionnel si l'écran n'a pas de [clavier](keyboard.md). Remarquez qu'il est possible de contrôler l'ouverture de l'interface - ou en tout cas son activation - par l'API du composant exposée aux [ordinateurs](../general/computer.md). Les écrans de niveau 3 permettent une détection plus précise de l'endroit touché, si ça a été activé dans leur composant. Cela permet de détecter si la partie haute ou basse d'un caractère a été cliqué, par exemple, ce qui peut être utile si vous vous servez de caractères spéciaux en Unicode pour simuler de meilleures résolutions.

Les résolutions et profondeurs de couleur des écrans sont les suivantes :
- Tier 1: 50x16, couleur sur 1-bit
- Tier 2: 80x25, couleur sur 4-bit
- Tier 3: 160x50, couleur sur 8-bit
