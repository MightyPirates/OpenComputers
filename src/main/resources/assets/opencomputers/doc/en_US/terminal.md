# Remote Terminal

![Remote access.](oredict:oc:terminal)

The Remote Terminal can be used to remote control [Servers](server.md). To use it, sneak-activate a server that is installed in a [Server Rack](serverRack.md) (click on the server rack block in the world, targeting the server to bind the terminal to).

When a terminal is bound to a server, a virtual [screen](screen1.md) and [keyboard](keyboard.md) get connected to the server. This can lead to unexpected behavior if another real screen and/or keyboard is connected to the server, so this should be avoided. When rightclicking with the terminal in hand after binding it, a GUI will open, the same as when opening the GUI of a screen with an attached keyboard.

Multiple terminals can be bound to one server, but they will all display the same information, as they will share the virtual screen and keyboard. The number of terminals that can be bound to a server depends on the server's tier. The range in which the terminals work can be configured in the server rack's GUI.
