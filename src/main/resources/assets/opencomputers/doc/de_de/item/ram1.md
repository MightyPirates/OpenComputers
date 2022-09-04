# Speicher (RAM)

![Do you remember, dancing in September~](oredict:opencomputers:ram1)

Der Random Access Memory ist, wie die [CPU](cpu1.md) ein grundlegendes Teil in allen [Computern](../general/computer.md). Je nach der Architektur der CPU hat der RAM einen großen Effekt darauf, was ein Computer kann und was er nicht kann. In der standardmäßigen LUA-Architektur kontrolliert es die tatsächliche Menge von Memory die LUA-Scripts verwenden können. Um größere und speicherintensivere Programme zu schreiben wird mehr Speicher verwendet.

Der RAM ist verfügbar in verschiedenen Stufen mit unterschiedlichen Kapazitäten.
- Stufe 1: 192KB
- Stufe 1.5: 256KB 
- Stufe 2: 384KB
- Stufe 2.5: 512KB
- Stufe 3: 768KB
- Stufe 3.5: 1024KB

Dies trifft allerdings nur für die LUA-Architektur. Andere Architekturen stellen unterschiedliche Mengen an Speicher zur Verfügung. Zudem werden Stufe-1- und Stufe-1.5-Speicherriegel als Stufe-1-Riegel gehandhabt werden, wie es auch bei 2 und 3 der Fall ist.

Die Werte können in der Konfigurationsdatei geändert werden.
