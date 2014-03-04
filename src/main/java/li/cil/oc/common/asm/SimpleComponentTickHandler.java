package li.cil.oc.common.asm;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import li.cil.oc.api.Network;
import net.minecraft.tileentity.TileEntity;

import java.util.ArrayList;

// This class is used for adding simple components to the component network.
// It is triggered from a validate call, and executed in the next update tick.
public final class SimpleComponentTickHandler {
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

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent e) {
        synchronized (pendingAdds) {
            for (Runnable runnable : pendingAdds) {
                runnable.run();
            }
            pendingAdds.clear();
        }
    }
}
