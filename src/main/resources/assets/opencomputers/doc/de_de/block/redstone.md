# Redstone-I/O

![Hi Red.](oredict:opencomputers:redstone)

Der Redstone-I/O-Block kann verwendet werden, um ferngesteuert Redstonesignale auszugeben und einzulesen. Es verhält sich wie ein Hybrid einer Stufe-1- und einer Stufe-2-[Redstonekarte](../item/redstoneCard1.md). Es kann analoge und gebündelte Signale lesen wie schreiben, aber kann keine kabellosen Redstonesignale ausgeben.

Die Methoden der API müssen mit Seiten aus `sides` (d.h. den Himmelsrichtungen) verwendet werden.

Genau wie die [Redstonekarten](../item/redstoneCard1.md) sendet dieser Block ein Signal zum verbunden [Computer](../general/computer.md), wenn der Status eines Redstonesignals wechselt - sowohl bei analog als auch für gebündelte Signale. Er kann zudem verwendet werden um verbundene Computer aufzuwecken, sobald eine gewisse Signalstärke überschritten wird. Damit kann man Computer automatisch hochfahren.
