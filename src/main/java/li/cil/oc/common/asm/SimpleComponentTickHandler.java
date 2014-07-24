package li.cil.oc.common.asm;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import li.cil.oc.api.Network;
import li.cil.oc.util.SideTracker;
import net.minecraft.tileentity.TileEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

// This class is used for adding simple components to the component network.
// It is triggered from a validate call, and executed in the next update tick.
public final class SimpleComponentTickHandler {
    private static final Logger log = LogManager.getLogger("OpenComputers");

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

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.START) {
            final Runnable[] adds;
            synchronized (pending) {
                adds = pending.toArray(new Runnable[pending.size()]);
                pending.clear();
            }
            for (Runnable runnable : adds) {
                try {
                    runnable.run();
                } catch (Throwable t) {
                    log.warn("Error in scheduled tick action.", t);
                }
            }
        }
    }
}
