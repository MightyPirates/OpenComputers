# Capaciteur

![Au delà des 8000.](oredict:opencomputers:capacitor)

Le capaciteur emmagasine de l'énergie qui sera utilisée par le réseau, en se comportant comme un tampon d'énergie au besoin. Contrairement à la conversion de l'énergie d'autres mods vers l'énergie interne à OpenComputers (en utilisant un [convertisseur énergétique](powerConverter.md) par exemple), le transfert d'énergie dans un sous-réseau est instantané. Posséder un tampon d'énergie interne sera utile pour des tâches qui nécessitent beaucoup d'énergie, comme l'[assemblage](assembler.md) et/ou la [charge](charger.md) d'appareils comme les [robots](robot.md) ou les [drones](../item/drone.md).

L'efficacité de stockage des capaciteurs augmente avec le nombre de capaciteurs en contact direct ou à promixité. Par exemple, 2 capaciteurs directement en contact auront une plus grande capacité de stockage que la somme de 2 capaciteurs séparés. Ce bonus de proximité s'applique aux capaciteurs jusqu'à 2 blocs de distance, et diminue au fur et à mesure que la distance entre les capaciteurs augmente.

Le capaciteur peut être connecté à un [distributeur énergétique](powerDistributor.md) pour fournir de l'énergie aux autres [ordinateurs](../general/computer.md) ou machines du réseau.
