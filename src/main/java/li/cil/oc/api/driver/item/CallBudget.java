package li.cil.oc.api.driver.item;

import net.minecraft.item.ItemStack;

/**
 * Common functionality provided by parts that influence a machine's speed.
 * <p/>
 * By default, this is implemented by OpenComputers' {@link Processor}s and
 * {@link Memory}.
 * <p/>
 * The actual call budget of a machine is set to the average of
 * the specified call budget of all present components.
 * <p/>
 * A processor and memory implementation may choose not to implement this
 * interface. If no component providing a call budget it present in a machine,
 * a value of <tt>1.0</tt> will be used, i.e. the "default" speed modifier.
 */
public interface CallBudget {
    /**
     * The budget for direct calls provided by the specified component.
     * <p/>
     * For reference, the default budgets for OpenComputers' processors are
     * 0.5, 1.0 and 1.5 for tier one, two and three, respectively. This means
     * you can consider it a multiplier for the machine's operation speed.
     *
     * @param stack the stack representing the part to get the call budget for.
     * @return the budget for direct calls per tick provided.
     */
    double getCallBudget(ItemStack stack);
}
