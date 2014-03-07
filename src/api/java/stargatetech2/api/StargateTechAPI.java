package stargatetech2.api;

public abstract class StargateTechAPI implements IStargateTechAPI {
	protected static IStargateTechAPI apiInstance;
	
	/**
	 * StargateTech's API is abstract, and it's implementation is not visible in the API package.
	 * All available methods in IStargateTechAPI are implemented elsewhere.
	 * This method allows you to retrieve an instance of that implementation.
	 * 
	 * @return a concrete implementation of IStargateTechAPI
	 */
	public static IStargateTechAPI api(){
		return apiInstance;
	}
}