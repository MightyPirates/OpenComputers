package li.cil.oc.common.asm.template;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.SimpleComponent;
import net.minecraft.nbt.NBTTagCompound;

// This interface defines the names to which existing or placeholders for
// existing methods will be moved. This allows transparent injection of our
// functionality, i.e. existing validate() etc. methods will be called as
// if we didn't inject our code.
public interface SimpleComponentImpl extends Environment, SimpleComponent {
    void validate0();

    void invalidate0();

    void onChunkUnload0();

    void readFromNBT0(NBTTagCompound nbt);

    void writeToNBT0(NBTTagCompound nbt);
}
