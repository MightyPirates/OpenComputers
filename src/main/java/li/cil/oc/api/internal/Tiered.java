package li.cil.oc.api.internal;

/**
 * This is implemented by most things that are tiered in some way.
 * <br>
 * For example, this is implemented by screens, computer cases, robots and
 * drones as well as microcontrollers. If you want you can add tier specific
 * behavior this way.
 */
public interface Tiered {
    /**
     * The zero-based tier of this... thing.
     * <br>
     * For example, a tier one screen will return 0 here, a tier three screen
     * will return 2.
     */
    int tier();
}
