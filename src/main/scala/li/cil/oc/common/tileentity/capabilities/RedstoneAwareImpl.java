package li.cil.oc.common.tileentity.capabilities;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.tileentity.RedstoneAware;
import li.cil.oc.integration.util.BundledRedstone;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IThreadListener;

import java.util.Arrays;

public final class RedstoneAwareImpl implements RedstoneAware {
    public interface RedstoneAwareHost extends EnvironmentHost {
        default void onRedstoneInputChanged(final EnumFacing side, final int oldValue, final int newValue) {
        }

        default void onRedstoneOutputChanged(final EnumFacing side, final int oldValue, final int newValue) {
        }

        default void onRedstoneOutputEnabledChanged() {
        }
    }

    // --------------------------------------------------------------------- //
    // Persisted data.

    private final int[] input = new int[EnumFacing.VALUES.length];
    private final int[] output = new int[EnumFacing.VALUES.length];
    private boolean isOutputEnabled;

    // --------------------------------------------------------------------- //
    // Computed data.

    // NBT tag names.
    private static final String TAG_INPUT = "input";
    private static final String TAG_OUTPUT = "output";
    private static final String TAG_OUTPUT_ENABLED = "outputEnabled";

    private final RedstoneAwareHost host;
    private boolean isInputUpdateScheduled;

    // --------------------------------------------------------------------- //

    public RedstoneAwareImpl(final RedstoneAwareHost host) {
        this.host = host;
    }

    // --------------------------------------------------------------------- //
    // RedstoneAware

    @Override
    public int getInput(final EnumFacing side) {
        return input[side.ordinal()];
    }

    @Override
    public int getOutput(final EnumFacing side) {
        return output[side.ordinal()];
    }

    @Override
    public void setOutput(final EnumFacing side, final int value) {
        final int oldValue = output[side.ordinal()];

        if (value == oldValue) {
            return;
        }

        output[side.ordinal()] = value;

        host.onRedstoneOutputChanged(side, oldValue, value);
    }

    @Override
    public boolean isOutputEnabled() {
        return isOutputEnabled;
    }

    @Override
    public boolean isOutputStrong() {
        return false; // TODO
    }

    @Override
    public void setOutputEnabled(final boolean value) {
        if (value == isOutputEnabled) {
            return;
        }

        isOutputEnabled = value;

        if (!isOutputEnabled) {
            Arrays.fill(output, 0);
        }

        host.onRedstoneOutputEnabledChanged();
    }

    @Override
    public int getMaxInput() {
        int max = 0;
        for (final int i : input) {
            if (i > max) {
                max = i;
            }
        }
        return max;
    }

    @Override
    public void scheduleInputUpdate() {
        if (isInputUpdateScheduled) {
            return;
        }

        isInputUpdateScheduled = true;

        final IThreadListener thread = host.getHostWorld().getMinecraftServer();
        if (thread != null) {
            thread.addScheduledTask(this::updateRedstoneInput);
        }
    }

    // ----------------------------------------------------------------------- //
    // INBTSerializable

    @Override
    public NBTTagCompound serializeNBT() {
        final NBTTagCompound nbt = new NBTTagCompound();
        nbt.setIntArray(TAG_INPUT, input.clone());
        nbt.setIntArray(TAG_OUTPUT, output.clone());
        nbt.setBoolean(TAG_OUTPUT_ENABLED, isOutputEnabled);
        return nbt;
    }

    @Override
    public void deserializeNBT(final NBTTagCompound nbt) {
        final int[] nbtInput = nbt.getIntArray(TAG_INPUT);
        if (nbtInput.length == input.length) {
            System.arraycopy(nbtInput, 0, input, 0, input.length);
        }
        final int[] nbtOutput = nbt.getIntArray(TAG_OUTPUT);
        if (nbtInput.length == input.length) {
            System.arraycopy(nbtOutput, 0, output, 0, output.length);
        }
    }

    // ----------------------------------------------------------------------- //

    private void updateRedstoneInput() {
        for (final EnumFacing side : EnumFacing.VALUES) {
            setInput(side, BundledRedstone.computeInput(host.getHostBlockPosition(), side));
        }
    }

    private void setInput(final EnumFacing side, final int value) {
        final int oldValue = input[side.ordinal()];

        if (value == oldValue) {
            return;
        }

        input[side.ordinal()] = value;

        host.onRedstoneInputChanged(side, oldValue, value);
    }
}
