package li.cil.oc.api;

import cpw.mods.fml.common.event.FMLInterModComms;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import org.apache.commons.lang3.tuple.Pair;

/**
 * This is a pure utility class to more comfortably register things that can
 * only be registered using IMC.
 * <br>
 * Use this if you have some kind of abstraction layer in place anyway, and can
 * safely use the API without class not found exceptions and such, and don't
 * want to put together the IMC messages manually.
 * <br>
 * This also servers to document of all IMC messages OpenComputers handles.
 * <br>
 * Feel free to copy these functions into your own code, just please don't
 * copy this class while keeping the package name, to avoid conflicts if this
 * class gets updated.
 */
@SuppressWarnings("unused")
public final class IMC {
    /**
     * Register a callback that is used as a filter for assembler templates.
     * Any templates that require a base item that is rejected by <em>any</em>
     * registered filter will be disabled. For example, if a filter rejects the
     * computer case item stacks, robots can not be assembled.
     * <br>
     * Signature of callbacks must be:
     * <pre>
     * boolean callback(ItemStack stack)
     * </pre>
     * <br>
     * Callbacks must be declared as <tt>packagePath.className.methodName</tt>.
     * For example: <tt>com.example.Integration.callbackMethod</tt>.
     *
     * @param callback the callback to register as a filtering method.
     */
    public static void registerAssemblerFilter(final String callback) {
        FMLInterModComms.sendMessage(MOD_ID, "registerAssemblerFilter", callback);
    }

    /**
     * Register a new template for the assembler.
     * <br>
     * Signature of callbacks must be:
     * <pre>
     * boolean select(ItemStack stack)
     * Object[] validate(IInventory inventory)
     * Object[] assemble(IInventory inventory)
     * </pre>
     * Values in the array returned by <tt>validate</tt> must be one of the following:
     * <pre>
     * // Valid or not.
     * new Object[]{Boolean}
     * // Valid or not, text for progess bar.
     * new Object[]{Boolean, IChatComponent}
     * // Valid or not, text for progess bar, warnings for start button tooltip (one per line).
     * new Object[]{Boolean, IChatComponent, IChatComponent[]}
     * </pre>
     * Values in the array returned by <tt>assemble</tt> must be one of the following:
     * <pre>
     * // The assembled device.
     * new Object[]{ItemStack}
     * // The assembled device and energy cost (which also determines assembly duration).
     * new Object[]{ItemStack, Number}
     * </pre>
     * <br>
     * Callbacks must be declared as <tt>packagePath.className.methodName</tt>.
     * For example: <tt>com.example.Integration.callbackMethod</tt>.
     *
     * @param name           the name of the device created using the
     *                       template. Optional, only used in logging.
     * @param select         callback used to determine if the template
     *                       applies to a base item. For example, the robot
     *                       template returns true from this if the passed
     *                       item stack is a computer case.
     * @param validate       callback used to determine if the template
     *                       configuration is valid. Once a template is
     *                       valid assembly can be started, but not before.
     * @param assemble       callback used to apply a template and create a
     *                       device from it.
     * @param host           the class of the device being assembled, i.e.
     *                       the host class for the components being
     *                       installed in the device. Used for filtering
     *                       eligible components. See {@link #blacklistHost}.
     * @param containerTiers the tiers of the container slots provided by the
     *                       template. The length determines the number of
     *                       containers. Maximum number is three.
     * @param upgradeTiers   the tiers of the upgrade slots provided by the
     *                       template. The length determines the number of
     *                       upgrades. Maximum number is nine.
     * @param componentSlots the types and tiers of component slots provided by
     *                       this template. May contain <tt>null</tt> entries
     *                       to skip slots (slots are ordered top-to-bottom,
     *                       left-to-right). For example, a robot template
     *                       with only two card slots will pass <tt>null</tt>
     *                       for the third component slot. Up to nine.
     */
    public static void registerAssemblerTemplate(final String name, final String select, final String validate, final String assemble, final Class host, final int[] containerTiers, final int[] upgradeTiers, final Iterable<Pair<String, Integer>> componentSlots) {
        final NBTTagCompound nbt = new NBTTagCompound();
        if (name != null) {
            nbt.setString("name", name);
        }
        nbt.setString("select", select);
        nbt.setString("validate", validate);
        nbt.setString("assemble", assemble);
        if (host != null) {
            nbt.setString("hostClass", host.getName());
        }

        final NBTTagList containersNbt = new NBTTagList();
        if (containerTiers != null) {
            for (int tier : containerTiers) {
                final NBTTagCompound slotNbt = new NBTTagCompound();
                slotNbt.setInteger("tier", tier);
                containersNbt.appendTag(slotNbt);
            }
        }
        if (containersNbt.tagCount() > 0) {
            nbt.setTag("containerSlots", containersNbt);
        }

        final NBTTagList upgradesNbt = new NBTTagList();
        if (upgradeTiers != null) {
            for (int tier : upgradeTiers) {
                final NBTTagCompound slotNbt = new NBTTagCompound();
                slotNbt.setInteger("tier", tier);
                upgradesNbt.appendTag(slotNbt);
            }
        }
        if (upgradesNbt.tagCount() > 0) {
            nbt.setTag("upgradeSlots", upgradesNbt);
        }

        final NBTTagList componentsNbt = new NBTTagList();
        if (componentSlots != null) {
            for (Pair<String, Integer> slot : componentSlots) {
                if (slot == null) {
                    componentsNbt.appendTag(new NBTTagCompound());
                } else {
                    final NBTTagCompound slotNbt = new NBTTagCompound();
                    slotNbt.setString("type", slot.getLeft());
                    slotNbt.setInteger("tier", slot.getRight());
                    componentsNbt.appendTag(slotNbt);
                }
            }
        }
        if (componentsNbt.tagCount() > 0) {
            nbt.setTag("componentSlots", componentsNbt);
        }

        FMLInterModComms.sendMessage(MOD_ID, "registerAssemblerTemplate", nbt);
    }

    /**
     * Register a new template for the disassembler.
     * <br>
     * The <tt>disassemble</tt> callback gets passed the item stack to
     * disassemble, and a list of inferred ingredients (based on crafting
     * recipes). This is useful for not having to compute those yourself when
     * you just want to add a number of items from an internal inventory to
     * the output (e.g. for servers it's the components in the server).
     * <br>
     * Signature of callbacks must be:
     * <pre>
     * boolean select(ItemStack stack)
     * Object disassemble(ItemStack stack, ItemStack[] ingredients)
     * </pre>
     * <br>
     * Where the <code>Object</code> returned from the <code>disassemble</code>
     * method must be one of the following:
     * <ul>
     * <li><code>ItemStack[]</code>: list of resulting items, subject to random failure.</li>
     * <li><code>Object[]{ItemStack[],ItemStack[]}</code>: two lists of resulting items, the first being subject to
     * random failure, the second being guaranteed drops (e.g. for item inventory contents).</li>
     * </ul>
     * <br>
     * Callbacks must be declared as <tt>packagePath.className.methodName</tt>.
     * For example: <tt>com.example.Integration.callbackMethod</tt>.
     *
     * @param name        the name of the handler (e.g. name of the item
     *                    being handled). Optional, only used in logging.
     * @param select      callback used to determine if the template
     *                    applies to an item.
     * @param disassemble callback used to apply a template and extract
     *                    ingredients from an item.
     */
    public static void registerDisassemblerTemplate(final String name, final String select, final String disassemble) {
        final NBTTagCompound nbt = new NBTTagCompound();
        if (name != null) {
            nbt.setString("name", name);
        }
        nbt.setString("select", select);
        nbt.setString("disassemble", disassemble);

        FMLInterModComms.sendMessage(MOD_ID, "registerDisassemblerTemplate", nbt);
    }

    /**
     * Register a callback for providing tool durability information.
     * <br>
     * If your provider does not handle a tool/item, return <tt>Double.NaN</tt>
     * to indicate that another provider should be queried. The first value
     * that isn't <tt>NaN</tt> will be used as the durability.
     * <br>
     * The returned value must be the <em>relative</em> durability of the tool,
     * in a range of [0,1], with 0 being broken, 1 being new/fully repaired.
     * <br>
     * Signature of callbacks must be:
     * <pre>
     * double callback(ItemStack stack)
     * </pre>
     * <br>
     * Callbacks must be declared as <tt>packagePath.className.methodName</tt>.
     * For example: <tt>com.example.Integration.callbackMethod</tt>.
     *
     * @param callback the callback to register as a durability provider.
     */
    public static void registerToolDurabilityProvider(final String callback) {
        FMLInterModComms.sendMessage(MOD_ID, "registerToolDurabilityProvider", callback);
    }

    /**
     * Register a callback handling a wrench tool.
     * <br>
     * These are used when determining whether an item is a wrench tool, when
     * interacting with certain blocks while the player is holding such an item,
     * for example to avoid rotating blocks when opening their GUI.
     * <br>
     * The returned value must be <tt>true</tt> if the wrench was used/usable,
     * <tt>false</tt> otherwise.
     * <br>
     * Signature of callbacks must be:
     * <pre>
     * boolean callback(EntityPlayer player, BlockPos pos, boolean changeDurability)
     * </pre>
     * <br>
     * Callbacks must be declared as <tt>packagePath.className.methodName</tt>.
     * For example: <tt>com.example.Integration.callbackMethod</tt>.
     *
     * @param callback the callback to register as a wrench tool handler.
     */
    public static void registerWrenchTool(final String callback) {
        FMLInterModComms.sendMessage(MOD_ID, "registerWrenchTool", callback);
    }

    /**
     * Register a callback for checking if an item is a wrench.
     * <br>
     * This is used to determine whether certain item stacks are wrench items,
     * which is used, for example, when "itemizing" a drone.
     * <br>
     * The returned value must <tt>true</tt> if the item stack is a wrench,
     * <tt>false</tt> otherwise.
     * <br>
     * Signature of callbacks must be:
     * <pre>
     * boolean callback(ItemStack stack)
     * </pre>
     * <br>
     * Callbacks must be declared as <tt>packagePath.className.methodName</tt>.
     * For example: <tt>com.example.Integration.callbackMethod</tt>.
     *
     * @param callback the callback to register as a wrench tool tester.
     */
    public static void registerWrenchToolCheck(final String callback) {
        FMLInterModComms.sendMessage(MOD_ID, "registerWrenchToolCheck", callback);
    }

    /**
     * Register a handler for items that can be charged.
     * <br>
     * This is used by the charger to determine whether items can be charged
     * by it (<tt>canCharge</tt>) and to actually charge them (<tt>charge</tt>).
     * <br>
     * Note that OpenComputers comes with a few built-in handlers for third-
     * party charged items, such as Redstone Flux and IndustrialCraft 2.
     * <br>
     * Signature of callbacks must be:
     * <pre>
     * boolean canCharge(ItemStack stack)
     * double charge(ItemStack stack, double amount, boolean simulate)
     * </pre>
     * <br>
     * Callbacks must be declared as <tt>packagePath.className.methodName</tt>.
     * For example: <tt>com.example.Integration.callbackMethod</tt>.
     *
     * @param name      the name of the energy system/item type handled.
     * @param canCharge the callback to register for checking chargeability.
     * @param charge    the callback to register for charging items.
     */
    public static void registerItemCharge(final String name, final String canCharge, final String charge) {
        final NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("name", name);
        nbt.setString("canCharge", canCharge);
        nbt.setString("charge", charge);
        FMLInterModComms.sendMessage(MOD_ID, "registerItemCharge", nbt);
    }

    /**
     * Register a provider for ink usable in the 3D printer.
     * <br>
     * Default providers in OpenComputers are one for the ink cartridges as
     * well as one for arbitrary dyes (via the OreDictionary).
     * <br>
     * Use this to make other items usable as ink in the 3D printer. Return a
     * value larger than zero to indicate you handled the provided item stack,
     * with the value being the amount of ink provided by the stack.
     * <br>
     * Signature of callbacks must be:
     * <pre>
     * int callback(ItemStack stack)
     * </pre>
     * <br>
     * Callbacks must be declared as <tt>packagePath.className.methodName</tt>.
     * For example: <tt>com.example.Integration.callbackMethod</tt>.
     *
     * @param callback the callback to register as an ink provider.
     */
    public static void registerInkProvider(final String callback) {
        FMLInterModComms.sendMessage(MOD_ID, "registerInkProvider", callback);
    }

    /**
     * Blacklist a ComputerCraft peripheral from being wrapped by OpenComputers'
     * built-in driver for ComputerCraft peripherals.
     * <br>
     * Use this if you provide a driver for something that is a peripheral and
     * wish to avoid conflicts in the registered callbacks, for example.
     *
     * @param peripheral the class of the peripheral to blacklist.
     */
    public static void blacklistPeripheral(final Class peripheral) {
        FMLInterModComms.sendMessage(MOD_ID, "blacklistPeripheral", peripheral.getName());
    }

    /**
     * Blacklist an item for a specified host.
     * <br>
     * This can be used to prevent certain components to be installed in select
     * devices, via the devices class. For example, this is used to prevent
     * components that would not be functional in certain devices to be
     * installed in those devices, such as graphics cards in micro-controllers.
     * <br>
     * The host class is the class of the environment the component would be
     * installed in, e.g. {@link li.cil.oc.api.internal.Tablet}.
     *
     * @param name  the name of the component being blacklisted.
     * @param host  the class of the host to blacklist the component for.
     * @param stack the item stack representing the blacklisted component.
     */
    public static void blacklistHost(final String name, final Class host, final ItemStack stack) {
        final NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("name", name);
        nbt.setString("host", host.getName());
        final NBTTagCompound stackNbt = new NBTTagCompound();
        stack.writeToNBT(stackNbt);
        nbt.setTag("item", stackNbt);
        FMLInterModComms.sendMessage(MOD_ID, "blacklistHost", nbt);
    }

    /**
     * Notifies OpenComputers that there is some 3rd-party power system present
     * that adds integration on its side.
     * <br>
     * This will suppress the "no power system found" message on start up, and
     * avoid auto-disabling power use.
     */
    public static void registerCustomPowerSystem() {
        FMLInterModComms.sendMessage(MOD_ID, "registerCustomPowerSystem", "true");
    }

    /**
     * Register a mapping of program name to loot disk.
     * <br>
     * The table of mappings is made available to machines to allow displaying
     * a message to the user telling her on which floppy disk to find the program
     * they were trying to run.
     * <br>
     * For Lua programs, this should be the program <em>name</em>, i.e. the file
     * name without the <code>.lua</code> extension.
     * <br>
     * The list of architectures is optional, if it is not specified this mapping
     * will be made available to all architectures. It allows filtering since
     * typically programs will be written for one specific architecture type, e.g.
     * Lua programs will not (directly) work on a MIPS architecture. The name
     * specified is the in the {@link li.cil.oc.api.machine.Architecture.Name}
     * annotation of the architecture (also shown in the CPU tooltip).
     * <br>
     * The architecture names for Lua are <code>Lua 5.2</code>, <code>Lua 5.3</code>
     * and <code>LuaJ</code> for example.
     *
     * @param programName   the name of the program.
     * @param diskLabel     the label of the disk the program is on.
     * @param architectures the names of the architectures this entry applies to.
     */
    public static void registerProgramDiskLabel(final String programName, final String diskLabel, final String... architectures) {
        final NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("program", programName);
        nbt.setString("label", diskLabel);
        if (architectures != null && architectures.length > 0) {
            final NBTTagList architecturesNbt = new NBTTagList();
            for (final String architecture : architectures) {
                architecturesNbt.appendTag(new NBTTagString(architecture));
            }
            nbt.setTag("architectures", architecturesNbt);
        }
        FMLInterModComms.sendMessage(MOD_ID, "registerProgramDiskLabel", nbt);
    }

    // ----------------------------------------------------------------------- //

    private static final String MOD_ID = "OpenComputers";

    private IMC() {
    }
}
