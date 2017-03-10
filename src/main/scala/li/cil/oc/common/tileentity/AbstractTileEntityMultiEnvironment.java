package li.cil.oc.common.tileentity;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.network.Environment;
import li.cil.oc.common.capabilities.CapabilityEnvironment;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;

public abstract class AbstractTileEntityMultiEnvironment extends AbstractTileEntityEnvironmentHost {
    // ----------------------------------------------------------------------- //
    // Computed data.

    // NBT tag names.
    private static final String TAG_ENVIRONMENTS = "environments";

    // ----------------------------------------------------------------------- //
    // TileEntity

    @Override
    public boolean hasCapability(final Capability<?> capability, @Nullable final EnumFacing facing) {
        return (capability == CapabilityEnvironment.ENVIRONMENT_CAPABILITY && facing != null && getEnvironments()[facing.ordinal()] != null) ||
                super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getCapability(final Capability<T> capability, @Nullable final EnumFacing facing) {
        if (capability == CapabilityEnvironment.ENVIRONMENT_CAPABILITY && facing != null)
            return (T) getEnvironments()[facing.ordinal()];
        return super.getCapability(capability, facing);
    }

    // ----------------------------------------------------------------------- //
    // AbstractTileEntity

    @Override
    protected void readFromNBTCommon(final NBTTagCompound nbt) {
        super.readFromNBTCommon(nbt);
        final NBTTagList nbtList = new NBTTagList();
        for (final Environment environment : getEnvironments()) {
            nbtList.appendTag(environment.serializeNBT());
        }
        nbt.setTag(TAG_ENVIRONMENTS, nbtList);
    }

    @Override
    protected void writeToNBTCommon(final NBTTagCompound nbt) {
        super.writeToNBTCommon(nbt);
        final NBTTagList nbtList = nbt.getTagList(TAG_ENVIRONMENTS, Constants.NBT.TAG_COMPOUND);
        final Environment[] environments = getEnvironments();
        if (nbtList.tagCount() == environments.length) {
            for (int i = 0; i < environments.length; i++) {
                environments[i].deserializeNBT(nbtList.getCompoundTagAt(i));
            }
        } else {
            OpenComputers.log().warn("Environment count mismatch (got {}, expected {}). Not loading environments.", nbtList.tagCount(), environments.length);
        }
    }

    // ----------------------------------------------------------------------- //

    protected abstract Environment[] getEnvironments();
}
