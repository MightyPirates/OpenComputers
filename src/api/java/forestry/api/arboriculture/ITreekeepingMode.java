/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
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
