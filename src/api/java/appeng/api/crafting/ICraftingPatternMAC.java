package appeng.api.crafting;

import net.minecraft.world.World;

public interface ICraftingPatternMAC extends ICraftingPattern
{

	boolean isEncoded();

	boolean isCraftable(World theWorld);

}
