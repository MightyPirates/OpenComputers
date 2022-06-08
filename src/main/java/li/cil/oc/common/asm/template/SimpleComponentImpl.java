package li.cil.oc.common.asm.template;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.SimpleComponent;
import net.minecraft.nbt.NBTTagCompound;

/**
 * This interface defines the names to which existing or placeholders for
 * existing methods will be moved. This allows transparent injection of our
 * functionality, i.e. existing validate() etc. methods will be called as
 * if we didn't inject our code.
 * <br>
 * Yes, the names are not "conventional", but that is by design, to avoid
 * naming collisions.
 */
public interface SimpleComponentImpl extends Environment, SimpleComponent {
    public static final String PostFix = "_OpenComputers";

    void validate_OpenComputers();

    void invalidate_OpenComputers();

    void onChunkUnload_OpenComputers();

    void readFromNBT_OpenComputers(NBTTagCompound nbt);

    void writeToNBT_OpenComputers(NBTTagCompound nbt);
}
