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
public class SimpleSignalController extends SignalController {

    private SignalAspect aspect = SignalAspect.BLINK_RED;

    public SimpleSignalController(String desc, TileEntity tile) {
        super(desc, tile, 1);
    }

    public SignalAspect getAspect() {
        return aspect;
    }

    @Override
    public SignalAspect getAspectFor(WorldCoordinate receiver) {
        if(!pairings.contains(receiver)){
            return null;
        }
        return aspect;
    }

    private void updateReceiver() {
        for (WorldCoordinate recv : pairings) {
            SignalReceiver receiver = getReceiverAt(recv);
            if (receiver != null) {
                receiver.onControllerAspectChange(this, aspect);
            }
        }
        cleanPairings();
    }

    public void setAspect(SignalAspect aspect) {
        if (this.aspect != aspect) {
            this.aspect = aspect;
            updateReceiver();
        }
    }

    @Override
    protected void saveNBT(NBTTagCompound data) {
        super.saveNBT(data);
        data.setByte("aspect", (byte) aspect.ordinal());
    }

    @Override
    protected void loadNBT(NBTTagCompound data) {
        super.loadNBT(data);
        aspect = SignalAspect.values()[data.getByte("aspect")];
    }

    public void writePacketData(DataOutputStream data) throws IOException {
        data.writeByte(aspect.ordinal());
    }

    public void readPacketData(DataInputStream data) throws IOException {
        aspect = SignalAspect.values()[data.readByte()];
    }
    
    @Override
    public String toString(){
        return "Controller: " + aspect.toString();
    }
}
