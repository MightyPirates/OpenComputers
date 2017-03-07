package li.cil.oc.common.tileentity.traits;

import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Node;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;

public interface NotAnalyzable extends Analyzable {
    @Nullable
    @Override
    default Node[] onAnalyze(final EntityPlayer player, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        return null;
    }
}
