package li.cil.oc.api.prefab;

import li.cil.oc.api.machine.Value;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Context;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Basic implementation for the <tt>Value</tt> interface.
 */
public class AbstractValue implements Value {
    @Override
    public Object apply(Context context, Arguments arguments) {
        return null;
    }

    @Override
    public void unapply(Context context, Arguments arguments) {
    }

    @Override
    public Object[] call(Context context, Arguments arguments) {
        throw new RuntimeException("trying to call a non-callable value");
    }

    @Override
    public void dispose(Context context) {
    }

    @Override
    public void load(NBTTagCompound nbt) {
    }

    @Override
    public void save(NBTTagCompound nbt) {
    }
}
