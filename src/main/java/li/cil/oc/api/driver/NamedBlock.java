package li.cil.oc.api.driver;

/**
 * This interface can be added to block drivers to provide a 'preferred name' in
 * case the driver is merged with other block drivers (interface based drivers
 * such as for <tt>IInventory</tt>).
 */
public interface NamedBlock {
    /**
     * The preferred name, in case the driver is merged with others.
     * <p/>
     * If multiple drivers with a preferred name are merged, the first one is
     * picked. This should usually not happen, since this is only intended to
     * be implemented by drivers for actual tile entities (not interfaces).
     *
     * @return the preferred name.
     */
    String preferredName();
}
