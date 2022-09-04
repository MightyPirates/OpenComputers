# Getting Started

Also know as "how to build your first computer". To get your first [computer](computer.md) to run, you will need to first set it up correctly. There are many different types of computers in OpenComputers, but let's start with the basic one: the standard computer.

**Disclaimer**: this will be step-by-step, and also provide some information on how to look for issues yourself later on, so this is quite long. If you have never built a computer in real life, and/or are completely new to the mod, it is highly recommended you read through it all.

First off, you will need a [computer case](../block/case1.md). This is the block which will contain all of the components, defining the behavior of the computer you are building.

![A tier two computer case.](oredict:opencomputers:case2)

For example, you will need to choose what tier of [graphics card](../item/graphicsCard1.md) you wish to use, if you need a [network card](../item/lanCard.md), a [redstone card](../item/redstoneCard1.md) or, if you're just playing around in creative mode, maybe even a [debug card](../item/debugCard.md).

When you open the [computer case](../block/case1.md)'s GUI, you will see a few slots to the right. The number of slots, and what tier of component can be placed into them (indicated by the small roman numeral in the slot) depends on the tier of the case itself.
![GUI of a tier two computer case.](opencomputers:doc/img/configuration_case1.png)
In their empty state, [computer cases](../block/case1.md) are pretty useless. You can try to power up your [computer](computer.md) now, but it'll immediately print an error message to your chat log, and make its dissatisfaction heard by beeping at you. Good thing the error message is telling you what you can do to fix this situation: it requires energy. Connect your [computer](computer.md) to some power, either directly or via a [power converter](../block/powerConverter.md).

When you try to start it now, it will tell you that you need a [CPU](../item/cpu1.md). These come in different tiers - a trend you will notice is present throughout OpenComputers. For [CPUs](../item/cpu1.md), higher tiers mean more components at a time, as well as faster execution. So pick a tier, and put it in your [computer case](../block/case1.md).

Next up you will be asked to insert some [memory (RAM)](../item/ram1.md). Notice that the beep code is different now: long-short. Higher tiers of [memory (RAM)](../item/ram1.md) mean more memory available to the programs running on your [computer](computer.md). To run [OpenOS](openOS.md), which is the goal of this introduction, you will want to use at least two tier 1 [memory (RAM)](../item/ram1.md) sticks.

We're making good progress here. By now your [computer case](../block/case1.md) will look somewhat like this:
![Partially configured computer.](opencomputers:doc/img/configuration_case2.png)
And behold, turning it on now does not print any more error messages! But alas, it still doesn't do much. At least it beeps twice now. That means the actual execution of the [computer](computer.md) failed. In other words: it technically runs! This is where a very useful tool comes into play: the [analyzer](../item/analyzer.md). This tool allows inspecting many of OpenComputers' blocks, as well as some blocks from other mods. To use it on the [computer](computer.md), use the [analyzer](../item/analyzer.md) on the case while sneaking.

You should now see the error that caused the [computer](computer.md) to crash:
`no bios found; install configured EEPROM`

The emphasis here is on *configured*. Crafting an [EEPROM](../item/eeprom.md) is pretty simple. To configure it, you will usually use a [computer](computer.md) - but that's a little difficult right now, so we're going to use a recipe to craft a configured "Lua BIOS" [EEPROM](../item/eeprom.md). The standard recipe is an [EEPROM](../item/eeprom.md) plus a [manual](../item/manual.md). Put the configured [EEPROM](../item/eeprom.md) into your [computer](computer.md), aaaand.

Nope. Still nothing. But we know what to do: player uses [analyzer](../item/analyzer.md), it's super effective! Now we have a different error message:
`no bootable medium found; file not found`

Well then. That means the BIOS is working. It's just not finding a file system to boot from, such as a [floppy](../item/floppy.md) or [hard drive](../item/hdd1.md). The Lua BIOS in particular expects such a file system to furthermore contain a file named `init.lua` at root level. As with the [EEPROM](../item/eeprom.md), you usually write to file systems using a [computer](computer.md). You probably guessed it: we now need to craft our operating system disk. Take a blank [floppy disk](../item/floppy.md) and a [manual](../item/manual.md), craft them together, and you'll get an [OpenOS](openOS.md) disk.

Now, if you used a tier 2 [computer case](../block/case2.md) as in the screenshots above, you'll have nowhere to place that floppy. If you have a tier 3 or creative [computer case](../block/case3.md), you can place the floppy right into the [case](../block/case1.md). Otherwise you'll need to place a [disk drive](../block/diskDrive.md) next to your case (or connect it via [cables](../block/cable.md)). Once your disk is in place, you know what to do. Press the power button.

It lives! Or should, anyway. If it doesn't something went wrong, and you'll want to investigate using the [analyzer](../item/analyzer.md). But assuming it's running now, you're pretty much done. The hardest part is over. All that's left is to make it take input and show some output.

To allow the [computer](computer.md) to show some output, you'll want to grab a [screen](../block/screen1.md) and a [graphics card](../item/graphicsCard1.md).
![No, it's not a flatscreen.](oredict:opencomputers:screen2)

Place the [screen](../block/screen1.md) adjacent to your [computer case](../block/case1.md), or, again, connect it using some [cable](../block/cable.md). Then place a [graphics card](../item/graphicsCard1.md) of your choice into the [computer case](../block/case1.md). You should now see a blinking cursor on the [screen](../block/screen1.md). Finally, place a [keyboard](../block/keyboard.md) either on the [screen](../block/screen1.md) itself, or in a way so that it faces the [screen](../block/screen1.md), to enable [keyboard](../block/keyboard.md) input.

And with that, you're done. The [computer](computer.md) is up and running and ready for action. Try using it now! Type `lua` in the shell and press enter, and you'll be greeted with a bit of information on how to use the Lua interpreter. Here you can test basic Lua commands. For more information this topic see [the Lua page](lua.md).

![It lives!](opencomputers:doc/img/configuration_done.png)

Have fun building more complex [computers](computer.md), messing with [servers](../item/server1.md) and assembling [robots](../block/robot.md), [drones](../item/drone.md), [microcontrollers](../block/microcontroller.md) and [tablets](../item/tablet.md) in the [assembler](../block/assembler.md).

Happy coding!
