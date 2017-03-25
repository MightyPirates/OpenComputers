package li.cil.oc.server;

import li.cil.oc.common.AbstractGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public final class GuiHandlerServer extends AbstractGuiHandler {
    @Nullable
    @Override
    public Object getClientGuiElement(final int ID, final EntityPlayer player, final World world, final int x, final int y, final int z) {
        return null;
    }
}
