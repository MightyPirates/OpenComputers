package li.cil.oc.common.tileentity.traits;

import li.cil.oc.api.tileentity.Rotatable;
import li.cil.oc.api.util.Pitch;
import li.cil.oc.api.util.Yaw;
import li.cil.oc.util.RotationUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

public final class RotatableImpl implements Rotatable {
    public interface RotatableHost extends TileEntityAccess {
        default void onRotationChanged() {
        }
    }

    // ----------------------------------------------------------------------- //
    // Persisted data.

    private Pitch pitch = Pitch.FORWARD;
    private Yaw yaw = Yaw.SOUTH;

    // ----------------------------------------------------------------------- //
    // Computed data.

    // Mappings.
    private static final Pitch[] ANGLE_TO_PITCH = {Pitch.UP, Pitch.FORWARD, Pitch.DOWN};
    private static final Yaw[] ANGLE_TO_YAW = {Yaw.SOUTH, Yaw.WEST, Yaw.NORTH, Yaw.EAST};

    private final RotatableHost host;
    private EnumFacing[] validRotations;

    // ----------------------------------------------------------------------- //

    public RotatableImpl(final RotatableHost host) {
        this.host = host;
        updateValidRotations(false);
    }

    public void setFromFacing(final EnumFacing facing) {
        switch (facing) {
            case DOWN:
            case UP:
                setPitch(Pitch.fromFacing(facing));
                break;
            case NORTH:
            case SOUTH:
            case WEST:
            case EAST:
                setYaw(Yaw.fromFacing(facing));
                break;
        }
    }

    public void setFromEntityPitchAndYaw(final Entity entity) {
        final Pitch pitch = ANGLE_TO_PITCH[Math.round(entity.rotationPitch / 90) + 1];
        final Yaw yaw = ANGLE_TO_YAW[Math.round(entity.rotationYaw / 360 * 4) & 3];

        if (pitch == this.pitch && yaw == this.yaw) {
            return;
        }

        this.pitch = pitch;
        this.yaw = yaw;
        updateValidRotations(true);
    }

    // ----------------------------------------------------------------------- //
    // Rotatable

    @Override
    public Pitch getPitch() {
        return pitch;
    }

    @Override
    public boolean setPitch(final Pitch value) {
        if (pitch == value) {
            return false;
        }
        pitch = value;
        updateValidRotations(true);
        return true;
    }

    @Override
    public Yaw getYaw() {
        return yaw;
    }

    @Override
    public boolean setYaw(final Yaw value) {
        if (yaw == value) {
            return false;
        }
        yaw = value;
        updateValidRotations(true);
        return true;
    }

    @Override
    public boolean rotate(final EnumFacing.Axis around) {
        final TileEntity tileEntity = host.getTileEntity();
        final IBlockState state = tileEntity.getWorld().getBlockState(tileEntity.getPos());
        final EnumFacing[] valid = state.getBlock().getValidRotations(tileEntity.getWorld(), tileEntity.getPos());
        if (valid != null && ArrayUtils.contains(valid, around)) {
            final EnumFacing rotated = getFacing().rotateAround(around);
            if (rotated == EnumFacing.UP || rotated == EnumFacing.DOWN) {
                return setPitch(Pitch.fromFacing(rotated));
            } else {
                return setYaw(Yaw.fromFacing(rotated));
            }
        }
        return false;
    }

    @Override
    public EnumFacing getFacing() {
        if (pitch != Pitch.FORWARD) {
            return pitch.toEnumFacing();
        } else {
            return yaw.toEnumFacing();
        }
    }

    @Override
    public EnumFacing[] getValidRotations() {
        return validRotations;
    }

    @Override
    public EnumFacing toLocal(final EnumFacing value) {
        return RotationUtils.toLocal(pitch, yaw, value);
    }

    @Override
    public EnumFacing toGlobal(final EnumFacing value) {
        return RotationUtils.toGlobal(pitch, yaw, value);
    }

    // ----------------------------------------------------------------------- //
    // INBTSerializable

    @Override
    public NBTTagByte serializeNBT() {
        final byte compressed = (byte) (pitch.ordinal() << 2 | yaw.ordinal());
        return new NBTTagByte(compressed);
    }

    @Override
    public void deserializeNBT(final NBTTagByte nbt) {
        final byte compressed = nbt.getByte();
        pitch = Pitch.VALUES[(compressed >>> 2) & 0b11];
        yaw = Yaw.VALUES[compressed & 0b11];
        updateValidRotations(false);
    }

    // ----------------------------------------------------------------------- //

    private void updateValidRotations(final boolean notifyHost) {
        if (getPitch() == Pitch.FORWARD) {
            // All except the axis we're currently facing along.
            final EnumFacing.Axis axis = getFacing().getAxis();
            validRotations = Arrays.stream(EnumFacing.VALUES).
                    filter(facing -> facing.getAxis() != axis).
                    toArray(EnumFacing[]::new);
        } else {
            validRotations = EnumFacing.VALUES;
        }

        if (notifyHost) {
            host.onRotationChanged();
        }
    }
}
