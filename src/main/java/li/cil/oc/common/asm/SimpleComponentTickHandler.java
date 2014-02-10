package li.cil.oc.common.asm;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;

import java.util.ArrayList;
import java.util.EnumSet;

// This class is used for adding simple components to the component network.
// It is triggered from a validate call, and executed in the next update tick.
public final class SimpleComponentTickHandler implements ITickHandler {
    public static final ArrayList<Runnable> pendingOperations = new java.util.ArrayList<Runnable>();

    public static final SimpleComponentTickHandler Instance = new SimpleComponentTickHandler();

    private SimpleComponentTickHandler() {
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
        synchronized (pendingOperations) {
            for (Runnable runnable : pendingOperations) {
                runnable.run();
            }
            pendingOperations.clear();
        }
    }
}
