package li.cil.oc.api.driver;

/**
 * List of possible item component types.
 * <p/>
 * This is used to determine which item components may go into which slots in
 * a computer, since unlike block components, item components must be placed
 * inside the computer, not next to it.
 */
public enum Slot {
    /**
     * Power generating components, such as generators.
     */
    Power,

    /**
     * Memory extension components.
     */
    Memory,

    /**
     * Hard disk drives.
     */
    HardDiskDrive,

    /**
     * Extension cards such as graphics or redstone cards.
     */
    Card,

    /**
     * Floppy disks.
     */
    Disk
}