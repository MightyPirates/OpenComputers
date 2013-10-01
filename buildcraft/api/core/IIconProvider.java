package buildcraft.api.core;

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.util.Icon;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public interface IIconProvider
{

	/**
	 * @param iconIndex
	 * @return
	 */
	@SideOnly(Side.CLIENT)
	public Icon getIcon(int iconIndex);

	/**
	 * A call for the provider to register its Icons. This may be called multiple times but should
	 * only be executed once per provider
	 * 
	 * @param iconRegister
	 */
	@SideOnly(Side.CLIENT)
	public void registerIcons(IconRegister iconRegister);

}
