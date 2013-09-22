package li.cil.oc.api;

/**
 * List of possible item component types.
 * 
 * This is used to determine which item components may go into which slots in a
 * computer, since unlike block components, item components must be placed
 * inside the computer, not next to it.
 */
public enum ComponentType {
  PSU, RAM, HDD, PCI
}