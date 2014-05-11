package li.cil.oc.api.driver;

/**
 * List of possible item component types.
 * <p/>
 * This is used to determine which item components may go into which slots in
 * a computer's or robot's inventory.
 */
public enum Slot {
    /**
     * Invalid slot type.
     * <p/>
     * Drivers should never return this, used internally.
     */
    None,

    /**
     * Extension cards such as graphics cards or redstone cards.
     * <p/>
     * The primary means of adding new functionality to a computer or robot.
     */
    Card,

    /**
     * Floppy disks.
     * <p/>
     * These can be inserted into the disk drive block, robots and tier three
     * computer cases. They provide persistent storage cheaper than hard disk
     * drives, but with much more limited capacity.
     */
    Disk,

    /**
     * Hard disk drives.
     * <p/>
     * These can be installed in computers to provide persistent storage.
     */
    HardDiskDrive,

    /**
     * Memory extension components. Used to increase computer RAM.
     */
    Memory,

    /**
     * CPU slots, used in servers.
     * <p/>
     * Components should usually not implement this slot type, unless you want
     * to provide an alternative CPU (that will have the same effect as the
     * default one, so I don't think that would make a lot of sense).
     */
    Processor,

    /**
     * Tool slot in robots (equipment slot).
     * <p/>
     * Components should usually not implement this slot type, since this slot
     * allows any kind of item. It is only used to define the background icon
     * for the tool slot in robots.
     */
    Tool,

    /**
     * Upgrade slot for robots.
     * <p/>
     * Used for special robot upgrades such as internal engines and the like.
     */
    Upgrade,

    /**
     * Upgrade that provides a slot in the assembled robot.
     */
    UpgradeContainer
}