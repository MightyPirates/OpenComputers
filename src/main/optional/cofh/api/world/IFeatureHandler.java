package cofh.api.world;

/**
 * Provides an interface to allow for the addition of Features to world generation.
 * 
 * See {@link IFeatureGenerator}.
 * 
 * @author King Lemming
 * 
 */
public interface IFeatureHandler {

	/**
	 * Register a feature with an IFeatureHandler.
	 * 
	 * @param feature
	 *            The feature to register.
	 * @return True if the registration was successful, false if a feature with that name existed.
	 */
	public boolean registerFeature(IFeatureGenerator feature);

}
