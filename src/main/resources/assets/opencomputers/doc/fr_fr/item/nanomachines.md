# Nanomachines

![Nanomachines, son.](oredict:opencomputers:nanomachines)

Ces petits bonhommes créent une interface avec votre système nerveux pour vous faire aller toujours plus loin, toujours plus haut, toujours plus fort ! Ou vous tuer. Parfois les deux en même temps ! En clair, les nanomachines fournissent un système basé sur de l'énergie pour appliquer des effets (bons ou mauvais) sur le joueur dans lequel elles résident. Pour "installer" des nanomachines, mangez les !

Une fois injectées, un nouvel indicateur d'énergie dans votre affichage tête haute vous indiquera combien il reste d'énergie à vos nanomachines. Vous pouvez les recharger en vous tenant près d'un [chargeur](../block/charger.md). Plus vous utiliserez vos nanomachines, plus elles consommeront d'énergie.

Les nanomachines fournissent un certain nombre "d'entrées" qui peuvent être activées, ce qui provoque différents effets sur le joueur, en allant des effets visuels comme des particules apparaissant près du joueur, à des effets de potion et d'autres comportement rares et particuliers !

En fonction de la configuration des nanomachines, les entrées déclenchent différents effets, les "connexions" étant aléatoires par configuration. Cela signifie que vous devrez essayer d'activer différentes entrées pour découvrir ce qu'elles activent. Si vous n'êtes pas satisfaits d'une configuration, vous pouvez reconfigurer vos nanomachines en en injectant d'autres (mangez en d'autres). Pour vous débarrasser complètement des nanomachines en vous, buvez du [grog](acid.md). Faites attention, activer trop d'entrées à la fois peut avoir de sérieuses conséquences sur votre organisme !

Par défaut, les nanomachines seront passives. Vous devrez les contrôler en utilisant des messages sans fil, donc transporter une [tablette](tablet.md) avec une [carte de réseau sans-fil](wlanCard1.md) est vivement recommandé. Les nanomachines réagiront seulement aux signaux sans fil émis par des appareils situés à moins de 2 mètres, mais elles réagiront aux messages provenant de n'importe quel port, et de n'importe quel appareil !

Les nanomachines réagissent à un protocole propriétaire très simple : chaque paquet doit être constitué de plusieurs parties, la première étant le "header" (en-tête) et qui doit valoir `nanomachines`. La deuxième partie doit être le nom de la commande. Les parties supplémentaires sont les paramètres de la commande. Les commandes suivantes sont disponibles, avec le format `nomDeLaCommande(argument1, ...)` :

- `setResponsePort(port:number)` - Affecte le port sur lequel les nanomachines doivent renvoyer une réponse, pour les commandes qui renvoient une réponse.
- `getPowerState()` - Demande l'état actuel et la capacité maximale de l'énergie stockée dans les nanomachines.
- `getHealth()` - Demande le niveau de santé du joueur.
- `getHunger()` - Demande le niveau de faim du joueur.
- `getAge()` - Demande l'âge du joueur en secondes.
- `getName()` - Demande le nom du joueur affiché en jeu.
- `getExperience()` - Demande le niveau d'expérience du joueur.
- `getTotalInputCount()` - Demande le nombre total d'entrées disponibles.
- `getSafeActiveInputs()` - Demande le nombre d'entrées actives *sans risque*.
- `getMaxActiveInputs()` - Demande le nombre *maximal* d'entrées actives.
- `getInput(index:number)` - Demande l'état actuel de l'entrée avec l'index en paramètre.
- `setInput(index:number, value:boolean)` - Affecte l'état de l'entrée avec en paramètres l'index et l'état.
- `getActiveEffects()` - Demande une liste des effets actifs. Remarquez que certains effets pourraient ne pas s'afficher dans la liste.

Par exemple, dans OpenOS :
- `component.modem.broadcast(1, "nanomachines", "setInput", 1, true)` activera la première entrée.
- `component.modem.broadcast(1, "nanomachines", "getHealth")` renverra l'information sur la santé du joueur.