/*
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at railcraft.wikispaces.com.
 */
package mods.railcraft.api.signals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import mods.railcraft.api.core.WorldCoordinate;

/**
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public class DualSignalReceiver extends SignalReceiver {

    private SignalAspect topAspect = SignalAspect.BLINK_RED;
    private SignalAspect bottomAspect = SignalAspect.BLINK_RED;

    public DualSignalReceiver(String desc, TileEntity tile) {
        super(desc, tile, 2);
    }

    @Override
    public void onControllerAspectChange(SignalController con, SignalAspect aspect) {
        WorldCoordinate coord = pairings.peekFirst();
        if (coord == null) {
            return;
        }
        if (coord.equals(con.getCoords())) {
            if (aspect != topAspect) {
                topAspect = aspect;
                super.onControllerAspectChange(con, aspect);
            }
        } else {
            if (aspect != bottomAspect) {
                bottomAspect = aspect;
                super.onControllerAspectChange(con, aspect);
            }
        }
    }

    @Override
    protected void saveNBT(NBTTagCompound data) {
        super.saveNBT(data);
        data.setByte("topAspect", (byte) topAspect.ordinal());
        data.setByte("bottomAspect", (byte) bottomAspect.ordinal());
    }

    @Override
    protected void loadNBT(NBTTagCompound data) {
        super.loadNBT(data);
        topAspect = SignalAspect.values()[data.getByte("topAspect")];
        bottomAspect = SignalAspect.values()[data.getByte("bottomAspect")];
    }

    public void writePacketData(DataOutputStream data) throws IOException {
        data.writeByte(topAspect.ordinal());
        data.writeByte(bottomAspect.ordinal());
    }

    public void readPacketData(DataInputStream data) throws IOException {
        topAspect = SignalAspect.values()[data.readByte()];
        bottomAspect = SignalAspect.values()[data.readByte()];
    }

    public SignalAspect getTopAspect() {
        return topAspect;
    }

    public SignalAspect getBottomAspect() {
        return bottomAspect;
    }

    public boolean setTopAspect(SignalAspect aspect) {
        if (topAspect != aspect) {
            topAspect = aspect;
            return true;
        }
        return false;
    }

    public boolean setBottomAspect(SignalAspect aspect) {
        if (bottomAspect != aspect) {
            bottomAspect = aspect;
            return true;
        }
        return false;
    }
}
