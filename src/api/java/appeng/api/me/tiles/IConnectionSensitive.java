package appeng.api.me.tiles;

import java.util.Set;

import net.minecraftforge.common.ForgeDirection;

public interface IConnectionSensitive {
	
	void onMEConnectionsChanged( Set<ForgeDirection> connections, Set<ForgeDirection> visualConnections );
	
}
