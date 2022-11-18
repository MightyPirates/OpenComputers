# Schwebe-Upgrade

![Leicht wie eine Feder.](oredict:opencomputers:hoverUpgrade1)

Das Schwebe-Upgrade erlaubt es [Robotern](../block/robot.md) viel höher über dem Boden zu fliegen als normal. Im Gegensatz zu [Drohnen](drone.md) können diese nämlich standardmäßig nur 8 Block hoch schweben. Das ist normal kein großes Problem, da sie sich trotzdem an Wänden entlang bewegen können. Sie können sich so bewegen:
- Roboter können sich nur bewegen, wenn Start- und Zielposition gültig sind (z.B. um Brücken bauen zu können)
- Die Position unter einem Roboter ist immer gültig (sie können sich immer herab bewegen)
- Positionen bis `Flughöhe` über einem vollen Block sind gültig (eingeschränkte Flugfähigkeiten)
- Jede Position mit einem angrenzenden Block mit einer vollen Seite in Richtung der Position ist gültig (Roboter können "klettern")

Hier eine Visualisierung der Regeln:
![Bewegungsregeln der Roboter zusammengefasst.](opencomputers:doc/img/robotMovement.png)

Wenn diese Regeln außer Kraft gesetzt werden müssen, ist dieses Upgrade das Upgrade der Wahl.
