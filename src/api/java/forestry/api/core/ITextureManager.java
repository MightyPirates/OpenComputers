package forestry.api.core;

import net.minecraft.util.Icon;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface ITextureManager {

	void registerIconProvider(IIconProvider provider);

	Icon getIcon(short texUID);

	Icon getDefault(String ident);
}
