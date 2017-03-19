package li.cil.oc.common.tileentity;

import li.cil.oc.Settings;
import li.cil.oc.client.Sound;
import li.cil.oc.common.SaveHandler;
import li.cil.oc.common.tileentity.traits.TileEntityAccess;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

public abstract class AbstractTileEntity extends TileEntity implements TileEntityAccess {
    public boolean isClient() {
        return !isServer();
    }

    public boolean isServer() {
        return hasWorld() && getWorld().isRemote;
    }

    // ----------------------------------------------------------------------- //

    protected void dispose() {
        if (isClient()) {
            Sound.stopLoop(this);
        }
    }

    protected boolean mayTickEnergy() {
        // We use hashCode() as a (shitty but good enough) random value to
        // distribute updates of different tile entities across ticks.
        return (getWorld().getTotalWorldTime() + hashCode()) % Settings.Power.tickFrequency == 0;
    }

    protected void readFromNBTForServer(final NBTTagCompound nbt) {
        readFromNBTCommon(nbt);
    }

    protected void writeToNBTForServer(final NBTTagCompound nbt) {
        writeToNBTCommon(nbt);
    }

    protected void readFromNBTForClient(final NBTTagCompound nbt) {
        readFromNBTCommon(nbt);
    }

    protected void writeToNBTForClient(final NBTTagCompound nbt) {
        writeToNBTCommon(nbt);
    }

    protected void readFromNBTCommon(final NBTTagCompound nbt) {
    }

    protected void writeToNBTCommon(final NBTTagCompound nbt) {
    }

    // ----------------------------------------------------------------------- //
    // TileEntity

    @Override
    public void invalidate() {
        super.invalidate();
        dispose();
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        dispose();
    }

    @Override
    public void readFromNBT(final NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        readFromNBTForServer(nbt);
    }

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound nbtIn) {
        final NBTTagCompound nbt = super.writeToNBT(nbtIn);
        writeToNBTForServer(nbt);
        return nbt;
    }

    @Override
    public void onDataPacket(final NetworkManager manager, final SPacketUpdateTileEntity packet) {
        readFromNBTForClient(packet.getNbtCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        final NBTTagCompound nbt = new NBTTagCompound();
        writeToNBTForClient(nbt);
        return new SPacketUpdateTileEntity(getPos(), 0, nbt);
    }

    @Override
    public void handleUpdateTag(final NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        readFromNBTForClient(nbt);
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        final NBTTagCompound nbt = super.getUpdateTag();

        SaveHandler.savingForClients_$eq(true);
        try {
            writeToNBTForClient(nbt);
        } finally {
            SaveHandler.savingForClients_$eq(false);
        }
        writeToNBTForClient(nbt);
        return nbt;
    }

    // ----------------------------------------------------------------------- //
    // TileEntityAccess

    @Override
    public TileEntity getTileEntity() {
        return this;
    }
}
