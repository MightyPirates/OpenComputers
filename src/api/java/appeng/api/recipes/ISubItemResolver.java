package appeng.api.recipes;

public interface ISubItemResolver
{

	/**
	 * @param namespace
	 * @param fullName
	 * @return either a ResolveReslult, or a ResolverResultSet
	 */
	public Object resolveItemByName(String namespace, String fullName);

}
