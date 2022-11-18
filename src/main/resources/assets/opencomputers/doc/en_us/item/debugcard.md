# Debug Card

![Wait, if I- oooooh.](item:opencomputers:debugcard)

The debug card is a creative-only item that was originally only intended to make debugging things easier, by automating some processes. It has since gotten a bunch more functionality, making it quite useful for custom map-making.

Note that you can use sneak-activate while holding the card to bind it to you or unbind it, meaning `runCommand` will be performed using your permission levels instead of the default OpenComputers ones.

A debug card can receive messages similar to a [linked card](linkedCard.md), firing a `debug_message` event. You can send such a message using either another debug card's `sendDebugMessage` or the Minecraft command `/oc_sendDebugMessage` (or `/oc_sdbg`).
