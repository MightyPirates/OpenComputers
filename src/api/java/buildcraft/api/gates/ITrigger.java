/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.gates;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

public interface ITrigger {

	/**
	 * Every trigger needs a unique tag, it should be in the format of
	 * "<modid>:<name>".
	 *
	 * @return the unique id
	 */
	String getUniqueTag();

	@SideOnly(Side.CLIENT)
	IIcon getIcon();

	@SideOnly(Side.CLIENT)
	void registerIcons(IIconRegister iconRegister);

	/**
	 * Return true if this trigger can accept parameters
	 */
	boolean hasParameter();

	/**
	 * Return true if this trigger requires a parameter
	 */
	boolean requiresParameter();

	/**
	 * Return the trigger description in the UI
	 */
	String getDescription();

	/**
	 * Create parameters for the trigger. As for now, there is only one kind of
	 * trigger parameter available so this subprogram is final.
	 */
	ITriggerParameter createParameter();
}
