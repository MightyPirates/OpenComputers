package appeng.api.implementations;

import net.minecraft.tileentity.TileEntity;
import appeng.api.config.Upgrades;
import appeng.api.implementations.tiles.ISegmentedInventory;
import appeng.api.util.IConfigureableObject;

public interface IUpgradeableHost extends IConfigureableObject, ISegmentedInventory
{

	/**
	 * determine how many of an upgrade are installed.
	 */
	int getInstalledUpgrades(Upgrades u);

	/**
	 * the tile...
	 * 
	 * @return
	 */
	TileEntity getTile();

}
