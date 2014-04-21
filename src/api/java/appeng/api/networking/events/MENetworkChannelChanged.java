package appeng.api.networking.events;

import appeng.api.networking.IGridNode;

/**
 * Posted by storage devices to inform AE he channel chache that the included node has changed its mind about its
 * channel requirements.
 */
public class MENetworkChannelChanged extends MENetworkEvent
{

	public final IGridNode node;

	public MENetworkChannelChanged(IGridNode n) {
		node = n;
	}

}
