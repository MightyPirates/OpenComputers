# Bildschirme

![Kannst du das sehen?](oredict:opencomputers:screen1)

Ein Bildschirm wird in Kombinationen mit [Grafikkarten](../item/graphicsCard1.md) verwendet, um mit [Computern](../general/computer.md) Text darzustellen. Verschiedene Stufen haben unterschiedliche Fähigkeiten, wie die Unterstützung verschiedener Auflösungen und Farbtiefen. Sie reichen von geringauflösenden Monochromdisplays zu hochauflösenden Displays mit bis zu 256 Farben.

Die verfügbare Auflösung und Farbtiefe hängt von der niedrigststufigen Komponente ab. Eine Stufe-1-Grafikkarte mit einem Stufe-3-Bildschirm kann nur die Stufe-1-Auflösung und -Farbtiefe verwenden, genau wie bei einer Stufe-3-Grafikkarte und einem Stufe-1-Bildschirm. Dennoch werden die Operationen auf einer Stufe-3-Grafikkarte schneller ausgeführt als auf einer Stufe-1-Grafikkarte.

Bildschirme können nebeneinander platziert werden, um größere Bildschirme zu ermöglichen, solange sie in dieselbe Richtung zeigen. Wenn sie nach oben oder unten ausgerichtet werden, müssen sie auch gleich rotiert werden. Die Richtung wird von einem Pfeil angezeigt, wenn der Bildschirm in der Hand gehalten wird.

Die Größe eines Bildschirms hat keinen Einfluss auf die verfügbare Auflösung. Um verschiedene Bildschirme voneinander zu trennen können sie zudem mit jeder Farbe gefärbt werden. Der Bildschirm muss dafür nur mit einem Färbemittel rechts angeklickt werden. Das Färbemittel wird nicht aufgebraucht, allerdings geht die Farbe beim Abbau verloren. Bildschirme unterschiedlicher Stufen werden sich niemals verbinden, selbst wenn sie dieselbe Farbe haben.

Stufe-2- und Stufe-3-Bildschirme stellen außerdem einen Touchscreen zur Verfügung. Sie können in der Bildschirm-GUI verwendet werden (welche nur geöffnet werden kann, wenn eine [Tastatur](keyboard.md) angeschlossen ist), oder während der Bildschirm mit leeren Händen schleichend rechts angeklickt wird. Wenn der Bildschirm keine Tastatur hat, ist es nicht nötig zu schleichen. Dieses Verhalten kann mittels der zur Verfügung gestellten Komponenten-API angepasst werden.

Die folgenden Auflösungen und Farbtiefen sind verfügbar:
- Stufe 1: 50x16, 1-Bit-Farben (Monochrom).
- Stufe 2: 80x25, 4-Bit-Farben (16 Farben).
- Stufe 3: 160x50, 8-Bit-Farben (256 Farben).
