package forestry.api.farming;

import forestry.api.core.ITileStructure;

public interface IFarmComponent extends ITileStructure {

	boolean hasFunction();

	void registerListener(IFarmListener listener);

	void removeListener(IFarmListener listener);
}
