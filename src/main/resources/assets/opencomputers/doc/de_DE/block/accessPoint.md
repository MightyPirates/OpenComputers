# Access Point

![AAA](oredict:oc:accessPoint)

Der Access Point ("Zugriffspunkt") ist die kabellose Version des [Switches](switch.md). Er kann verwendet werden, um Subnetzwerke zu trennen, sodass Geräte in diesen keine [Komponenten](../general/computer.md) sehen. Dennoch ist es möglich, Netzwerknachrichten zu den Geräten in anderen Netzwerken zu senden.

Zudem kann dieser Block als Repeater verwendet werden: Es kann Nachrichten aus verkabelten Netzwerken zu anderen Geräten in verkabelten Netzwerken weiterleiten, oder Nachrichten aus kabellosen Netzwerken zu verkabelten oder kabellosen Netzwerken.

[Switches](switch.md) und Access Points führen *kein Protokoll* über kürzlich versendete Nachrichten, also ist es wichtig, Kreisläufe im Netzwerk zu vermeiden, oder das selbe Paket kann mehrmals empfangen werden. Aufgrund der geringen Puffergröße von Switches kann Paketverlust zu einem Problem werden, wenn Netzwerknachrichten zu oft gesendet werden. Ein Upgrade für Switches und Access Points zur Beschleunigung der Nachrichtenweiterleitung ist möglich, genau wie die interne Nachrichtenqueue erweitert werden kann.

Pakete werden nur eine bestimmte Anzahl an Versuchen wiederholt, also eine unbegrenzte Anzahl an [Switches](switch.md) oder Access Points aufzustellen ist nicht möglich. Standardmäßig wird ein Paket bis zu fünf mal wiederholt.
