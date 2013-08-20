package li.cil.oc.api

/**
 * List of possible item component types.
 *
 * This is used to determine which item components may go into which slots in
 * a computer, since unlike block components, item components must be placed
 * inside the computer, not next to it.
 */
object ComponentType extends Enumeration {
  val PSU = Value("PSU")
  val RAM = Value("RAM")
  val HDD = Value("HDD")
  val PCI = Value("PCI")
  val GPU = Value("GPU")
}