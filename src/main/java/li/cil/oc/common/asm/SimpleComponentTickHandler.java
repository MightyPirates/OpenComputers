package li.cil.oc.common.asm;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import li.cil.oc.api.Network;
import li.cil.oc.util.SideTracker;
import net.minecraft.tileentity.TileEntity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

// This class is used for adding simple components to the component network.
// It is triggered from a validate call, and executed in the next update tick.
public final class SimpleComponentTickHandler implements ITickHandler {
    private static final Logger log = Logger.getLogger("OpenComputers");

    public static final ArrayList<Runnable> pending = new java.util.ArrayList<Runnable>();

    public static final SimpleComponentTickHandler Instance = new SimpleComponentTickHandler();

    private SimpleComponentTickHandler() {
    }

    public static void schedule(final TileEntity tileEntity) {
        if (SideTracker.isServer()) {
            synchronized (pending) {
                pending.add(new Runnable() {
                    @Override
                    public void run() {
                        Network.joinOrCreateNetwork(tileEntity);
                    }
                });
            }
        }
    }

    @Override
    public String getLabel() {
        return "OpenComputers SimpleComponent Ticker";
    }

    @Override
    public EnumSet<TickType> ticks() {
        return EnumSet.of(TickType.SERVER);
    }

    @Override
    public void tickStart(EnumSet<TickType> type, Object... tickData) {
    }

    @Override
    public void tickEnd(EnumSet<TickType> type, Object... tickData) {
        final Runnable[] adds;
        synchronized (pending) {
            adds = pending.toArray(new Runnable[pending.size()]);
            pending.clear();
        }
        for (Runnable runnable : adds) {
            try {
                runnable.run();
            } catch (Throwable t) {
                log.log(Level.WARNING, "Error in scheduled tick action.", t);
            }
        }
    }
}
