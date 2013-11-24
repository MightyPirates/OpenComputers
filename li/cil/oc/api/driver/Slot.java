package li.cil.oc.api.driver;

/**
 * List of possible item component types.
 * <p/>
 * This is used to determine which item components may go into which slots in
 * a computer's inventory.
 */
public enum Slot {
    /**
     * Invalid slot type.
     */
    None,

    /**
     * Extension cards such as graphics cards or redstone cards.
     */
    Card,

    /**
     * Floppy disks. These can be inserted into the Disk Drive block.
     */
    Disk,

    /**
     * Hard disk drives. These can be installed in computers.
     */
    HardDiskDrive,

    /**
     * Memory extension components. Used to increase computer RAM.
     */
    Memory,

    /**
     * Power generating components, such as generators.
     */
    Power,

    /**
     * Tools that can be used by robots.
     */
    Tool
}