package li.cil.oc.common.asm;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

import java.util.ArrayList;

// This class is used for adding simple components to the component network.
// It is triggered from a validate call, and executed in the next update tick.
public final class SimpleComponentTickHandler {
    public static final ArrayList<Runnable> pendingOperations = new java.util.ArrayList<Runnable>();

    public static final SimpleComponentTickHandler Instance = new SimpleComponentTickHandler();

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent e) {
        synchronized (pendingOperations) {
            for (Runnable runnable : pendingOperations) {
                runnable.run();
            }
            pendingOperations.clear();
        }
    }
}
