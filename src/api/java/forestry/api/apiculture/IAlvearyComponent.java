package forestry.api.apiculture;

import forestry.api.core.ITileStructure;

/**
 * Needs to be implemented by TileEntities that want to be part of an alveary.
 */
public interface IAlvearyComponent extends ITileStructure {

	void registerBeeModifier(IBeeModifier modifier);

	void removeBeeModifier(IBeeModifier modifier);

	void registerBeeListener(IBeeListener event);

	void removeBeeListener(IBeeListener event);

	void addTemperatureChange(float change, float boundaryDown, float boundaryUp);

	void addHumidityChange(float change, float boundaryDown, float boundaryUp);

	/**
	 * @return true if this TE has a function other than a plain alveary block. Returning true prevents the TE from becoming master.
	 */
	boolean hasFunction();

}
