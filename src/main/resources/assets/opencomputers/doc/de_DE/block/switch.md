# Switch

![Baut Brücken.](oredict:oc:switch)

Der Switch kann verwendet werden um verschiedene Subnetzwerken das Senden von Nachrichten zueinander zu ermöglichen, ohne Komponenten Computern in anderen Netzen zugänglich zu machen. Grundsätzlich ist es eine gute Idee Komponenten lokal zu behalten, damit [Computer](../general/computer.md) nicht die falschen Komponenten ansprechen oder Komponenten-Overflows zu verursachen (welche dazu führen, dass Computer crashen und nicht hochfahren.)

Es gibt auch eine kabellose Variation dieses Blocks, welcher als [Access Points](accessPoint.md) bezeichnet wird. Kabellos verschickte Nachrichten können von anderen Zugriffspunkten oder Computern mit kabelloser Netzwerkkarte sowohl empfangen als auch weitergesendet werden.

Switches und [Access Points](accessPoint.md) *führen kein Protokoll* über die bereits gesendeten Pakete, daher besteht die Gefahr, dass Pakete in einem Kreis verlaufen oder dass das Paket mehrmals empfangen wird. Durch die geringe Buffergröße von Switches führt das zu häufige Senden von Nachrichten zu Paketverlust. Switches und Zugriffspunkte können jedoch mit upgrades versehen werden, damit die Geschwindigkeit oder der interne Puffer verbessert wird.

Pakete werden nur begrenzt häufig neu versendet, daher ist es nicht möglich, eine unbegrenzte Anzahl von Switches oder Zugriffspunkten hintereinander zu hängen. Standardmäßig wird ein Paket bis zu fünf mal neu gesendet.
