package li.cil.oc.api.driver;

/**
 * This interface specifies the structure of a driver for a component.
 * <p/>
 * A driver is essentially the glue code that allows arbitrary objects to be
 * used as computer components. They specify an API that is injected into the
 * Lua state when the driver is installed, and provide general information used
 * by the computer.
 * <p/>
 * Do not implement this interface directly; use the `ItemDriver` and
 * `BlockDriver` interfaces for the respective component types.
 */
public interface Driver {
}
