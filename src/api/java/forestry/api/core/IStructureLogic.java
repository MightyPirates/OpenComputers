package forestry.api.core;

public interface IStructureLogic extends INBTTagable {

	/**
	 * @return String unique to the type of structure controlled by this structure logic.
	 */
	String getTypeUID();

	/**
	 * Called by {@link ITileStructure}'s validateStructure().
	 */
	void validateStructure();

}
