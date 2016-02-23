# Server Rack

![Free housing.](oredict:oc:serverRack)

A server rack houses up to four [servers](../item/server1.md). A [server](../item/server1.md) is a higher tier [computer](../general/computer.md), which can only run when inside a server rack. [Servers](../item/server1.md) can be remote controlled using a [remote terminal](../item/terminal.md). The number of [remote terminals](../item/terminal.md) that can be connected to a single [server](../item/server1.md) at a time depends on the tier of the [server](../item/server1.md). The distance up to which the [remote terminal](../item/terminal.md) work can be configured in the rack's GUI. Higher values have require more energy.

Each [server](../item/server1.md) in a server rack can only communicate with one "face" of the server rack at a time - or none at all. Which side each [server](../item/server1.md) is connected to can be configured in the server rack's GUI. Beware that the sides are from the point of view of the server rack, i.e. if you are looking at the front of the server rack, `sides.right` will be to your left and vice versa.

Server racks act as [relay](relay.md) and [power distributor](powerDistributor.md) in one. The switch mode of the server rack can be configured in its GUI, with the two options being internal and external. In external mode the server rack will behave like a normal [relay](relay.md). In internal mode, messages are only passed to the [servers](../item/server1.md) in the rack, and will not be automatically relayed to the other faces of the rack. [Servers](../item/server1.md) will still be able to send messages to each other. This allows using server racks as advanced [relays](relay.md) that can perform filter and mapping operations, for example.
