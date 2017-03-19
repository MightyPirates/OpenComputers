package li.cil.oc.common.tileentity.traits;

import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.api.util.Location;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;

public final class NetworkBridgeAdapterWireless extends AbstractNodeBridgeAdapter implements WirelessEndpoint, INBTSerializable<NBTTagCompound> {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private double strength = Settings.Misc.maxWirelessRange;
    private boolean isRepeater = false;

    // ----------------------------------------------------------------------- //
    // Computed data.

    // NBT tag names.
    private static final String TAG_STRENGTH = "strength";
    private static final String TAG_REPEATER = "repeater";

    private final Location location;

    // ----------------------------------------------------------------------- //

    public NetworkBridgeAdapterWireless(final Location location) {
        this.location = location;
    }

    public double getStrength() {
        return strength;
    }

    public void setStrength(final double value) {
        strength = MathHelper.clamp(value, 0, Settings.Misc.maxWirelessRange);
    }

    public boolean isRepeater() {
        return isRepeater;
    }

    public void setRepeater(final boolean value) {
        isRepeater = value;
    }

    // ----------------------------------------------------------------------- //
    // AbstractNodeBridgeAdapter

    @Override
    protected void onEnabled() {
        Network.joinWirelessNetwork(this);
    }

    @Override
    protected void onDisabled() {
        Network.leaveWirelessNetwork(this);
    }

    @Override
    protected void sendPacket(final int receivePort, final Packet packet) {
        if (receivePort == getPort() && !isRepeater()) {
            return;
        }

        Network.sendWirelessPacket(this, strength, packet);
    }

    // ----------------------------------------------------------------------- //
    // WirelessEndpoint

    @Override
    public World getWorld() {
        return location.getHostWorld();
    }

    @Override
    public BlockPos getPosition() {
        return location.getHostBlockPosition();
    }

    @Override
    public void receivePacket(final Packet packet, final WirelessEndpoint sender) {
        tryEnqueuePacket(packet);
    }

    // ----------------------------------------------------------------------- //
    // INBTSerializable

    @Override
    public NBTTagCompound serializeNBT() {
        final NBTTagCompound nbt = new NBTTagCompound();
        nbt.setDouble(TAG_STRENGTH, strength);
        nbt.setBoolean(TAG_REPEATER, isRepeater);
        return nbt;
    }

    @Override
    public void deserializeNBT(final NBTTagCompound nbt) {
        strength = nbt.getDouble(TAG_STRENGTH);
        isRepeater = nbt.getBoolean(TAG_REPEATER);
    }
}
