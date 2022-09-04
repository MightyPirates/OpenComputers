# Screens

![See this?](oredict:opencomputers:screen1)

A screen is used in combination with a [graphics card](../item/graphicsCard1.md), to allow [computers](../general/computer.md) to display text. Different screen tiers have different capabilities, such as supporting different resolutions and color depths. Screens range from low-resolution, monochrome displays to high-resolution displays with up to 256 colors. 

The available resolution and color depth depends on the lowest tier component. When using a [graphics card (tier 1)](../item/graphicsCard1.md) with a [screen (tier 3)](screen3.md), only the tier 1 resolution and color depth is usable. However, when using a tier 3 [graphics card](../item/graphicsCard1.md) with a tier 1 screen, while resolution and color depth will still be limit to tier 1, the different operations on the [graphics card](../item/graphicsCard1.md) will be faster than when using a tier 1 [graphics card](../item/graphicsCard1.md).

Screens can be placed next to each other to form multi-block screens, as long as they are facing the same way. When placed facing up or down they must also be rotated the same way. Their orientation is indicated by an arrow overlay shown while holding a screen in hand.

The size of a screen has no impact on the available resolution, only its tier has. To control how adjacent screens connect, screens can also be dyed using any dye. Simply right-click the screen with a dye in hand. The dye will not be consumed, but screens will not retain this color when broken. Screens with different colors will not connect. Screens with different tiers will never connect, even if they have the same color.

Tier 2 and tier 3 screens also support mouse input. Clicks can either be performed in a screen's GUI (which can only be opened if a [keyboard](keyboard.md) is connected to the screen), or by using a screen while sneaking (empty-handed when in doubt). The sneaking part is optional if the screen has no [keyboard](keyboard.md). Note that whether the GUI opens when sneak- or normally activating a screen can be controlled using the component API that it exposes to connected [computers](../general/computer.md). Tier 3 screens allow more accurate hit position detection, if enabled in their component. This allows detecting whether the upper or lower half of a single character space was clicked, for example, which can be useful when using special Unicode characters to simulate higher resolutions.

The resolutions and color depths for the screens are as follows:
- Tier 1: 50x16, 1-bit color.
- Tier 2: 80x25, 4-bit color.
- Tier 3: 160x50, 8-bit color.
