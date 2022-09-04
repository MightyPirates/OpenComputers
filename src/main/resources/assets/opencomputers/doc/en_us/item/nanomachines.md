# Nanomachines

![Nanomachines, son.](oredict:opencomputers:nanomachines)

These little guys interface with your nervous system to make you harder, better, faster, stronger, or kill you. Sometimes both at the same time! Put simply, nanomachines provide a power driven system for applying buffs (and debuffs) to the player they reside in. To "install" nanomachines, eat them!

Once injected, a new power indicator in your HUD will indicate how much energy your nanomachines have left to work with. You can recharge them by standing near a [charger](../block/charger.md). The more use you make of the nanomachines, the more energy they'll consume.

Nanomachines provide a certain number of "inputs" that can be triggered, causing many different effects on the player, ranging from visual effects such as particles spawning near the player, to select potion effects and some more rare and special behaviors!

Which input triggers what effect depends on the current configuration of the nanomachines, the actual "connections" being random per configuration. This means you'll have to try enabling different inputs to see what they do. If you're unhappy with a configuration, you can always reconfigure your nanomachines by injecting a new batch (just eat some more). To completely get rid of the nanomachines in you, consider drinking some [grog](acid.md). Beware that enabling too many inputs at a time has severe negative effects on you!

By default, the nanomachines will be on standby. You'll need to control them using wireless messages, so carrying a [tablet](tablet.md) with a [wireless network card](wlanCard1.md) is strongly recommended. Nanomachines will only react to wireless signals emitted by devices no further than two meters away, but they will react to messages on any port, and from any device!

Nanomachines react to a simple, proprietary protocol: each packet must consist of multiple parts, the first of which is the "header" and must equal the string `nanomachines`. The second part must be the command name. Additional parts are parameters for the command. The following commands are available, formatted as `commandName(arg1, ...)`:

- `setResponsePort(port:number)` - Set the port nanomachines should send response messages to, for commands that have a response.
- `getPowerState()` - Request the currently stored and maximum stored energy of the nanomachines.
- `getHealth()` - Request the player's health state.
- `getHunger()` - Request the player's hunger state.
- `getAge()` - Request the player's age in seconds.
- `getName()` - Request the player's display name.
- `getExperience()` - Request the player's experience level.
- `getTotalInputCount()` - Request the total number of available inputs.
- `getSafeActiveInputs()` - Request the number of *safe* active inputs.
- `getMaxActiveInputs()` - Request the number of *maximum* active inputs.
- `getInput(index:number)` - Request the current state of the input with the specified index.
- `setInput(index:number, value:boolean)` - Set the state of the input with the specified index to the specified value.
- `getActiveEffects()` - Request a list of active effects. Note that some effects may not show up in this list.
- `saveConfiguration()` - Requires a set of nanomachines in the players inventory, will store the current configuration to it.

For example, in OpenOS:
- `component.modem.broadcast(1, "nanomachines", "setInput", 1, true)` will enable the first input.
- `component.modem.broadcast(1, "nanomachines", "getHealth")` will get the player's health info.
