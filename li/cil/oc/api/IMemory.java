package li.cil.oc.api;

/**
 * Unlike all other component drivers, drivers for memory (RAM) need to
 * implement an additional interface, since we want to keep control over memory
 * under tight control. Like this, RAM components don't directly set the
 * available memory, but instead we check all of them and decide how much memory
 * to really make available to the computer (makes an upper limit realizable
 * even if mods add custom RAM modules).
 */
public interface IMemory extends IDriver {
  /**
   * The amount of memory this component provides. Note that this number may, in
   * fact, be negative, so you could make components that reserve some portion
   * of the memory, for example.
   */
  int getAmount();
}