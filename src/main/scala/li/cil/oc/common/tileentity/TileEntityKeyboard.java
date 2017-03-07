package li.cil.oc.common.tileentity;

import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.capabilities.CapabilityEnvironment;
import li.cil.oc.common.tileentity.traits.RotatableImpl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public final class TileEntityKeyboard extends AbstractTileEntityEnvironmentHost implements RotatableImpl.RotatableHost, Analyzable {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final RotatableImpl rotatable = new RotatableImpl(this);
    private final Environment keyboard = new li.cil.oc.server.component.Keyboard(this);

    // ----------------------------------------------------------------------- //
    // Computed data.

    // NBT tag names.
    private static final String KEYBOARD_TAG = "keyboard";

    // ----------------------------------------------------------------------- //
    // AbstractTileEntityEnvironmentHost

    @Override
    protected Environment getEnvironment() {
        return keyboard;
    }

    // ----------------------------------------------------------------------- //
    // AbstractTileEntity

    @Override
    protected void readFromNBTCommon(final NBTTagCompound nbt) {
        super.readFromNBTCommon(nbt);
        keyboard.deserializeNBT(nbt);
    }

    @Override
    protected void writeToNBTCommon(final NBTTagCompound nbt) {
        super.writeToNBTCommon(nbt);
        nbt.setTag(KEYBOARD_TAG, keyboard.serializeNBT());
    }

    // ----------------------------------------------------------------------- //
    // RotatableHost

    @Override
    public void onRotationChanged() {
        getWorld().notifyNeighborsOfStateChange(getPos(), getBlockType(), false);
    }

    // ----------------------------------------------------------------------- //
    // Analyzable

    @Override
    public Node[] onAnalyze(final EntityPlayer player, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        return new Node[]{keyboard.getNode()};
    }

    // ----------------------------------------------------------------------- //
    // TileEntity

    @Override
    public boolean hasCapability(final Capability<?> capability, @Nullable final EnumFacing facing) {
        if (capability == CapabilityEnvironment.ENVIRONMENT_CAPABILITY && !hasNodeOnSide(facing))
            return false;
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(final Capability<T> capability, @Nullable final EnumFacing facing) {
        if (capability == CapabilityEnvironment.ENVIRONMENT_CAPABILITY && !hasNodeOnSide(facing))
            return null;
        return super.getCapability(capability, facing);
    }

    // ----------------------------------------------------------------------- //

    private boolean hasNodeOnSide(@Nullable final EnumFacing side) {
        if (side == null) {
            return false;
        }
        if (side.getOpposite() == rotatable.getFacing()) {
            return true;
        }
        if (side == getForward()) {
            return true;
        }
        if (isOnWall() && side.getOpposite() == EnumFacing.UP) {
            return true;
        }
        return false;
    }

    private boolean isOnWall() {
        final EnumFacing facing = rotatable.getFacing();
        return facing != EnumFacing.UP && facing != EnumFacing.DOWN;
    }

    private EnumFacing getForward() {
        return isOnWall() ? EnumFacing.UP : rotatable.getYaw().toEnumFacing();
    }
}
