package forestry.api.core;

import java.util.ArrayList;

/**
 * Used mostly by hives to determine whether they can spawn at a certain
 * position. Rather limited and hackish.
 * @depreciated there are better ways now
 */
@Deprecated
public class GlobalManager {

	/**
	 * @deprecated use Block.isGenMineableReplaceable(), anything that accepts
	 * dirt will be accepted
	 */
	@Deprecated
	public static ArrayList<Integer> dirtBlockIds = new ArrayList<Integer>();
	/**
	 * @deprecated use Block.isGenMineableReplaceable(), anything that accepts
	 * sand will be accepted
	 */
	@Deprecated
	public static ArrayList<Integer> sandBlockIds = new ArrayList<Integer>();
	/**
	 * @deprecated why is this needed?
	 */
	@Deprecated
	public static ArrayList<Integer> snowBlockIds = new ArrayList<Integer>();
	/**
	 * @deprecated Ensure your block's isLeaves function returns true instead.
	 */
	@Deprecated
	public static ArrayList<Integer> leafBlockIds = new ArrayList<Integer>();
}
