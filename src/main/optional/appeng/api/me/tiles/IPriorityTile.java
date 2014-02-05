package appeng.api.me.tiles;

public interface IPriorityTile
{
	/**
	 * gets the current priroity, can be negitive or positive.
	 * implemented by storage systems, and is used for sorting.
	 * @return
	 */
	int getPriority();	
	
	/**
	 * change the priority.
	 * @param p
	 */
	void setPriority( int p );	
}
