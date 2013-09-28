package li.cil.oc.api

/**
 * Possible reachability values foe nodes.
 * <p/>
 * Since all components that are connected are packed into the same network,
 * we want some way of controlling what's accessible from where on a low
 * level (to avoid unnecessary messages and unauthorized access).
 */
object Visibility extends Enumeration {
  /** The node neither receives nor sends messages. */
  val None = Value("None")

  /** The node only handles messages from its immediate neighbors. */
  val Neighbors = Value("Neighbors")

  /** The node can interact with the complete network. */
  val Network = Value("Network")
}
