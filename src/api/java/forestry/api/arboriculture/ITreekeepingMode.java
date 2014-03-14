package forestry.api.arboriculture;

import java.util.ArrayList;

public interface ITreekeepingMode extends ITreeModifier {

	/**
	 * @return Localized name of this treekeeping mode.
	 */
	String getName();

	/**
	 * @return Localized list of strings outlining the behaviour of this treekeeping mode.
	 */
	ArrayList<String> getDescription();

}
