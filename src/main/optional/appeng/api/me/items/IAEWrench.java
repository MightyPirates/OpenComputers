package appeng.api.me.items;

import net.minecraft.entity.player.EntityPlayer;

public interface IAEWrench {

	boolean canWrench(EntityPlayer player, int x, int y, int z);

}
