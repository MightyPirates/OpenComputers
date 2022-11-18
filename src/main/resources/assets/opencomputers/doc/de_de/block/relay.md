# Relay

![Baut Brücken.](oredict:opencomputers:relay)

Das Relay kann verwendet werden um verschiedene Subnetzwerken das Senden von Nachrichten zueinander zu ermöglichen, ohne Komponenten Computern in anderen Netzen zugänglich zu machen. Grundsätzlich ist es eine gute Idee Komponenten lokal zu behalten, damit [Computer](../general/computer.md) nicht die falschen Komponenten ansprechen oder Komponenten-Overflows zu verursachen (welche dazu führen, dass Computer crashen und nicht hochfahren.)

Mit einer [Drahtlosnetzwerkkarte](../item/wlanCard1.md) können auch kabellose Nachrichten weitergeleitet werden. Dann kann dieser Block als Repeater verwendet werden: Es kann Nachrichten aus verkabelten Netzwerken zu anderen Geräten in verkabelten Netzwerken weiterleiten, oder Nachrichten aus kabellosen Netzwerken zu verkabelten oder kabellosen Netzwerken.

Relays führen *kein Protokoll* über kürzlich versendete Nachrichten, also ist es wichtig, Kreisläufe im Netzwerk zu vermeiden, oder das selbe Paket kann mehrmals empfangen werden. Aufgrund der geringen Puffergröße von Switches kann Paketverlust zu einem Problem werden, wenn Netzwerknachrichten zu oft gesendet werden. Ein Upgrade für Switches und Access Points zur Beschleunigung der Nachrichtenweiterleitung ist möglich, genau wie die interne Nachrichtenqueue erweitert werden kann.

Pakete werden nur ein paar mal weitergeschickt; demnach ist nicht möglich eine unbegrenzte Anzahl an Relays aufzustellen. Standardmäßig kann ein Paket bis zu fünf mal "springen".
