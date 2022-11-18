# Wegpunkt

!["Da lang!" - "Nein, ganz falsch! Dort entlang!"](oredict:opencomputers:waypoint)

Der Wegpunkt kann mit Hilfe des [Navigations-Upgrades](../item/navigationUpgrade.md) erkannt werden. So können Geräte mit diesem Upgrade Wegpunkte verwenden um durch die Welt zu navigieren. Dies ist besonders nützlich zum Schreiben einfach wiederverwendbarer Programme für Geräte wie [Roboter](robot.md) und [Drohnen](../item/drone.md).

Es gilt zu beachten, dass die tatsächliche Position welche das Navigationsupgrade zurückgibt *der Block vor dem Wegpunkt* ist (wie durch die Partikel angedeutet). So kann der Wegpunkt neben und über eine Kiste platziert werden und die Position des Wegpunktes kann als "über der Kiste" bezeichnet werden, ohne die Rotation beachten zu müssen.

Ein Wegpunkt hat zwei Eigenschaften die über das Navigations-Upgrade abgefragt werden können: Das derzeitige Redstonesignal und ein veränderbares Label. Das Label ist ein 32 Zeichen langer String der entweder über die GUI oder über die Komponenten-API des Wegpunktblocks verändert werden kann. Diese zwei Eigenschaften können dann auf dem Gerät verwendet werden um festzulegen was an dem Wegpunkt zu tun ist. Beispielsweise können alle Wegpunkt mit einem starken Redstonesignal als Input, alle mit schwachem Signal als Output verwendet werden.
