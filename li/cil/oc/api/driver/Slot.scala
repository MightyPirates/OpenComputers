package li.cil.oc.api.driver

/**
 * List of possible item component types.
 * <p/>
 * This is used to determine which item components may go into which slots in
 * a computer, since unlike block components, item components must be placed
 * inside the computer, not next to it.
 */
object Slot extends Enumeration {
  /** Power generating components, such as generators. */
  val Power = Value("Power")

  /** Memory extension components. */
  val Memory = Value("Memory")

  /** Hard disk drives. */
  val HardDiskDrive = Value("HardDiskDrive")

  /** Extension cards such as graphics or redstone cards. */
  val Card = Value("Card")

  /** Floppy disks. */
  val Disk = Value("Disk")
}