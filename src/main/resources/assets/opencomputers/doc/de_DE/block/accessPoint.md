# Access Point

![AAA](oredict:oc:accessPoint)

Der Access Point ist die drahtlose Variante des [Switches](switch.md). Dieser kann benutzt werden um Subnetzwerke zu separieren, so dass [Komponente](../general/computer.md) nicht in anderen Netzwerken erreichbar sind. Dennoch ist es möglich Nachrichten in andere Netzwerke zu schicken.

Als Erweiterung dient dieser Block als Verstärker. Er leitet kabelgebundene Nachrichten als solche weiter, beziehungsweise werden Nachrichten, die über eine Drahtlos-Netzwerkkarte gesendet werden, als Funknachricht oder kabelgebundene Nachricht weiter geleitet.

[Switches](switch.md) und Access Point können die Pakete, die Sie weiter geleitet haben nicht folgen, somit ist es empfohlen Schleifen zu vermeiden, sonst könnten Pakete Ihr Ziel mehrfach erreichen. Aufgrund der begrenzten Puffergröße von [Switches](switch.md) kann es zum Paketverlust kommen, wenn Netzwerknachrichten zu häufig gesendet werden. Sie können Ihre Switches und Access Points aufrüsten, um ihre Geschwindigkeit, mit der sie weiterzuleiten Nachrichten, sowie ihre internen Nachrichtenwarteschlange zu verbessern.

Pakete werden nicht unendlich oft erneut gesendet, sodass eine eventuelle Verkettung von [Switches](switch.md) und Access Points nicht möglich ist. Die Nachrichten werden standartmaäßig nur fünf mal erneut gesendet.
