package li.cil.oc.common.asm;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import li.cil.oc.api.Network;
import net.minecraft.tileentity.TileEntity;

import java.util.ArrayList;
import java.util.EnumSet;

// This class is used for adding simple components to the component network.
// It is triggered from a validate call, and executed in the next update tick.
public final class SimpleComponentTickHandler implements ITickHandler {
    public static final ArrayList<Runnable> pendingAdds = new java.util.ArrayList<Runnable>();

    public static final SimpleComponentTickHandler Instance = new SimpleComponentTickHandler();

    private SimpleComponentTickHandler() {
    }

    public static void schedule(final TileEntity tileEntity) {
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            synchronized (pendingAdds) {
                pendingAdds.add(new Runnable() {
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
        synchronized (pendingAdds) {
            for (Runnable runnable : pendingAdds) {
                runnable.run();
            }
            pendingAdds.clear();
        }
    }
}
