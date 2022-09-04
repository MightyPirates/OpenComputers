# Point d'accès

![AAA](oredict:opencomputers:accessPoint)

*Ce bloc est déprécié et sera retiré dans une version future.* Transformez les en [relai](relay.md) pour éviter de les perdre.

Le point d'accès est la version sans-fil du [routeur](switch.md). Il peut être utilisé pour séparer des sous-réseaux pour que les machines qui les composent ne voient pas les [composants](../general/computer.md) des autres réseaux, tout en leur permettant d'envoyer des messages réseau aux machines d'autres réseaux.

En plus de ça, ce bloc peut faire office de répéteur : il peut renvoyer des messages filaires en tant que messages filaires à d'autres appareils, ou des messages sans-fil en tant que messages filaires ou sans-fil.

Les [routeurs](switch.md) et points d'accès ne gardent *pas* de trace des paquets qu'ils ont récemment relayé, donc évitez les boucles dans votre réseau ou vous pourriez recevoir le même paquet plusieurs fois. A cause de la taille limitée de la mémoire tampon des routeurs, une perte des paquets peut survenir si vous essayez d'envoyer des messages réseau trop fréquemment. Vous pouvez améliorer vos routeurs et points d'accès pour augmenter la vitesse à laquelle ils relaient les messages, ainsi que la taille interne de la file des messages.

Les paquets sont seulement renvoyés un certain nombre de fois, donc enchaîner un certain nombre de [routeurs](switch.md) ou de points d'accès n'est pas possible. Par défaut, un paquet sera renvoyé jusqu'à 5 fois.
