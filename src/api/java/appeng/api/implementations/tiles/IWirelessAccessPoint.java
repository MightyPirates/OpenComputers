package appeng.api.implementations.tiles;

import appeng.api.networking.IGrid;
import appeng.api.util.DimensionalCoord;

public interface IWirelessAccessPoint {

	/**
	 * @return location of WAP
	 */
	DimensionalCoord getLocation();
	
	/**
	 * @return max range for this WAP
	 */
	double getRange();
	
	/**
	 * @return can you use this WAP?
	 */
	boolean isActive();

	/**
	 * @return
	 */
	IGrid getGrid();

}
