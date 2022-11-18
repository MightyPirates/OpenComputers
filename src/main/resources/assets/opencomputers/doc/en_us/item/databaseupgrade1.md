# Database Upgrade

![Living in the database.](oredict:opencomputers:databaseUpgrade1)

The database upgrade can be configured to store a list of item stack representations, which can then be used by other components. This is particularly useful for items that are differentiated purely based on their NBT data, which is not part of the item stack descriptor returned by callbacks.

To configure a database, open it by right-clicking with the database in hand. Place the stacks you wish to configure it with into the top inventory. This will store a "ghost stack", i.e. no "real" items are stored in the database.

Alternatively the database can be configured automatically using the component API provided by [inventory controllers](inventoryControllerUpgrade.md) and [geolyzers](../block/geolyzer.md).
