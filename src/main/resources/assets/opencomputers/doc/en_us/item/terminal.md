# Remote Terminal

![Remote access.](oredict:opencomputers:terminal)

The remote terminal can be used to remotely control computers via [terminal servers](terminalServer.md). To use it, activate a [terminal server](terminalServer.md) that is installed in a [rack](../block/rack.md) (click on the [rack](../block/rack.md) block in the world, targeting the [terminal server](terminalServer.md) to bind the terminal to).

A [terminal server](terminalServer.md) provides a virtual [screen](../block/screen1.md) and [keyboard](../block/keyboard.md) which can be controlled via the terminal. This can lead to unexpected behavior if another real screen and/or keyboard is connected to the same subnetwork as the [terminal server](terminalServer.md), so this should usually be avoided. When using the terminal in hand after binding it, a GUI will open in the same manner as a [keyboard](../block/keyboard.md) attached to a [screen](../block/screen1.md).

Multiple terminals can be bound to one [terminal server](terminalServer.md), but they will all display the same information, as they will share the virtual [screen](../block/screen1.md) and [keyboard](../block/keyboard.md). The number of terminals that can be bound to a [terminal server](terminalServer.md) is limited. When the number of bound terminals is at the limit, binding another one will unbind the first bound one.
