package appeng.api.networkevents;

import appeng.api.me.tiles.IGridTileEntity;
import appeng.api.me.util.IGridInterface;

/**
 * These events are posted via IGridInterface.PostEvent
 */
public class MENetworkEvent {

	private int visited = 0;
	private int available = 0;
	private boolean canceled;

	/**
	 * The tile omitting the event, not necessarily non-null, but included to suggest standard practice.
	 */
	public final IGridTileEntity tile;
	
	public MENetworkEvent( IGridTileEntity t ) {
		tile = t;
	}
	
	/**
	 * Call to prevent AE from posting the event to any further objects.
	 */
	public void cancel()
	{
		canceled = true;
	}
	
	/**
	 * called by AE after each object is called to cancel any future calls.
	 * @return
	 */
	public boolean isCanceled()
	{
		return canceled;
	}
	
	/**
	 * the number of objects that were visited by the event.
	 * @return
	 */
	public int getVisitedObjects()
	{
		return visited;
	}
	
	/**
	 * May differ from visited is the event is canceled.
	 * @return
	 */
	public int getAvailableObjects()
	{
		return available;
	}
	
	/**
	 * Called by AE after iterating the event subscribers.
	 * @param v
	 * @param a
	 */
	public void setVisitedObjects( int v, int a )
	{
		visited = v;
		available = a;
	}
}
