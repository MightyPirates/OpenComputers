package appeng.api.networkevents;

import appeng.api.me.tiles.IGridTileEntity;
import appeng.api.me.util.IGridInterface;

/**
 * informs the controller, that a IMEPowerStorage block that had either run, out of power, or was full, is no longer in that state.
 */
public class MENetworkPowerStorage extends MENetworkEvent {
	
	public enum PowerEventType {
		REQUEST_POWER, // informs the network this tile is ready to receive power again.
		PROVIDE_POWER // informs the network this tile is ready to provide power again.
	};
	
	public final PowerEventType type;
	
	public MENetworkPowerStorage(IGridTileEntity t, PowerEventType y) {
		super(t);
		type = y;
	}

}
