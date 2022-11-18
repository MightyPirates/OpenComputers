# Rack

![Free housing.](oredict:opencomputers:rack)

A rack houses up to four rack mountables such as [servers](../item/server1.md), [terminal servers](../item/terminalServer.md) and [mountable disk drives](../item/diskDriveMountable.md). Connectivity of mountables in a rack can be configured in detail via the GUI. In particular, if [servers](../item/server1.md) contain components that support it, such as [network cards](../item/lanCard.md), network-only connections can be defined for those components. These connections will serve purely for passing along network messages, components will not be visible through them. Such network-only connections are differentiated by the lines being slimmer than the "main" connections, which also allow component access. Each internal connection must be between a mountable / component in a mountable and a bus connected to a side of the rack. To connect multiple mountables to each other, connect them to the same bus.

Racks also act as [relay](relay.md) and [power distributor](powerDistributor.md) in one. Whether it acts as a relay or not can be configured in the rack's GUI, it being enabled being indicated by a connector line between the side buses.
