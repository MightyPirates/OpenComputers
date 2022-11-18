# Routeur

![[relays](relay.md).](oredict:opencomputers:switch)

*Ce bloc est déprécié et sera retiré dans une version future.* Transformez les en [relai](relay.md) pour éviter de les perdre.

Le routeur peut être utilisé pour permettre à différents sous-réseaux de s'envoyer des messages réseau entre eux, sans exposer leurs composants aux [ordinateurs](../general/computer.md) des autres réseaux. Maintenir l'état "local" d'un composant est généralement une bonne idée, pour éviter aux [ordinateurs](../general/computer.md) d'utiliser le mauvais [écran](screen1.md) ou pour éviter qu'une surcharge de composants survienne (ce qui provoque le crash de l'[ordinateur](../general/computer.md) et l'empêche de démarrer).

Il y a également une version sans-fil de ce bloc, appelée le [point d'accès](accessPoint.md), qui peut aussi relayer des messages sans-fil. Les messages sans-fil peuvent être reçus et relayés par d'autres [points d'accès](accessPoint.md), ou par des [ordinateurs](../general/computer.md) équipés d'une [carte réseau sans-fil](../item/wlanCard1.md).

Les routeurs et [points d'accès](accessPoint.md) ne gardent *pas* de trace des paquets qu'ils ont récemment relayé, donc évitez les boucles dans votre réseau ou vous pourriez recevoir le même paquet plusieurs fois. A cause de la taille limitée de la mémoire tampon des routeurs, une perte des paquets peut survenir si vous essayez d'envoyer des messages réseau trop fréquemment. Vous pouvez améliorer vos routeurs et points d'accès pour augmenter la vitesse à laquelle ils relaient les messages, ainsi que la taille interne de la file des messages.

Les paquets sont seulement renvoyés un certain nombre de fois, donc enchaîner un certain nombre de [routeurs](switch.md) ou de points d'accès n'est pas possible. Par défaut, un paquet sera renvoyé jusqu'à 5 fois.
