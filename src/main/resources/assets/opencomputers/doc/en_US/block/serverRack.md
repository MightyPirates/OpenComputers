# Server Rack

![Free housing.](oredict:oc:serverRack)

A Server Rack houses up to four [Servers](server.md). A server is a higher tier computer, which can only run when inside a server rack. Servers can be remote controlled using a [Remote Terminal](terminal.md). The number of terminals that can be connected to a single server at a time depends on the tier of the server. The distance up to which the remote terminals work can be configured in the rack's GUI. Higher values have a higher constant energy draw.

Each server in a server rack can only communicate with one "face" of the server rack at a time - or none at all. Which side each server is connected to can be configured in the server rack's GUI. Beware that the sides are from the point of view of the server rack, i.e. if you are looking at the front of the server rack, right will be to your left and vice versa.

Server racks act as [Switch](switch.md) and [Power Distributor](powerDistributor.md) in one. The switch mode of the server rack can be configured in its GUI, with the two options being internal and external. In external mode the server rack will behave like a normal switch. In internal mode, messages are only passed to the servers in the rack, they will not be automatically relayed to the other faces of the rack. Servers will still be able to send messages to each other. This allows using server racks as advanced switches that can perform filter and mapping operations, for example.
