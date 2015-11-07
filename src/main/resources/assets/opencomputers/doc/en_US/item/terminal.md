# Remote Terminal

![Remote access.](oredict:oc:terminal)

The remote terminal can be used to remotely control [servers](server1.md). To use it, sneak-activate a server that is installed in a [rack](../block/rack.md) (click on the [rack](../block/rack.md) block in the world, targeting the [server](server1.md) to bind the terminal to).

When a terminal is bound to a [server](server1.md), a virtual [screen](../block/screen1.md) and [keyboard](../block/keyboard.md) get connected to the server. This can lead to unexpected behavior if another real screen and/or keyboard is connected to the server, so this should be avoided. When using the terminal in hand after binding it, a GUI will open in the same manner as a [keyboard](../block/keyboard.md) attached to a [screen](../block/screen1.md).

Multiple terminals can be bound to one [server](server1.md), but they will all display the same information, as they will share the virtual [screen](../block/screen1.md) and [keyboard](../block/keyboard.md). The number of terminals that can be bound to a [server](server1.md) depends on the [server](server1.md)'s tier. The range in which the terminals work can be configured in the [rack](../block/rack.md)'s GUI.
