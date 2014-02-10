/*
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at railcraft.wikispaces.com.
 */
package mods.railcraft.api.signals;

import net.minecraft.tileentity.TileEntity;
import mods.railcraft.api.core.WorldCoordinate;

/**
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public abstract class SignalController extends AbstractPair {

    public SignalController(String name, TileEntity tile, int maxPairings) {
        super(name, tile, maxPairings);
    }

    public SignalReceiver getReceiverAt(WorldCoordinate coord) {
        TileEntity recv = getPairAt(coord);
        if (recv != null) {
            return ((IReceiverTile) recv).getReceiver();
        }
        return null;
    }

    public abstract SignalAspect getAspectFor(WorldCoordinate receiver);

    public boolean sendAspectTo(WorldCoordinate receiver, SignalAspect aspect) {
        SignalReceiver recv = getReceiverAt(receiver);
        if (recv != null) {
            recv.onControllerAspectChange(this, aspect);
            return true;
        }
        return false;
    }

    @Override
    protected String getTagName() {
        return "controller";
    }

    @Override
    public boolean isValidPair(TileEntity tile) {
        if (tile instanceof IReceiverTile) {
            SignalReceiver receiver = ((IReceiverTile) tile).getReceiver();
            return receiver.isPairedWith(getCoords());
        }
        return false;
    }

    @Deprecated
    public void registerLegacyReceiver(int x, int y, int z) {
        pairings.add(new WorldCoordinate(0, x, y, z));
    }

    public void registerReceiver(SignalReceiver receiver) {
        WorldCoordinate coords = receiver.getCoords();
        addPairing(coords);
        receiver.registerController(this);
        receiver.onControllerAspectChange(this, getAspectFor(coords));
    }

    @Override
    public void tickClient() {
        super.tickClient();
        if (SignalTools.effectManager != null && SignalTools.effectManager.isTuningAuraActive()) {
            for (WorldCoordinate coord : pairings) {
                SignalReceiver receiver = getReceiverAt(coord);
                if (receiver != null) {
                    SignalTools.effectManager.tuningEffect(getTile(), receiver.getTile());
                }
            }
        }
    }
}
