package forestry.api.genetics;

public interface IFruitFamily {

	/**
	 * @return Unique String identifier.
	 */
	String getUID();

	/**
	 * @return Localized family name for user display.
	 */
	String getName();

	/**
	 * A scientific-y name for this fruit family
	 * 
	 * @return flavour text (may be null)
	 */
	String getScientific();

	/**
	 * @return Localized description of this fruit family. (May be null.)
	 */
	String getDescription();

}
