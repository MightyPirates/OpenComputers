package forestry.api.core;

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.util.Icon;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Provides icons, needed in some interfaces, most notably for bees and trees. 
 */
public interface IIconProvider {
	
	@SideOnly(Side.CLIENT)
	Icon getIcon(short texUID);
	
	@SideOnly(Side.CLIENT)
	void registerIcons(IconRegister register);

}
