package li.cil.oc.api.network;

/**
 * This interface can be used to easily convert tile entities to components,
 * without having to implement {@link li.cil.oc.api.network.Environment}
 * themselves. The simple implementation will provide no access to OC's internal
 * component network, since you won't have access to the node representing the
 * tile entity. Use this only for simple cases, where you want to expose a
 * couple of methods to the programs running computers.
 * <p/>
 * This is an interface instead of an annotation, to allow stripping via the
 * ever so handy {@link cpw.mods.fml.common.Optional} annotation, meaning there
 * will be no strong dependency on OpenComputers.
 * <p/>
 * Classes implementing this interface will be expanded with the methods
 * required for them to function as native block components (say, like the
 * screen or keyboard). This means functions in the <tt>Environment</tt>
 * interface have to created using a class transformer. If any of the methods
 * already exist, this will fail! If things don't work, check your logs, first.
 * <p/>
 * To expose methods to OC, tag them with {@link li.cil.oc.api.machine.Callback}
 * and have them use the according signature (see the documentation on the
 * <tt>Callback</tt> annotation).
 * <p/>
 * Alternatively, implement {@link li.cil.oc.api.network.ManagedPeripheral} in
 * addition to this interface, to make methods available ComputerCraft style.
 * <p/>
 * So, in short:
 * <ul>
 * <li>Implement this interface on a tile entity that should expose
 * methods to computers.</li>
 * <li>Annotate methods with <tt>Callback</tt> so they exported.</li>
 * <li>Alternatively/additionally implement <tt>ManagedPeripheral</tt> to
 * provide methods via a list of names and single callback method.</li>
 * </ul>
 * <p/>
 * For example:
 * <pre>
 *     {@literal @}Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "OpenComputers")
 *     public class TileEntityMyFancyThing extends TileEntity
 *            implements SimpleComponent
 *     {
 *         {@literal @}Override
 *         public String getComponentName() {
 *             return "fancy_thing";
 *         }
 *
 *         {@literal @}Callback
 *         {@literal @}Optional.Method(modid = "OpenComputers")
 *         public Object[] greet(Context context, Arguments args) {
 *             return new Object[]{String.format("Hello, %s!", args.checkString(0))};
 *         }
 *     }
 * </pre>
 * Using the alternative method to provide methods:
 * <pre>
 *     {@literal @}Optional.InterfaceList({
 *         {@literal @}Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "OpenComputers"),
 *         {@literal @}Optional.Interface(iface = "li.cil.oc.api.network.ManagedPeripheral", modid = "OpenComputers")
 *     })
 *     public class TileEntityMyFancyThing extends TileEntity
 *            implements SimpleComponent, ManagedPeripheral
 *     {
 *         {@literal @}Override
 *         public String getComponentName() {
 *             return "fancy_thing";
 *         }
 *
 *         public String[] methods() {
 *             return new String[] {"greet"};
 *         }
 *
 *         {@literal @}Optional.Method(modid = "OpenComputers")
 *         public Object[] invoke(String method, Context context, Arguments args) {
 *             if ("greet".equals(method)) {
 *                 return new Object[]{String.format("Hello, %s!", args.checkString(0))};
 *             } else {
 *                 throw new NoSuchMethodException();
 *             }
 *         }
 *     }
 * </pre>
 */
public interface SimpleComponent {
    /**
     * The name the component should be made available as.
     * <p/
     * This is the name as seen in the <tt>component.list()</tt> in Lua, for
     * example. You'll want to make this short and descriptive. The convention
     * for component names is: all lowercase, underscores where necessary. Good
     * component names are for example: disk_drive, furnace, crafting_table.
     *
     * @return the component's name.
     */
    String getComponentName();
}
