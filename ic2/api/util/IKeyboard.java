package ic2.api.util;

import net.minecraft.entity.player.EntityPlayer;

public interface IKeyboard {
	boolean isAltKeyDown(EntityPlayer player);
	boolean isBoostKeyDown(EntityPlayer player);
	boolean isForwardKeyDown(EntityPlayer player);
	boolean isJumpKeyDown(EntityPlayer player);
	boolean isModeSwitchKeyDown(EntityPlayer player);
	boolean isSideinventoryKeyDown(EntityPlayer player);
	boolean isHudModeKeyDown(EntityPlayer player);
	boolean isSneakKeyDown(EntityPlayer player);
}
